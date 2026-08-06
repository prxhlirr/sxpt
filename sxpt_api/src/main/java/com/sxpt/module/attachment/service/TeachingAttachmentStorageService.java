package com.sxpt.module.attachment.service;

import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;

/**
 * 教学附件本地存储服务。
 *
 * 后端只保存原文件并提供带签名的访问地址。Office、PDF、图片等文件统一交给
 * 前端 Open File Viewer 在浏览器中解析，不再调用 LibreOffice 或生成 PDF 副本。
 */
@Service
public class TeachingAttachmentStorageService {

    public static final long DEFAULT_MAX_FILE_BYTES = 5L * 1024L * 1024L;
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String METADATA_FILE = "metadata.properties";
    private static final String CONTENT_VARIANT = "content";

    private final Path storageRoot;
    private final long maxFileBytes;
    private final byte[] accessTokenSecret;

    public TeachingAttachmentStorageService(
            @Value("${sxpt.attachments.storage-root:./data/teaching-attachments}") String storageRoot,
            @Value("${sxpt.attachments.max-file-bytes:5242880}") long maxFileBytes,
            @Value("${sxpt.attachments.url-token-secret:sxpt_dev_attachment_url_secret}") String accessTokenSecret) {
        this.storageRoot = Paths.get(storageRoot).toAbsolutePath().normalize();
        this.maxFileBytes = Math.max(1L, maxFileBytes);
        this.accessTokenSecret = accessTokenSecret.getBytes(StandardCharsets.UTF_8);
    }

    /** 保存原始附件，工作区只保留返回的 URL。 */
    public StoredAttachment store(MultipartFile multipartFile, String ownerUserId) {
        validateUpload(multipartFile);
        String id = UUID.randomUUID().toString().replace("-", "");
        String originalName = normalizeOriginalName(multipartFile.getOriginalFilename());
        String extension = extensionOf(originalName);
        Path directory = resolveAttachmentDirectory(id);
        Path originalFile = directory.resolve("original" + (extension.isEmpty() ? "" : "." + extension));

        StoredAttachment result = new StoredAttachment();
        result.id = id;
        result.name = originalName;
        result.mimeType = normalizeMimeType(multipartFile.getContentType());
        result.size = multipartFile.getSize();
        result.uploadedAt = Instant.now().toString();
        result.originalFileName = originalFile.getFileName().toString();
        result.ownerUserId = ownerUserId;

        try {
            Files.createDirectories(directory);
            try (InputStream inputStream = multipartFile.getInputStream()) {
                Files.copy(inputStream, originalFile, StandardCopyOption.REPLACE_EXISTING);
            }
            writeMetadata(directory, result);
            return result;
        } catch (IOException exception) {
            deleteFileQuietly(originalFile);
            deleteFileQuietly(directory.resolve(METADATA_FILE));
            deleteDirectoryQuietly(directory);
            throw new BusinessException(ApiResultCode.SYSTEM_ERROR.getCode(), "附件保存失败，请稍后重试");
        }
    }

    /** 使用签名读取原文件，供 Open File Viewer 通过 URL 加载。 */
    public AttachmentResource load(String id, String token) {
        validateId(id);
        if (!isValidAccessToken(id, token)) {
            throw new BusinessException(ApiResultCode.FORBIDDEN);
        }
        Path directory = resolveAttachmentDirectory(id);
        StoredAttachment metadata = readMetadata(directory);
        Path file = directory.resolve(metadata.originalFileName).normalize();
        if (!file.startsWith(directory) || !Files.isRegularFile(file)) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND.getCode(), "附件文件不存在");
        }
        return new AttachmentResource(file, metadata.name, metadata.mimeType, fileSize(file));
    }

    public String createAccessToken(String id) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(accessTokenSecret, HMAC_ALGORITHM));
            byte[] signature = mac.doFinal((id + ":" + CONTENT_VARIANT).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception exception) {
            throw new IllegalStateException("无法生成附件访问签名", exception);
        }
    }

    private boolean isValidAccessToken(String id, String token) {
        if (!StringUtils.hasText(token)) return false;
        byte[] actual = token.getBytes(StandardCharsets.UTF_8);
        byte[] expected = createAccessToken(id).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(actual, expected);
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR.getCode(), "请选择需要上传的附件");
        }
        if (file.getSize() > maxFileBytes) {
            throw new BusinessException(
                    ApiResultCode.PARAM_ERROR.getCode(),
                    "附件不能超过 " + (maxFileBytes / 1024L / 1024L) + " MB"
            );
        }
    }

    private void writeMetadata(Path directory, StoredAttachment attachment) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("id", attachment.id);
        properties.setProperty("name", attachment.name);
        properties.setProperty("mimeType", attachment.mimeType);
        properties.setProperty("size", String.valueOf(attachment.size));
        properties.setProperty("uploadedAt", attachment.uploadedAt);
        properties.setProperty("originalFileName", attachment.originalFileName);
        properties.setProperty("ownerUserId", attachment.ownerUserId == null ? "" : attachment.ownerUserId);
        try (OutputStream outputStream = Files.newOutputStream(directory.resolve(METADATA_FILE))) {
            properties.store(outputStream, "sxpt teaching attachment");
        }
    }

    private StoredAttachment readMetadata(Path directory) {
        Path metadataFile = directory.resolve(METADATA_FILE);
        if (!Files.isRegularFile(metadataFile)) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND.getCode(), "附件不存在");
        }
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(metadataFile)) {
            properties.load(inputStream);
        } catch (IOException exception) {
            throw new BusinessException(ApiResultCode.SYSTEM_ERROR.getCode(), "附件元数据读取失败");
        }
        StoredAttachment result = new StoredAttachment();
        result.id = properties.getProperty("id", "");
        result.name = properties.getProperty("name", "attachment");
        result.mimeType = properties.getProperty("mimeType", "application/octet-stream");
        result.size = parseLong(properties.getProperty("size"));
        result.uploadedAt = properties.getProperty("uploadedAt", "");
        result.originalFileName = properties.getProperty("originalFileName", "original");
        result.ownerUserId = properties.getProperty("ownerUserId", "");
        return result;
    }

    private Path resolveAttachmentDirectory(String id) {
        Path directory = storageRoot.resolve(id).normalize();
        if (!directory.startsWith(storageRoot)) {
            throw new BusinessException(ApiResultCode.FORBIDDEN);
        }
        return directory;
    }

    private void validateId(String id) {
        if (id == null || !id.matches("[a-fA-F0-9]{32}")) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND.getCode(), "附件不存在");
        }
    }

    private String normalizeOriginalName(String originalName) {
        if (!StringUtils.hasText(originalName)) return "attachment";
        String normalized = Paths.get(originalName).getFileName().toString().replaceAll("[\\r\\n]", "_");
        return normalized.length() > 180 ? normalized.substring(normalized.length() - 180) : normalized;
    }

    private String normalizeMimeType(String mimeType) {
        return StringUtils.hasText(mimeType)
                ? mimeType.trim().toLowerCase(Locale.ROOT)
                : "application/octet-stream";
    }

    private String extensionOf(String name) {
        int position = name.lastIndexOf('.');
        if (position < 0 || position == name.length() - 1) return "";
        String extension = name.substring(position + 1).toLowerCase(Locale.ROOT);
        return extension.matches("[a-z0-9]{1,12}") ? extension : "";
    }

    private long fileSize(Path file) {
        try {
            return Files.size(file);
        } catch (IOException exception) {
            return 0L;
        }
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    private void deleteFileQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // 上传失败后的清理不覆盖原始异常。
        }
    }

    private void deleteDirectoryQuietly(Path directory) {
        try {
            Files.deleteIfExists(directory);
        } catch (IOException ignored) {
            // 上传失败后的清理不覆盖原始异常。
        }
    }

    public static final class StoredAttachment {
        private String id;
        private String name;
        private String mimeType;
        private long size;
        private String uploadedAt;
        private String originalFileName;
        private String ownerUserId;

        public String getId() { return id; }
        public String getName() { return name; }
        public String getMimeType() { return mimeType; }
        public long getSize() { return size; }
        public String getUploadedAt() { return uploadedAt; }
    }

    public static final class AttachmentResource {
        private final Path path;
        private final String name;
        private final String mimeType;
        private final long size;

        public AttachmentResource(Path path, String name, String mimeType, long size) {
            this.path = path;
            this.name = name;
            this.mimeType = mimeType;
            this.size = size;
        }

        public Path getPath() { return path; }
        public String getName() { return name; }
        public String getMimeType() { return mimeType; }
        public long getSize() { return size; }
    }
}

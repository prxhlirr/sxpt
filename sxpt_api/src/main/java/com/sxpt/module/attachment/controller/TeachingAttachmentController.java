package com.sxpt.module.attachment.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.common.security.CurrentUserContext;
import com.sxpt.module.attachment.service.TeachingAttachmentStorageService;
import com.sxpt.module.attachment.service.TeachingAttachmentStorageService.AttachmentResource;
import com.sxpt.module.attachment.service.TeachingAttachmentStorageService.StoredAttachment;
import com.sxpt.module.attachment.vo.TeachingAttachmentVO;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 教学附件上传和带签名的内容访问接口。
 */
@RestController
@RequestMapping("/api/v1/teaching-attachments")
public class TeachingAttachmentController {

    private final TeachingAttachmentStorageService storageService;

    public TeachingAttachmentController(TeachingAttachmentStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<TeachingAttachmentVO> upload(@RequestPart("file") MultipartFile file) {
        CurrentUserContext.CurrentUser user = CurrentUserContext.getRequiredUser();
        if (!user.hasAnyRole("ADMIN", "TEACHER")) {
            throw new BusinessException(ApiResultCode.FORBIDDEN);
        }
        StoredAttachment stored = storageService.store(file, user.getUserId());
        return ApiResult.success(toVO(stored));
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<FileSystemResource> content(
            @PathVariable String id,
            @RequestParam String token,
            @RequestParam(defaultValue = "false") boolean download) {
        return fileResponse(storageService.load(id, token), download);
    }

    private TeachingAttachmentVO toVO(StoredAttachment stored) {
        String contentUrl = signedUrl(stored.getId());
        TeachingAttachmentVO vo = new TeachingAttachmentVO();
        vo.setId(stored.getId());
        vo.setName(stored.getName());
        vo.setMimeType(stored.getMimeType());
        vo.setSize(stored.getSize());
        vo.setDataUrl(contentUrl);
        vo.setDownloadUrl(UriComponentsBuilder.fromHttpUrl(contentUrl)
                .queryParam("download", true)
                .build(true)
                .toUriString());
        vo.setUploadedAt(stored.getUploadedAt());
        return vo;
    }

    private String signedUrl(String id) {
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/teaching-attachments/")
                .path(id)
                .path("/content")
                .toUriString();
        return UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("token", storageService.createAccessToken(id))
                .build(true)
                .toUriString();
    }

    private ResponseEntity<FileSystemResource> fileResponse(
            AttachmentResource resource,
            boolean download) {
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(resource.getMimeType());
        } catch (Exception exception) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        ContentDisposition disposition = ContentDisposition
                .builder(download ? "attachment" : "inline")
                .filename(resource.getName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(resource.getSize())
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(resource.getPath()));
    }
}

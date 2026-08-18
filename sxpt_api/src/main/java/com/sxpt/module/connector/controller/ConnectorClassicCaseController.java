package com.sxpt.module.connector.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.common.security.ExternalConnectorContext;
import com.sxpt.module.connector.dto.ClassicCaseDisableRequest;
import com.sxpt.module.connector.dto.ClassicCaseUpsertRequest;
import com.sxpt.module.connector.entity.ClassicCaseAsset;
import com.sxpt.module.connector.service.ClassicCaseService;
import com.sxpt.module.connector.service.ExternalConnectorCredentialService.AuthenticatedExternalConnector;
import com.sxpt.module.connector.vo.ClassicCaseSyncResultVO;
import org.springframework.util.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/** OA 通过 X-Connector-Key 调用的经典案例目标契约。 */
@RestController
@RequestMapping("/api/v1/connector/classic-cases")
public class ConnectorClassicCaseController {

    private final ClassicCaseService classicCaseService;

    public ConnectorClassicCaseController(ClassicCaseService classicCaseService) {
        this.classicCaseService = classicCaseService;
    }

    @PostMapping("/upsert")
    public ApiResult<ClassicCaseSyncResultVO> upsert(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ClassicCaseUpsertRequest request) {
        requireIdempotencyKey(idempotencyKey);
        AuthenticatedExternalConnector connector = ExternalConnectorContext.require();
        ClassicCaseAsset asset = classicCaseService.upsertClassicCase(request, connector);
        return ApiResult.success(toResult(asset, request.getCaseVersionId()));
    }

    @PostMapping("/disable")
    public ApiResult<ClassicCaseSyncResultVO> disable(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ClassicCaseDisableRequest request) {
        requireIdempotencyKey(idempotencyKey);
        AuthenticatedExternalConnector connector = ExternalConnectorContext.require();
        ClassicCaseAsset asset = classicCaseService.disableClassicCase(
                connector.getSourceSystem().getTenantId(),
                connector.getSourceSystem().getId(),
                request.getCaseCode(),
                request.getReason(),
                connector.getSourceSystem().getId());
        return ApiResult.success(toResult(asset, null));
    }

    private ClassicCaseSyncResultVO toResult(ClassicCaseAsset asset, String caseVersionId) {
        ClassicCaseSyncResultVO result = new ClassicCaseSyncResultVO();
        result.setClassicCaseId(asset.getId());
        result.setCaseCode(asset.getCaseCode());
        result.setCaseVersionId(caseVersionId);
        result.setSyncStatus(asset.getStatus());
        return result;
    }

    private void requireIdempotencyKey(String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new BusinessException(ApiResultCode.IDEMPOTENCY_KEY_REQUIRED);
        }
    }

    /** 该系统间契约按业务码同步返回真实 HTTP 状态，便于 OA 重试与告警。 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult<Void>> handleBusinessException(BusinessException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getCode());
        if (status == null) {
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status)
                .body(ApiResult.failure(exception.getCode(), exception.getMessage()));
    }

    /** 并发推送命中版本唯一约束时按幂等冲突返回，而不是暴露数据库异常。 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResult<Void>> handleDataIntegrityViolationException(
            DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResult.failure(
                ApiResultCode.IDEMPOTENCY_CONFLICT.getCode(),
                "相同案例版本正在处理或内容已存在"));
    }
}

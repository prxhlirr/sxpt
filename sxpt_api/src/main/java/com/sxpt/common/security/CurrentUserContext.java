package com.sxpt.common.security;

import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 当前用户上下文。
 *
 * 业务功能：
 * 1. 保存本次请求经过 JWT 认证后得到的可信用户身份。
 * 2. 为 Service 层、审计日志、对象级授权提供统一的当前操作者来源。
 *
 * 关键流程：
 * 1. JwtAuthInterceptor 认证通过后调用 set 写入当前线程。
 * 2. 业务代码通过 getRequiredUser 读取当前用户，避免继续信任前端传入的 operatorId。
 * 3. 请求结束或认证失败时必须 clear，避免 Web 容器线程复用造成身份串用。
 */
public final class CurrentUserContext {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<CurrentUser>();

    private CurrentUserContext() {
    }

    /**
     * 写入当前请求用户。
     *
     * @param currentUser 当前认证用户，必须来自服务端认证链路。
     */
    public static void set(CurrentUser currentUser) {
        if (currentUser == null || !StringUtils.hasText(currentUser.getUserId())) {
            throw new BusinessException(ApiResultCode.UNAUTHORIZED);
        }
        HOLDER.set(currentUser);
    }

    /**
     * 获取当前请求用户。
     *
     * @return 当前用户可选值，未认证请求为空。
     */
    public static Optional<CurrentUser> get() {
        return Optional.ofNullable(HOLDER.get());
    }

    /**
     * 获取必须存在的当前请求用户。
     *
     * @return 当前认证用户。
     */
    public static CurrentUser getRequiredUser() {
        return get().orElseThrow(() -> new BusinessException(ApiResultCode.UNAUTHORIZED));
    }

    /**
     * 清理当前线程用户。
     */
    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 当前认证用户。
     *
     * 业务功能：
     * 1. 表达服务端已认证的最小用户身份。
     * 2. 后续可扩展 tenantId、roles、orgIds、identityBindings 等上下文字段。
     *
     * 关键流程：
     * 1. 当前阶段先由 JwtPrincipal 转换生成。
     * 2. auth/me 和核心业务服务优先读取该对象，而不是读取前端传参。
     */
    public static final class CurrentUser {

        private final String userId;

        private final String tenantId;

        private final String username;

        private final String displayName;

        private final String userType;

        private final String studentNo;

        private final String employeeNo;

        private final List<String> roleCodes;

        private final List<String> orgIds;

        private final List<IdentityBindingSummary> identityBindings;

        public CurrentUser(String userId, String username) {
            this(userId, null, username, null, null, null, null,
                    Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }

        public CurrentUser(String userId,
                           String tenantId,
                           String username,
                           String displayName,
                           String userType,
                           String studentNo,
                           String employeeNo,
                           List<String> roleCodes,
                           List<String> orgIds,
                           List<IdentityBindingSummary> identityBindings) {
            this.userId = userId;
            this.tenantId = tenantId;
            this.username = username;
            this.displayName = displayName;
            this.userType = userType;
            this.studentNo = studentNo;
            this.employeeNo = employeeNo;
            this.roleCodes = immutableCopy(roleCodes);
            this.orgIds = immutableCopy(orgIds);
            this.identityBindings = immutableIdentityBindingCopy(identityBindings);
        }

        public String getUserId() {
            return userId;
        }

        public String getTenantId() {
            return tenantId;
        }

        public String getUsername() {
            return username;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getUserType() {
            return userType;
        }

        public String getStudentNo() {
            return studentNo;
        }

        public String getEmployeeNo() {
            return employeeNo;
        }

        public List<String> getRoleCodes() {
            return roleCodes;
        }

        public List<String> getOrgIds() {
            return orgIds;
        }

        public List<IdentityBindingSummary> getIdentityBindings() {
            return identityBindings;
        }

        private static List<String> immutableCopy(List<String> values) {
            if (values == null || values.isEmpty()) {
                return Collections.emptyList();
            }
            return Collections.unmodifiableList(new ArrayList<String>(values));
        }

        private static List<IdentityBindingSummary> immutableIdentityBindingCopy(
                List<IdentityBindingSummary> values) {
            if (values == null || values.isEmpty()) {
                return Collections.emptyList();
            }
            return Collections.unmodifiableList(new ArrayList<IdentityBindingSummary>(values));
        }
    }

    /**
     * 鍘熷钩鍙拌韩浠界粦瀹氭憳瑕併€?     *
     * 涓氬姟鍔熻兘锛?     * 1. 灏嗗綋鍓嶇敤鎴峰湪鍘熷钩鍙扮殑鍙俊韬唤缁戝畾鏆存斁鍏ヨ璇佷笂涓嬫枃銆?     * 2. 鍚庣画鏁版嵁鍑嗗鍜?launchToken 鏀堕敛鏃跺彲鐩存帴浣跨敤鏈嶅姟绔弽鏌ョ殑韬唤鏄犲皠銆?     *
     * 鍏抽敭娴佺▼锛?     * 1. 褰撳墠闃舵浼樺厛淇濈暀缁撴瀯锛屾帴鍏ュ叏閲忕粦瀹氭煡璇㈠悗鍐嶅～鍏呭垪琛ㄣ€?     * 2. 瀵硅薄涓嶆彁渚涘彲鍙樻洿闆嗗悎锛岄伩鍏嶄笟鍔＄嚎绋嬩慨鏀硅璇佺粨鏋溿€?     */
    public static final class IdentityBindingSummary {

        private final String connectorSystemId;

        private final String externalUserId;

        private final String externalUsername;

        public IdentityBindingSummary(String connectorSystemId, String externalUserId, String externalUsername) {
            this.connectorSystemId = connectorSystemId;
            this.externalUserId = externalUserId;
            this.externalUsername = externalUsername;
        }

        public String getConnectorSystemId() {
            return connectorSystemId;
        }

        public String getExternalUserId() {
            return externalUserId;
        }

        public String getExternalUsername() {
            return externalUsername;
        }
    }
}

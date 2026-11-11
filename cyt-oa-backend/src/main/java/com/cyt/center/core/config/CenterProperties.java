package com.cyt.center.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 项目自己的配置项。认证与集成一律走服务端环境变量，任何情况下都不接受浏览器传来的身份声明。
 */
@Data
@Component
@ConfigurationProperties(prefix = "cyt")
public class CenterProperties {

    private final Auth auth = new Auth();
    private final Sso sso = new Sso();
    private final Wecom wecom = new Wecom();
    private final Directory directory = new Directory();
    private final Web web = new Web();
    private final Ai ai = new Ai();

    @Data
    public static class Ai {
        /** Gemini API key（迁自 Portal 的 GEMINI_API_KEY；空 = 相关 AI 功能不可用但服务可启动）。 */
        private String geminiKey = "";
        /** 厂商：gemini（Claude 渠道已停用，同 Portal 口径）。 */
        private String provider = "gemini";
    }

    @Data
    public static class Auth {
        /** 账号密码登录开关：本地开，线上必须 false（线上只允许企微）。 */
        private boolean passwordLoginEnabled = false;
        /** 兼容期开关：允许前端用 X-User-Id 头直接指定身份，只可在本机 profile 打开。 */
        private boolean devIdentityEnabled = false;
        /** 引导管理员：这些企微 userid 同步进来后自动获得 admin 角色，否则没人能进后台。 */
        private String adminUserids = "";
        /** 登录令牌有效期（秒）。 */
        private long tokenTimeout = 604800;

        public List<String> adminUseridList() {
            return split(adminUserids);
        }
    }

    @Data
    public static class Sso {
        /** Portal 侧还没为本应用登记 SSO 客户时保持 false，避免半可用状态误导使用者。 */
        private boolean enabled = false;
        private String portalBaseUrl = "";
        private String portalPublicBaseUrl = "";
        /** Portal 为本应用签发的固定路径前缀，一个客户一套。 */
        private String endpointPrefix = "/api/portal/auth/sso/oa";
        private String audience = "cyt-oa-center";
        private String statePrefix = "oa.";
        private String codePrefix = "oa_";
        /** 登录完成后回跳的前端基址，必须由服务端配置，不能从请求头推导。 */
        private String returnBaseUrl = "";
    }

    @Data
    public static class Wecom {
        private String gatewayUrl = "";
        private String apiKey = "";
        private String systemId = "portal";
        /** 本机联调时改用 X-System-Id 免密钥调用；线上必须 false。 */
        private boolean allowDevIdentity = false;
        private String corpId = "";
        /**
         * 各审批在网关里绑定的业务模板别名，不是企微的 template_id；
         * 真实 template_id 与控件映射由「系统管理 - 审批模板」页的绑定决定，换模板只改这里。
         */
        private String leaveTemplate = "leave";
        private String overtimeTemplate = "overtime";
        private String reimbursementTemplate = "reimbursement";
        private String travelTemplate = "travel";
        private String travelReimbursementTemplate = "travel-reimbursement";
        private String supplyTemplate = "supply";
        private String hrmLeaveTemplate = "hrm-leave";
    }

    @Data
    public static class Directory {
        private boolean syncEnabled = true;
        private String syncCron = "0 30 6 * * ?";
    }

    @Data
    public static class Web {
        private String allowedOrigins = "";

        public List<String> allowedOriginList() {
            return split(allowedOrigins);
        }
    }

    private static List<String> split(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(item -> !item.isEmpty()).toList();
    }

}

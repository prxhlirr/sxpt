package com.sxpt.module.teachingdata.enums;

/**
 * 数据准备状态枚举集合。
 *
 * 业务功能：
 * 1. 统一定义数据需求、需求项、准备任务、数据池、数据实例、分配、协作、考试题目、校验和触发方式的状态值。
 * 2. 为数据准备任务、数据实例、练习重置、考试锁定和归档提供稳定状态入口。
 * 3. 避免后续 Service、Controller 和测试中散落魔法字符串。
 *
 * 关键流程：
 * 1. 数据准备前先创建 RequirementStatus。
 * 2. 每条需求明细用 RequirementItemStatus 跟踪原平台请求和校验结果。
 * 3. 原平台返回可见性、可操作性和状态匹配后，用 ValidationStatus 表达校验结论。
 * 4. 数据准备任务按 TriggerType 区分人工触发、发布触发、按需触发、重试和系统任务。
 * 5. Service 层引用本类枚举值，避免状态字符串散落在不同业务服务中。
 */
public final class DataPrepareStatusEnums {

    private DataPrepareStatusEnums() {
    }

    /**
     * 判断状态值是否包含有效文本。
     *
     * @param value 待判断文本。
     * @return true 表示文本不为空且去除空白后仍有内容。
     */
    private static boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }

    /**
     * 数据需求批次状态。
     *
     * 业务功能：
     * 1. 描述一次批量数据准备需求从创建到完成的生命周期。
     * 2. 支撑数据池、准备任务和后台页面按批次状态筛选。
     *
     * 关键流程：
     * 1. 创建批次时为 CREATED。
     * 2. 调用原平台准备数据时进入 PREPARING。
     * 3. 全部明细成功为 READY，部分失败为 PARTIAL_FAILED，关键失败为 FAILED。
     */
    public enum RequirementStatus {
        CREATED("CREATED", "已创建", false),
        PREPARING("PREPARING", "准备中", false),
        READY("READY", "已就绪", true),
        PARTIAL_FAILED("PARTIAL_FAILED", "部分失败", true),
        FAILED("FAILED", "失败", true),
        CANCELLED("CANCELLED", "已取消", true);

        private final String value;

        private final String label;

        private final boolean terminal;

        RequirementStatus(String value, String label, boolean terminal) {
            this.value = value;
            this.label = label;
            this.terminal = terminal;
        }

        /**
         * 获取数据库持久化和接口传输使用的状态值。
         *
         * @return 状态值。
         */
        public String getValue() {
            return value;
        }

        /**
         * 获取后台页面展示使用的中文名称。
         *
         * @return 中文名称。
         */
        public String getLabel() {
            return label;
        }

        /**
         * 判断该状态是否为批次终态，避免终态批次被重复推进。
         *
         * @return true 表示终态。
         */
        public boolean isTerminal() {
            return terminal;
        }

        /**
         * 按持久化状态值解析枚举。
         *
         * @param value 状态值。
         * @return 匹配的枚举；未命中时返回 null，让调用方决定是否抛业务异常。
         */
        public static RequirementStatus fromValue(String value) {
            if (!hasText(value)) {
                return null;
            }
            for (RequirementStatus status : values()) {
                if (status.value.equals(value)) {
                    return status;
                }
            }
            return null;
        }
    }

    /**
     * 数据需求明细状态。
     *
     * 业务功能：
     * 1. 描述每个学生、题目、角色片段对应的数据准备明细状态。
     * 2. 支撑批量创建时逐条回填、逐条重试和逐条废弃。
     *
     * 关键流程：
     * 1. 需求项创建后为 CREATED。
     * 2. 已请求原平台为 REQUESTED。
     * 3. 原平台返回并校验通过为 READY。
     * 4. 数据生成成功但单位、角色、状态或动作校验失败为 VALIDATION_FAILED。
     */
    public enum RequirementItemStatus {
        CREATED("CREATED", "已创建", false),
        REQUESTED("REQUESTED", "已请求", false),
        READY("READY", "已就绪", true),
        VALIDATION_FAILED("VALIDATION_FAILED", "校验失败", true),
        FAILED("FAILED", "失败", true),
        DISCARDED("DISCARDED", "已废弃", true);

        private final String value;

        private final String label;

        private final boolean terminal;

        RequirementItemStatus(String value, String label, boolean terminal) {
            this.value = value;
            this.label = label;
            this.terminal = terminal;
        }

        /**
         * 获取数据库持久化和接口传输使用的状态值。
         *
         * @return 状态值。
         */
        public String getValue() {
            return value;
        }

        /**
         * 获取后台页面展示使用的中文名称。
         *
         * @return 中文名称。
         */
        public String getLabel() {
            return label;
        }

        /**
         * 判断该需求项是否已经到达终态，避免重复请求原平台。
         *
         * @return true 表示终态。
         */
        public boolean isTerminal() {
            return terminal;
        }

        /**
         * 按持久化状态值解析枚举。
         *
         * @param value 状态值。
         * @return 匹配的枚举；未命中时返回 null。
         */
        public static RequirementItemStatus fromValue(String value) {
            if (!hasText(value)) {
                return null;
            }
            for (RequirementItemStatus status : values()) {
                if (status.value.equals(value)) {
                    return status;
                }
            }
            return null;
        }
    }

    /**
     * 数据准备任务状态。
     *
     * 业务功能：
     * 1. 描述 data_prepare_job 从创建、执行、重试到完成的生命周期。
     * 2. 支撑后台任务列表按状态筛选，也支撑失败补偿任务只扫描可重试状态。
     *
     * 关键流程：
     * 1. 创建任务时为 CREATED。
     * 2. 调用原平台期间为 RUNNING。
     * 3. 全部成功为 SUCCESS，部分失败为 PARTIAL_FAILED，全部失败为 FAILED。
     */
    public enum PrepareJobStatus {
        CREATED("CREATED", "已创建", false, false),
        RUNNING("RUNNING", "执行中", false, false),
        SUCCESS("SUCCESS", "成功", true, false),
        PARTIAL_FAILED("PARTIAL_FAILED", "部分失败", true, true),
        FAILED("FAILED", "失败", true, true),
        CANCELLED("CANCELLED", "已取消", true, false);

        private final String value;

        private final String label;

        private final boolean terminal;

        private final boolean retryable;

        PrepareJobStatus(String value, String label, boolean terminal, boolean retryable) {
            this.value = value;
            this.label = label;
            this.terminal = terminal;
            this.retryable = retryable;
        }

        /**
         * 获取数据库持久化和接口传输使用的状态值。
         *
         * @return 状态值。
         */
        public String getValue() {
            return value;
        }

        /**
         * 获取后台页面展示使用的中文名称。
         *
         * @return 中文名称。
         */
        public String getLabel() {
            return label;
        }

        /**
         * 判断任务是否到达终态，避免终态任务被继续推进。
         *
         * @return true 表示终态。
         */
        public boolean isTerminal() {
            return terminal;
        }

        /**
         * 判断任务是否允许进入失败补偿队列。
         *
         * @return true 表示允许重试。
         */
        public boolean isRetryable() {
            return retryable;
        }

        /**
         * 按持久化状态值解析枚举。
         *
         * @param value 状态值。
         * @return 匹配的枚举；未命中时返回 null。
         */
        public static PrepareJobStatus fromValue(String value) {
            if (!hasText(value)) {
                return null;
            }
            for (PrepareJobStatus status : values()) {
                if (status.value.equals(value)) {
                    return status;
                }
            }
            return null;
        }
    }

    /**
     * 教学数据池状态。
     *
     * 业务功能：
     * 1. 描述 teaching_data_pool 中库存聚合从创建、准备到可领取的状态。
     * 2. 支撑学生领取数据前判断数据池是否可用。
     *
     * 关键流程：
     * 1. 数据池创建后为 CREATED。
     * 2. 数据准备任务运行期间为 PREPARING。
     * 3. 存在可用实例后为 READY，容量耗尽后为 EXHAUSTED。
     */
    public enum DataPoolStatus {
        CREATED("CREATED", "已创建", false, false),
        PREPARING("PREPARING", "准备中", false, false),
        READY("READY", "已就绪", false, true),
        EXHAUSTED("EXHAUSTED", "已耗尽", false, false),
        FAILED("FAILED", "失败", true, false),
        CLOSED("CLOSED", "已关闭", true, false);

        private final String value;

        private final String label;

        private final boolean terminal;

        private final boolean allocatable;

        DataPoolStatus(String value, String label, boolean terminal, boolean allocatable) {
            this.value = value;
            this.label = label;
            this.terminal = terminal;
            this.allocatable = allocatable;
        }

        /**
         * 获取数据库持久化和接口传输使用的状态值。
         *
         * @return 状态值。
         */
        public String getValue() {
            return value;
        }

        /**
         * 获取后台页面展示使用的中文名称。
         *
         * @return 中文名称。
         */
        public String getLabel() {
            return label;
        }

        /**
         * 判断数据池是否已关闭或失败。
         *
         * @return true 表示终态。
         */
        public boolean isTerminal() {
            return terminal;
        }

        /**
         * 判断数据池是否允许学生领取数据。
         *
         * @return true 表示可领取。
         */
        public boolean isAllocatable() {
            return allocatable;
        }

        /**
         * 按持久化状态值解析枚举。
         *
         * @param value 状态值。
         * @return 匹配的枚举；未命中时返回 null。
         */
        public static DataPoolStatus fromValue(String value) {
            if (!hasText(value)) {
                return null;
            }
            for (DataPoolStatus status : values()) {
                if (status.value.equals(value)) {
                    return status;
                }
            }
            return null;
        }
    }

    /**
     * 教学数据实例状态。
     *
     * 业务功能：
     * 1. 描述 teaching_data_instance 是否可领取、已锁定、已废弃或已归档。
     * 2. 支撑练习重置和考试锁定时明确实例生命周期。
     *
     * 关键流程：
     * 1. 原平台数据校验通过后为 READY。
     * 2. 学生领取后为 ALLOCATED。
     * 3. 练习重置生成新的原平台业务数据后为 RESET。
     * 4. 考试或强约束场景锁定后为 LOCKED。
     */
    public enum DataInstanceStatus {
        CREATED("CREATED", "已创建", false),
        READY("READY", "已就绪", false),
        ALLOCATED("ALLOCATED", "已分配", false),
        RESET("RESET", "已重置", false),
        LOCKED("LOCKED", "已锁定", false),
        CONSUMED("CONSUMED", "已消费", true),
        DISCARDED("DISCARDED", "已废弃", true),
        ARCHIVED("ARCHIVED", "已归档", true),
        FAILED("FAILED", "失败", true);

        private final String value;

        private final String label;

        private final boolean terminal;

        DataInstanceStatus(String value, String label, boolean terminal) {
            this.value = value;
            this.label = label;
            this.terminal = terminal;
        }

        /**
         * 获取数据库持久化和接口传输使用的状态值。
         *
         * @return 状态值。
         */
        public String getValue() {
            return value;
        }

        /**
         * 获取后台页面展示使用的中文名称。
         *
         * @return 中文名称。
         */
        public String getLabel() {
            return label;
        }

        /**
         * 判断实例是否已经不能继续被业务推进。
         *
         * @return true 表示终态。
         */
        public boolean isTerminal() {
            return terminal;
        }

        /**
         * 按持久化状态值解析枚举。
         *
         * @param value 状态值。
         * @return 匹配的枚举；未命中时返回 null。
         */
        public static DataInstanceStatus fromValue(String value) {
            if (!hasText(value)) {
                return null;
            }
            for (DataInstanceStatus status : values()) {
                if (status.value.equals(value)) {
                    return status;
                }
            }
            return null;
        }
    }

    /**
     * 数据实例分配状态。
     *
     * 业务功能：
     * 1. 描述 data_instance_allocation 中学生领取记录的生命周期。
     * 2. 支撑练习重置、考试提交、释放和审计查询。
     *
     * 关键流程：
     * 1. 学生领取数据后为 ALLOCATED。
     * 2. 进入原平台并产生使用行为后为 CONSUMED。
     * 3. 重置或异常释放后为 RELEASED 或 DISCARDED。
     */
    public enum AllocationStatus {
        ALLOCATED("ALLOCATED", "已分配", false),
        CONSUMED("CONSUMED", "已消费", true),
        RELEASED("RELEASED", "已释放", true),
        DISCARDED("DISCARDED", "已废弃", true);

        private final String value;

        private final String label;

        private final boolean terminal;

        AllocationStatus(String value, String label, boolean terminal) {
            this.value = value;
            this.label = label;
            this.terminal = terminal;
        }

        /**
         * 获取数据库持久化和接口传输使用的状态值。
         *
         * @return 状态值。
         */
        public String getValue() {
            return value;
        }

        /**
         * 获取后台页面展示使用的中文名称。
         *
         * @return 中文名称。
         */
        public String getLabel() {
            return label;
        }

        /**
         * 判断分配记录是否已经到达终态。
         *
         * @return true 表示终态。
         */
        public boolean isTerminal() {
            return terminal;
        }

        /**
         * 按持久化状态值解析枚举。
         *
         * @param value 状态值。
         * @return 匹配的枚举；未命中时返回 null。
         */
        public static AllocationStatus fromValue(String value) {
            if (!hasText(value)) {
                return null;
            }
            for (AllocationStatus status : values()) {
                if (status.value.equals(value)) {
                    return status;
                }
            }
            return null;
        }
    }

    /**
     * 协作单元状态。
     *
     * 业务功能：
     * 1. 描述多角色协作数据单元从创建、办理到整体重置或完成的状态。
     * 2. 支撑“协作整体重置”，避免只替换其中一个角色导致流程断裂。
     *
     * 关键流程：
     * 1. 协作单元创建后为 CREATED。
     * 2. 至少一个片段开始办理后为 IN_PROGRESS。
     * 3. 所有片段完成后为 COMPLETED。
     */
    public enum CollaborationStatus {
        CREATED("CREATED", "已创建", false),
        IN_PROGRESS("IN_PROGRESS", "办理中", false),
        COMPLETED("COMPLETED", "已完成", true),
        DISCARDED("DISCARDED", "已废弃", true),
        FAILED("FAILED", "失败", true);

        private final String value;

        private final String label;

        private final boolean terminal;

        CollaborationStatus(String value, String label, boolean terminal) {
            this.value = value;
            this.label = label;
            this.terminal = terminal;
        }

        /**
         * 获取数据库持久化和接口传输使用的状态值。
         *
         * @return 状态值。
         */
        public String getValue() {
            return value;
        }

        /**
         * 获取后台页面展示使用的中文名称。
         *
         * @return 中文名称。
         */
        public String getLabel() {
            return label;
        }

        /**
         * 判断协作单元是否已经到达终态。
         *
         * @return true 表示终态。
         */
        public boolean isTerminal() {
            return terminal;
        }

        /**
         * 按持久化状态值解析枚举。
         *
         * @param value 状态值。
         * @return 匹配的枚举；未命中时返回 null。
         */
        public static CollaborationStatus fromValue(String value) {
            if (!hasText(value)) {
                return null;
            }
            for (CollaborationStatus status : values()) {
                if (status.value.equals(value)) {
                    return status;
                }
            }
            return null;
        }
    }

    /**
     * 考试题目数据绑定状态。
     *
     * 业务功能：
     * 1. 描述考试每生每题数据从创建、锁定、提交到评分的状态。
     * 2. 支撑考试数据不可重置、不可复用和可追溯。
     *
     * 关键流程：
     * 1. 题目进入或开考准备后为 CREATED。
     * 2. 原平台数据锁定后为 LOCKED。
     * 3. 学生提交题目后为 SUBMITTED，评分完成后为 SCORED。
     */
    public enum ExamQuestionDataStatus {
        CREATED("CREATED", "已创建", false),
        LOCKED("LOCKED", "已锁定", false),
        SUBMITTED("SUBMITTED", "已提交", false),
        SCORED("SCORED", "已评分", true),
        ABNORMAL("ABNORMAL", "异常", true);

        private final String value;

        private final String label;

        private final boolean terminal;

        ExamQuestionDataStatus(String value, String label, boolean terminal) {
            this.value = value;
            this.label = label;
            this.terminal = terminal;
        }

        /**
         * 获取数据库持久化和接口传输使用的状态值。
         *
         * @return 状态值。
         */
        public String getValue() {
            return value;
        }

        /**
         * 获取后台页面展示使用的中文名称。
         *
         * @return 中文名称。
         */
        public String getLabel() {
            return label;
        }

        /**
         * 判断考试题目数据绑定是否到达终态。
         *
         * @return true 表示终态。
         */
        public boolean isTerminal() {
            return terminal;
        }

        /**
         * 按持久化状态值解析枚举。
         *
         * @param value 状态值。
         * @return 匹配的枚举；未命中时返回 null。
         */
        public static ExamQuestionDataStatus fromValue(String value) {
            if (!hasText(value)) {
                return null;
            }
            for (ExamQuestionDataStatus status : values()) {
                if (status.value.equals(value)) {
                    return status;
                }
            }
            return null;
        }
    }

    /**
     * 原平台启动上下文状态。
     *
     * 业务功能：
     * 1. 描述 platform_launch_context 中一次 launchToken 从创建、校验、使用到失败或过期的生命周期。
     * 2. 支撑原平台 verify token、SDK 上下文下发和启动审计。
     *
     * 关键流程：
     * 1. 教学平台生成 launchToken 后为 CREATED。
     * 2. 原平台 verify token 成功后为 VERIFIED。
     * 3. 原平台完成 session 建立后为 USED。
     * 4. 超时或启动失败后为 EXPIRED 或 FAILED。
     */
    public enum LaunchStatus {
        CREATED("CREATED", "已创建", false),
        VERIFIED("VERIFIED", "已校验", false),
        USED("USED", "已使用", true),
        EXPIRED("EXPIRED", "已过期", true),
        FAILED("FAILED", "失败", true);

        private final String value;

        private final String label;

        private final boolean terminal;

        LaunchStatus(String value, String label, boolean terminal) {
            this.value = value;
            this.label = label;
            this.terminal = terminal;
        }

        /**
         * 获取数据库持久化和接口传输使用的状态值。
         *
         * @return 状态值。
         */
        public String getValue() {
            return value;
        }

        /**
         * 获取后台页面展示使用的中文名称。
         *
         * @return 中文名称。
         */
        public String getLabel() {
            return label;
        }

        /**
         * 判断启动上下文是否已经不能继续推进。
         *
         * @return true 表示终态。
         */
        public boolean isTerminal() {
            return terminal;
        }

        /**
         * 按持久化状态值解析枚举。
         *
         * @param value 状态值。
         * @return 匹配的枚举；未命中时返回 null。
         */
        public static LaunchStatus fromValue(String value) {
            if (!hasText(value)) {
                return null;
            }
            for (LaunchStatus status : values()) {
                if (status.value.equals(value)) {
                    return status;
                }
            }
            return null;
        }
    }

    /**
     * 原平台数据校验状态。
     *
     * 业务功能：
     * 1. 表达原平台业务数据是否满足指定单位、角色、状态和动作要求。
     * 2. 防止未校验或校验失败的数据被生成 launchToken 进入原平台。
     *
     * 关键流程：
     * 1. 原平台未返回校验结果时为 NOT_CHECKED 或 UNKNOWN。
     * 2. 可见、可操作、状态匹配时为 PASSED。
     * 3. 任一强约束不满足时为 FAILED。
     */
    public enum ValidationStatus {
        NOT_CHECKED("NOT_CHECKED", "未校验", false),
        PASSED("PASSED", "通过", true),
        FAILED("FAILED", "失败", false),
        PARTIAL("PARTIAL", "部分满足", false),
        UNKNOWN("UNKNOWN", "未知", false);

        private final String value;

        private final String label;

        private final boolean launchAllowed;

        ValidationStatus(String value, String label, boolean launchAllowed) {
            this.value = value;
            this.label = label;
            this.launchAllowed = launchAllowed;
        }

        /**
         * 获取数据库持久化和接口传输使用的状态值。
         *
         * @return 状态值。
         */
        public String getValue() {
            return value;
        }

        /**
         * 获取后台页面展示使用的中文名称。
         *
         * @return 中文名称。
         */
        public String getLabel() {
            return label;
        }

        /**
         * 判断该校验状态是否允许生成进入原平台的 launchToken。
         *
         * @return true 表示允许进入原平台。
         */
        public boolean isLaunchAllowed() {
            return launchAllowed;
        }

        /**
         * 按持久化状态值解析枚举。
         *
         * @param value 状态值。
         * @return 匹配的枚举；未命中时返回 null。
         */
        public static ValidationStatus fromValue(String value) {
            if (!hasText(value)) {
                return null;
            }
            for (ValidationStatus status : values()) {
                if (status.value.equals(value)) {
                    return status;
                }
            }
            return null;
        }
    }

    /**
     * 通用记录状态。
     *
     * 业务功能：
     * 1. 统一表达多数业务表的 status 字段，避免 ACTIVE、DISABLED 等字符串散落。
     * 2. 与 deleted 软删除字段配合使用，status 表达业务启停，deleted 表达逻辑删除。
     *
     * 关键流程：
     * 1. 创建记录时默认为 ACTIVE。
     * 2. 管理后台启停业务模块、模板或策略时切换 ACTIVE 和 DISABLED。
     */
    public enum RecordStatus {
        ACTIVE("ACTIVE", "启用"),
        DISABLED("DISABLED", "停用");

        private final String value;

        private final String label;

        RecordStatus(String value, String label) {
            this.value = value;
            this.label = label;
        }

        /**
         * 获取数据库持久化和接口传输使用的状态值。
         *
         * @return 状态值。
         */
        public String getValue() {
            return value;
        }

        /**
         * 获取后台页面展示使用的中文名称。
         *
         * @return 中文名称。
         */
        public String getLabel() {
            return label;
        }

        /**
         * 按持久化状态值解析枚举。
         *
         * @param value 状态值。
         * @return 匹配的枚举；未命中时返回 null。
         */
        public static RecordStatus fromValue(String value) {
            if (!hasText(value)) {
                return null;
            }
            for (RecordStatus status : values()) {
                if (status.value.equals(value)) {
                    return status;
                }
            }
            return null;
        }
    }

    /**
     * 教学数据准备场景。
     *
     * 业务功能：
     * 1. 定义数据准备策略可绑定的教学场景，避免备案、学习、练习和考试之间策略误用。
     * 2. 为策略启用校验、数据需求生成和运行时数据隔离提供稳定场景边界。
     *
     * 关键流程：
     * 1. 后台创建策略时必须选择一个场景。
     * 2. 运行态按模块编码和场景查询启用策略。
     * 3. 考试场景后续会触发更严格的独占和锁定校验。
     */
    public enum StrategySceneType {
        RECORD("RECORD", "备案"),
        LEARN("LEARN", "学习"),
        PRACTICE("PRACTICE", "练习"),
        EXAM("EXAM", "考试");

        private final String value;

        private final String label;

        StrategySceneType(String value, String label) {
            this.value = value;
            this.label = label;
        }

        /**
         * 获取数据库持久化和接口传输使用的场景值。
         *
         * @return 场景值。
         */
        public String getValue() {
            return value;
        }

        /**
         * 获取后台页面展示使用的中文名称。
         *
         * @return 中文名称。
         */
        public String getLabel() {
            return label;
        }

        /**
         * 按持久化场景值解析枚举。
         *
         * @param value 场景值。
         * @return 匹配的枚举；未命中时返回 null。
         */
        public static StrategySceneType fromValue(String value) {
            if (!hasText(value)) {
                return null;
            }
            for (StrategySceneType sceneType : values()) {
                if (sceneType.value.equals(value)) {
                    return sceneType;
                }
            }
            return null;
        }
    }

    /**
     * 数据准备时机。
     *
     * 业务功能：
     * 1. 定义数据准备发生在发布时、开始前还是按需领取时。
     * 2. 为任务发布、考试开考和学生领取数据提供清晰事务边界。
     *
     * 关键流程：
     * 1. 发布时准备用于教师发布任务后提前生成数据。
     * 2. 开始前准备用于考试或训练开始前统一预热。
     * 3. 按需准备用于学生进入练习或题目时再生成数据。
     */
    public enum StrategyPrepareTiming {
        ON_PUBLISH("ON_PUBLISH", "发布时"),
        BEFORE_START("BEFORE_START", "开始前"),
        ON_DEMAND("ON_DEMAND", "按需准备");

        private final String value;

        private final String label;

        StrategyPrepareTiming(String value, String label) {
            this.value = value;
            this.label = label;
        }

        /**
         * 获取数据库持久化和接口传输使用的准备时机值。
         *
         * @return 准备时机值。
         */
        public String getValue() {
            return value;
        }

        /**
         * 获取后台页面展示使用的中文名称。
         *
         * @return 中文名称。
         */
        public String getLabel() {
            return label;
        }

        /**
         * 按持久化准备时机值解析枚举。
         *
         * @param value 准备时机值。
         * @return 匹配的枚举；未命中时返回 null。
         */
        public static StrategyPrepareTiming fromValue(String value) {
            if (!hasText(value)) {
                return null;
            }
            for (StrategyPrepareTiming timing : values()) {
                if (timing.value.equals(value)) {
                    return timing;
                }
            }
            return null;
        }
    }

    /**
     * 数据共享策略。
     *
     * 业务功能：
     * 1. 定义原平台业务数据在学生、attempt 和考试题目之间是否允许共享。
     * 2. 保护练习重置和考试每题独占的数据隔离边界。
     *
     * 关键流程：
     * 1. 学习通常使用只读共享。
     * 2. 练习通常使用 attempt 独占。
     * 3. 考试题目通常使用题目独占。
     */
    public enum StrategySharePolicy {
        SHARED_READONLY("SHARED_READONLY", "只读共享"),
        STUDENT_EXCLUSIVE("STUDENT_EXCLUSIVE", "学生独占"),
        ATTEMPT_EXCLUSIVE("ATTEMPT_EXCLUSIVE", "练习独占"),
        QUESTION_EXCLUSIVE("QUESTION_EXCLUSIVE", "题目独占");

        private final String value;

        private final String label;

        StrategySharePolicy(String value, String label) {
            this.value = value;
            this.label = label;
        }

        /**
         * 获取数据库持久化和接口传输使用的共享策略值。
         *
         * @return 共享策略值。
         */
        public String getValue() {
            return value;
        }

        /**
         * 获取后台页面展示使用的中文名称。
         *
         * @return 中文名称。
         */
        public String getLabel() {
            return label;
        }

        /**
         * 按持久化共享策略值解析枚举。
         *
         * @param value 共享策略值。
         * @return 匹配的枚举；未命中时返回 null。
         */
        public static StrategySharePolicy fromValue(String value) {
            if (!hasText(value)) {
                return null;
            }
            for (StrategySharePolicy policy : values()) {
                if (policy.value.equals(value)) {
                    return policy;
                }
            }
            return null;
        }
    }

    /**
     * 数据重生成策略。
     *
     * 业务功能：
     * 1. 定义练习重置、补考或失败重试时是否允许生成新的原平台业务数据。
     * 2. 防止重置时复用旧数据导致状态污染或评分串扰。
     *
     * 关键流程：
     * 1. 不允许重建时复用或拒绝重置。
     * 2. 按 attempt 重建用于练习重置。
     * 3. 按失败重建用于原平台创建失败后的补偿。
     */
    public enum StrategyRegeneratePolicy {
        NEVER("NEVER", "不重建"),
        ON_ATTEMPT("ON_ATTEMPT", "每次练习重建"),
        ON_RETAKE("ON_RETAKE", "补考重建"),
        ON_FAILURE("ON_FAILURE", "失败重建");

        private final String value;

        private final String label;

        StrategyRegeneratePolicy(String value, String label) {
            this.value = value;
            this.label = label;
        }

        /**
         * 获取数据库持久化和接口传输使用的重生成策略值。
         *
         * @return 重生成策略值。
         */
        public String getValue() {
            return value;
        }

        /**
         * 获取后台页面展示使用的中文名称。
         *
         * @return 中文名称。
         */
        public String getLabel() {
            return label;
        }

        /**
         * 按持久化重生成策略值解析枚举。
         *
         * @param value 重生成策略值。
         * @return 匹配的枚举；未命中时返回 null。
         */
        public static StrategyRegeneratePolicy fromValue(String value) {
            if (!hasText(value)) {
                return null;
            }
            for (StrategyRegeneratePolicy policy : values()) {
                if (policy.value.equals(value)) {
                    return policy;
                }
            }
            return null;
        }
    }

    /**
     * 数据锁定策略。
     *
     * 业务功能：
     * 1. 定义数据领取或考试开始时是否调用原平台锁定能力。
     * 2. 保护考试和强约束练习中原平台业务数据不被并发修改。
     *
     * 关键流程：
     * 1. 无锁定用于学习和普通练习。
     * 2. 领取锁定用于学生拿到数据后立即阻止他人修改。
     * 3. 考试开始锁定用于开考后固定每题数据状态。
     */
    public enum StrategyLockPolicy {
        NONE("NONE", "不锁定"),
        ON_ALLOCATE("ON_ALLOCATE", "领取时锁定"),
        ON_EXAM_START("ON_EXAM_START", "考试开始锁定");

        private final String value;

        private final String label;

        StrategyLockPolicy(String value, String label) {
            this.value = value;
            this.label = label;
        }

        /**
         * 获取数据库持久化和接口传输使用的锁定策略值。
         *
         * @return 锁定策略值。
         */
        public String getValue() {
            return value;
        }

        /**
         * 获取后台页面展示使用的中文名称。
         *
         * @return 中文名称。
         */
        public String getLabel() {
            return label;
        }

        /**
         * 按持久化锁定策略值解析枚举。
         *
         * @param value 锁定策略值。
         * @return 匹配的枚举；未命中时返回 null。
         */
        public static StrategyLockPolicy fromValue(String value) {
            if (!hasText(value)) {
                return null;
            }
            for (StrategyLockPolicy policy : values()) {
                if (policy.value.equals(value)) {
                    return policy;
                }
            }
            return null;
        }
    }

    /**
     * 数据准备任务触发方式。
     *
     * 业务功能：
     * 1. 区分数据准备任务是人工触发、发布触发、学生按需触发、失败重试还是系统任务。
     * 2. 为幂等键生成、审计查询和失败补偿提供稳定分类。
     *
     * 关键流程：
     * 1. 管理员或教师手动准备数据时使用 MANUAL。
     * 2. 任务发布时预生成数据使用 PUBLISH。
     * 3. 学生开始练习或进入考试题目时按需生成数据使用 ON_DEMAND。
     * 4. 失败任务重试使用 RETRY。
     */
    public enum TriggerType {
        MANUAL("MANUAL", "人工触发"),
        PUBLISH("PUBLISH", "发布触发"),
        ON_DEMAND("ON_DEMAND", "按需触发"),
        RETRY("RETRY", "失败重试"),
        SYSTEM("SYSTEM", "系统触发");

        private final String value;

        private final String label;

        TriggerType(String value, String label) {
            this.value = value;
            this.label = label;
        }

        /**
         * 获取数据库持久化和接口传输使用的触发方式值。
         *
         * @return 触发方式值。
         */
        public String getValue() {
            return value;
        }

        /**
         * 获取后台页面展示使用的中文名称。
         *
         * @return 中文名称。
         */
        public String getLabel() {
            return label;
        }

        /**
         * 按持久化触发方式值解析枚举。
         *
         * @param value 触发方式值。
         * @return 匹配的枚举；未命中时返回 null。
         */
        public static TriggerType fromValue(String value) {
            if (!hasText(value)) {
                return null;
            }
            for (TriggerType triggerType : values()) {
                if (triggerType.value.equals(value)) {
                    return triggerType;
                }
            }
            return null;
        }
    }
}

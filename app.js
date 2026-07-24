/**
 * ==========================================================================
 * MINDSHIELD - 核心状态机与交互控制器
 * 包含 Mock AI 意图判定引擎、物理翻转传感器模拟、打字摩擦力算法
 * ==========================================================================
 */

// ==================== 状态定义与全局变量 ====================
const STATES = {
    DASHBOARD: 'view-dashboard',
    INTENT_INPUT: 'view-intent',
    AI_ANALYZING: 'view-analyzing',
    FLIP_PROMPT: 'view-flip-prompt',
    FOCUSING: 'focusing', // 此时手机处于 Flipped 状态（背面朝上）
    COMPLETED: 'view-completed'
};

let currentState = STATES.DASHBOARD;
let focusDuration = 10; // 默认 10 秒体验版，可通过界面选择
let timerInterval = null;
let secondsRemaining = 0;
let totalInterventions = 0; // 统计在本次专注期内试图分心（拿起手机）的次数
let currentIntentText = "";

// ==================== Mock 数据库：AI 解析字典 ====================
// 区分“生产性意图”和“娱乐性分心意图”，模拟 AI 的自然语言理解能力
const PRODUCTIVE_KEYWORDS = ['写代码', '开发', '学习', '阅读', '写论文', '背单词', '工作', '思考', '写日记', 'coding', 'study'];
const UNPRODUCTIVE_KEYWORDS = ['刷抖音', '打游戏', '刷视频', '看小说', '逛淘宝', '闲聊', '刷朋友圈', '微博', '游戏'];

// ==================== DOM 元素获取 ====================
const phoneInner = document.getElementById('phone-inner');
const phoneContainer = document.getElementById('phone-container');
const dynamicIsland = document.getElementById('dynamic-island');
const islandText = document.getElementById('island-text');
const statusTime = document.getElementById('status-time');
const externalStatus = document.getElementById('external-status');
const statusText = document.getElementById('status-text');
const statusWifi = document.getElementById('status-wifi');

const viewDashboard = document.getElementById('view-dashboard');
const viewIntent = document.getElementById('view-intent');
const viewAnalyzing = document.getElementById('view-analyzing');
const viewFlipPrompt = document.getElementById('view-flip-prompt');
const viewCompleted = document.getElementById('view-completed');
const interventionOverlay = document.getElementById('intervention-overlay');

const btnStartSession = document.getElementById('btn-start-session');
const btnIntentBack = document.getElementById('btn-intent-back');
const btnSubmitIntent = document.getElementById('btn-submit-intent');
const intentInput = document.getElementById('intent-input');
const durationCards = document.querySelectorAll('.duration-card');

const scrollingLog = document.getElementById('scrolling-log');
const analyzingResult = document.getElementById('analyzing-result');
const recapIntentText = document.getElementById('recap-intent-text');
const whitelistApps = document.getElementById('whitelist-apps');
const blacklistApps = document.getElementById('blacklist-apps');
const btnConfirmAnalysis = document.getElementById('btn-confirm-analysis');
const btnSimulateFlip = document.getElementById('btn-simulate-flip');

const backCountdown = document.getElementById('back-countdown');
const simulatorControls = document.getElementById('simulator-controls');
const btnSimLift = document.getElementById('btn-sim-lift');

const interventionIntentText = document.getElementById('intervention-intent-text');
const frictionReasonInput = document.getElementById('friction-reason-input');
const currentCharCount = document.getElementById('current-char-count');
const charProgressBar = document.getElementById('char-progress-bar');
const btnAbandonDeviation = document.getElementById('btn-abandon-deviation');
const btnSubmitDeviation = document.getElementById('btn-submit-deviation');

const completedIntentName = document.getElementById('completed-intent-name');
const completedTimeVal = document.getElementById('completed-time-val');
const completedInterventionsVal = document.getElementById('completed-interventions-val');
const evaluationText = document.getElementById('evaluation-text');
const btnFinishAll = document.getElementById('btn-finish-all');

// ==================== 初始化与系统时钟 ====================
document.addEventListener('DOMContentLoaded', () => {
    updateSystemTime();
    setInterval(updateSystemTime, 60000); // 每一分钟同步一次虚拟手机顶部的系统时钟
    registerEventListeners();
});

/**
 * 业务功能：同步手机状态栏的时间显示
 * 关键方法：Date() 获取当前真实的物理时间，并格式化为 iOS 经典的 HH:MM
 */
function updateSystemTime() {
    const now = new Date();
    let hours = now.getHours();
    let minutes = now.getMinutes();
    hours = hours < 10 ? '0' + hours : hours;
    minutes = minutes < 10 ? '0' + minutes : minutes;
    statusTime.textContent = `${hours}:${minutes}`;
}

// ==================== 状态机控制器 ====================

/**
 * 业务功能：负责虚拟手机各视图切换的核心路由
 * 关键流程：
 * 1. 隐藏所有屏幕视图，重置滑动动画基础位移。
 * 2. 根据 viewId 选择性激活对应视图。
 * 3. 动态控制顶部灵动岛（Dynamic Island）和外部辅助调试面板的视觉交互。
 */
function switchView(viewId) {
    // 隐藏所有视图
    [viewDashboard, viewIntent, viewAnalyzing, viewFlipPrompt, viewCompleted].forEach(view => {
        view.classList.remove('active');
    });

    if (viewId === STATES.FOCUSING) {
        // 手机物理翻转为背面朝上，不展示任何正面视图
        externalStatus.className = "status-badge status-active";
        statusText.textContent = "隔离中";
        statusWifi.className = "ri-wifi-off-line"; // 模拟 VPN 物理断网，WIFI 图标断开
        dynamicIsland.classList.add('focusing');
        islandText.textContent = "已进入物理隔离";
        simulatorControls.style.display = "block"; // 展现物理拿起调试面板
    } else {
        // 正面视图正常展现
        const targetView = document.getElementById(viewId);
        if (targetView) targetView.classList.add('active');
        
        externalStatus.className = "status-badge status-idle";
        statusText.textContent = "未隔离";
        statusWifi.className = "ri-wifi-line"; // 恢复网络连接
        dynamicIsland.classList.remove('focusing');
        islandText.textContent = "MindShield";
        simulatorControls.style.display = "none"; // 隐藏拿起手机的调试按钮
    }
    
    currentState = viewId;
}

// ==================== 事件监听注册器 ====================
function registerEventListeners() {
    // 1. Dashboard 交互
    btnStartSession.addEventListener('click', () => {
        switchView(STATES.INTENT_INPUT);
        intentInput.focus();
    });

    // 2. 意图声明页面交互
    btnIntentBack.addEventListener('click', () => {
        switchView(STATES.DASHBOARD);
    });

    // 监听输入，若意图过短则禁止提交，确保意图声明的诚实性与具体性
    intentInput.addEventListener('input', (e) => {
        const text = e.target.value.trim();
        btnSubmitIntent.disabled = text.length < 4; // 长度小于 4 个字不允许解析，强迫用户思考
    });

    // 选择专注时长卡片
    durationCards.forEach(card => {
        card.addEventListener('click', () => {
            durationCards.forEach(c => c.classList.remove('active'));
            card.classList.add('active');
            focusDuration = parseInt(card.getAttribute('data-time'), 10);
        });
    });

    // 提交意图，启动 Mock AI 解析
    btnSubmitIntent.addEventListener('click', () => {
        currentIntentText = intentInput.value.trim();
        switchView(STATES.AI_ANALYZING);
        runMockAIAnalysis(currentIntentText);
    });

    // 3. AI 解析确认，进入物理翻转提示
    btnConfirmAnalysis.addEventListener('click', () => {
        switchView(STATES.FLIP_PROMPT);
    });

    // 4. 模拟翻转平放动作
    btnSimulateFlip.addEventListener('click', () => {
        // 触发 3D 翻转到背面
        phoneInner.classList.add('flipped');
        switchView(STATES.FOCUSING);
        startFocusTimer();
    });

    // 5. 调试面板：模拟拿起手机（破坏物理隔离）
    btnSimLift.addEventListener('click', () => {
        triggerIntervention();
    });

    // 6. 打字摩擦力干预事件监听
    frictionReasonInput.addEventListener('input', handleFrictionInput);

    // 坚守意图：重新将手机反转为屏幕朝下
    btnAbandonDeviation.addEventListener('click', () => {
        interventionOverlay.classList.remove('active');
        phoneInner.classList.add('flipped');
        switchView(STATES.FOCUSING);
        resumeFocusTimer();
    });

    // 确认偏离意图：惩罚性扣除时间，并直接中断
    btnSubmitDeviation.addEventListener('click', () => {
        terminateSessionByFailure();
    });

    // 7. 完成报告返回
    btnFinishAll.addEventListener('click', () => {
        switchView(STATES.DASHBOARD);
        resetSystemData();
    });

    // 8. 运行系统自动化测试套件
    const btnRunTests = document.getElementById('btn-run-tests');
    if (btnRunTests) {
        btnRunTests.addEventListener('click', () => {
            if (typeof TestSuite !== 'undefined') {
                TestSuite.runAll();
            } else {
                console.error("TestSuite 未找到，请确保已引入 test_suite.js");
            }
        });
    }
}

// ==================== Mock AI 语义分析引擎 ====================

/**
 * 业务功能：模拟 LLM 模型对专注意图的识别，提供动态白名单与策略
 * 关键方法：关键词扫描归类，配以炫酷的命令行滚动日志输出以营造 AI 正在高维计算的视觉感。
 */
function runMockAIAnalysis(intent) {
    // 重置面板状态
    analyzingResult.style.display = "none";
    scrollingLog.style.display = "block";
    
    const logs = [
        "正在建立意图神经网络...",
        "正在对意图文本进行中文分词与语义分析...",
        "提取核心实体与动作语义...",
        "正在查询安全沙箱应用分类数据库...",
        "正在生成动态防火墙过滤规则...",
        "分析完成！已应用网络隔离策略。"
    ];

    let logIndex = 0;
    scrollingLog.textContent = logs[0];

    // 通过循环计时器模拟日志的逐步喷涌
    const logInterval = setInterval(() => {
        logIndex++;
        if (logIndex < logs.length) {
            scrollingLog.textContent = logs[logIndex];
        } else {
            clearInterval(logInterval);
            displayAIAnalysisResult(intent);
        }
    }, 450);
}

/**
 * 业务功能：展示 AI 分析出的白名单与拦截结果
 * 关键流程：判定意图是属于学习性（放行工作软件，封锁娱乐）还是偏向娱乐性（提示警告）。
 */
function displayAIAnalysisResult(intent) {
    scrollingLog.style.display = "none";
    analyzingResult.style.display = "flex";
    recapIntentText.textContent = `“${intent}”`;

    // 默认列表
    let whitelist = ["VS Code", "终端", "Github", "StackOverflow"];
    let blacklist = ["小红书", "抖音/快手", "和平精英/王者荣耀", "朋友圈/聊天群"];

    // 检测是否包含娱乐性词汇
    const isUnproductive = UNPRODUCTIVE_KEYWORDS.some(keyword => intent.includes(keyword));
    const isProductive = PRODUCTIVE_KEYWORDS.some(keyword => intent.includes(keyword));

    if (isUnproductive) {
        // 如果声明的意图本身就偏向娱乐，AI 给出强拦截警示，并将其全部拉黑
        whitelist = ["无。当前意图被系统识别为娱乐/低价值行为"];
        blacklist = ["全网社交媒体", "全域手机游戏", "视频流媒体解析"];
        recapIntentText.style.color = "var(--color-neon-pink)";
        
        // 动态修改提示卡片的背景色，警示用户
        analyzingResult.querySelector('.intent-recap').style.borderLeftColor = "var(--color-neon-pink)";
    } else if (isProductive) {
        // 匹配到高价值工作意图，定向定制白名单
        if (intent.includes('单词') || intent.includes('学习')) {
            whitelist = ["背单词APP", "有道词典", "电子书阅读器", "系统设置"];
        } else if (intent.includes('论文') || intent.includes('阅读')) {
            whitelist = ["PDF阅读器", "学术浏览器 (arXiv)", "Notion", "备忘录"];
        }
        recapIntentText.style.color = "var(--color-electric-blue)";
        analyzingResult.querySelector('.intent-recap').style.borderLeftColor = "var(--color-electric-blue)";
    } else {
        // 中性意图，使用通用白名单
        recapIntentText.style.color = "var(--color-electric-blue)";
        analyzingResult.querySelector('.intent-recap').style.borderLeftColor = "var(--color-electric-blue)";
    }

    // 动态渲染白名单 DOM
    whitelistApps.innerHTML = whitelist.map(app => `<span>${app}</span>`).join('');
    blacklistApps.innerHTML = blacklist.map(app => `<span>${app}</span>`).join('');
}

// ==================== 专注倒计时模块 ====================

/**
 * 业务功能：开始专注计时
 * 关键方法：setInterval 每秒递减，并输出等宽格式的剩余时间，保障视觉上倒计时不会抖动。
 */
function startFocusTimer() {
    secondsRemaining = focusDuration;
    updateTimerDisplay();
    
    timerInterval = setInterval(() => {
        secondsRemaining--;
        updateTimerDisplay();
        
        if (secondsRemaining <= 0) {
            clearInterval(timerInterval);
            completeFocusSession();
        }
    }, 1000);
}

function pauseFocusTimer() {
    if (timerInterval) {
        clearInterval(timerInterval);
    }
}

function resumeFocusTimer() {
    timerInterval = setInterval(() => {
        secondsRemaining--;
        updateTimerDisplay();
        
        if (secondsRemaining <= 0) {
            clearInterval(timerInterval);
            completeFocusSession();
        }
    }, 1000);
}

/**
 * 业务功能：格式化输出剩余时间（MM:SS）
 */
function updateTimerDisplay() {
    const mins = Math.floor(secondsRemaining / 60);
    const secs = secondsRemaining % 60;
    const timeStr = `${mins < 10 ? '0' : ''}${mins}:${secs < 10 ? '0' : ''}${secs}`;
    backCountdown.textContent = timeStr;
}

// ==================== 物理隔离偏离与打字摩擦力干预算法 ====================

/**
 * 业务功能：触发行为干预（模拟拿起手机）
 * 关键流程：
 * 1. 终止计时，进行 Y 轴 3D 翻转归位，展现正面屏幕。
 * 2. 展现磨砂玻璃阻断幕墙。
 * 3. 重置打字输入框及校验按钮。
 */
function triggerIntervention() {
    totalInterventions++;
    pauseFocusTimer();
    
    // 手机 3D 翻转回正面
    phoneInner.classList.remove('flipped');
    
    // 激活全屏阻断幕墙
    interventionIntentText.textContent = `“${currentIntentText}”`;
    interventionOverlay.classList.add('active');
    
    // 重置打字框状态
    frictionReasonInput.value = "";
    currentCharCount.textContent = "0";
    charProgressBar.style.width = "0%";
    charProgressBar.style.backgroundColor = "var(--color-neon-pink)";
    btnSubmitDeviation.disabled = true;
}

/**
 * 业务功能：实现打字摩擦力校验算法，限制绕过行为
 * 物理原理：通过监听字符输入，强制要求字数在 15 字以上以激活系统 2 思考。
 * 关键算法：
 * 1. 统计去除首尾空格后的真实字符长度。
 * 2. 计算打字进度百分比：(当前字数 / 15) * 100。
 * 3. 进度条颜色平滑过渡：0-7字为红色（极度分心），8-14字为黄色（挣扎期），>=15字为绿色（理性辩护就绪，按钮解锁）。
 */
function handleFrictionInput(e) {
    const text = e.target.value.trim();
    const count = text.length;
    
    currentCharCount.textContent = count;
    
    // 计算百分比并封顶在 100%
    const percent = Math.min((count / 15) * 100, 100);
    charProgressBar.style.width = `${percent}%`;
    
    // 渐变状态过渡
    if (count < 8) {
        charProgressBar.style.backgroundColor = "var(--color-neon-pink)"; // 红色，距离解锁尚远
    } else if (count < 15) {
        charProgressBar.style.backgroundColor = "#FFEB3B"; // 黄色，已展现初步理性思考
    } else {
        charProgressBar.style.backgroundColor = "var(--color-green-success)"; // 绿色，思考合格
        btnSubmitDeviation.disabled = false; // 激活解锁选项
    }
}

/**
 * 业务功能：处理非理性退出/退出失败的惩罚机制
 * 关键流程：终止专注，扣除本次记录，直接以“意图破裂”结果返回 Dashboard。
 */
function terminateSessionByFailure() {
    if (timerInterval) clearInterval(timerInterval);
    interventionOverlay.classList.remove('active');
    
    // 模拟强制锁屏或中断惩罚：显示提示并返回主页
    alert(`专注已强行中断！由于您承认了意图偏离并提交了解锁，本次意图“${currentIntentText}”未达成，网络环回隧道已重置。`);
    
    switchView(STATES.DASHBOARD);
    resetSystemData();
}

/**
 * 业务功能：计时顺利结束，达成专注意图
 * 关键流程：
 * 1. 翻转回正面，关闭 VPN 指示。
 * 2. 切换至完成报告页面，动态计算评估结果。
 */
function completeFocusSession() {
    phoneInner.classList.remove('flipped');
    switchView(STATES.COMPLETED);
    
    completedIntentName.textContent = `“${currentIntentText}”`;
    
    // 渲染耗时信息
    const totalMinutes = Math.ceil(focusDuration / 60);
    completedTimeVal.textContent = focusDuration >= 60 ? `${totalMinutes} 分钟` : `${focusDuration} 秒`;
    completedInterventionsVal.textContent = `${totalInterventions} 次`;
    
    // 根据在计时阶段被触发拿起手机的次数，输出差异化的多维度评估报告
    if (totalInterventions === 0) {
        evaluationText.textContent = "完美自控！在本次意图期间，您一次也没有拿起过手机，物理隔离防线固若金汤，AI 给予您 100 分专注力勋章。";
        evaluationText.style.color = "var(--color-green-success)";
    } else if (totalInterventions <= 2) {
        evaluationText.textContent = `勉强坚持。本次意图期间您拿起了 ${totalInterventions} 次手机，但最终通过打字辩护克制了欲望并重新回到专注。专注防线较为脆弱，仍需努力。`;
        evaluationText.style.color = "#FFEB3B";
    } else {
        evaluationText.textContent = `多次失守。虽然您完成了时间积累，但在专注期内您高达 ${totalInterventions} 次被无意识冲动打断，过度依赖打字辩护。建议下次开启强阻断模式。`;
        evaluationText.style.color = "var(--color-neon-pink)";
    }
}

// ==================== 辅助重置工具 ====================
function resetSystemData() {
    totalInterventions = 0;
    currentIntentText = "";
    intentInput.value = "";
    btnSubmitIntent.disabled = true;
}

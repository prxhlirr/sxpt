/**
 * ==========================================================================
 * MINDSHIELD - 自动化测试套件
 * 包含针对状态机转换、AI 意图解析分类和打字摩擦力校验的断言测试
 * ==========================================================================
 */

const TestSuite = {
    // 存储测试运行结果
    results: [],

    // 简单断言工具
    assert(condition, message) {
        if (condition) {
            this.results.push({ success: true, message: `[PASS] ${message}` });
        } else {
            this.results.push({ success: false, message: `[FAIL] ${message}` });
            console.error(`[Assertion Failed] ${message}`);
        }
    },

    /**
     * 测试用例 1：验证状态机路由切换
     * 业务功能：切换视图后，系统的当前状态变量必须更新，且状态角标的视觉效果必须对应改变
     */
    testSwitchView() {
        console.log("正在执行测试：testSwitchView...");
        
        // 动作：切换到意图输入视图
        switchView(STATES.INTENT_INPUT);
        this.assert(currentState === STATES.INTENT_INPUT, "状态成功切换为 INTENT_INPUT");
        this.assert(viewIntent.classList.contains('active'), "意图视图已被添加 active 类");
        this.assert(!viewDashboard.classList.contains('active'), "主面板视图已被移除 active 类");

        // 动作：切换到物理隔离运行中
        switchView(STATES.FOCUSING);
        this.assert(currentState === STATES.FOCUSING, "状态成功切换为 FOCUSING");
        this.assert(externalStatus.classList.contains('status-active'), "外部状态角标成功显示激活态");
        this.assert(dynamicIsland.classList.contains('focusing'), "灵动岛已被激活 focusing 类");

        // 恢复初始状态
        switchView(STATES.DASHBOARD);
    },

    /**
     * 测试用例 2：验证 Mock AI 意图判定引擎
     * 业务功能：根据意图的娱乐或生产属性，对白名单/黑名单进行分流拦截
     */
    testAIAnalysis() {
        console.log("正在执行测试：testAIAnalysis...");

        // 测试场景 A：高价值生产性意图
        const productiveIntent = "我要在VS Code中学习编写React CSS动画";
        displayAIAnalysisResult(productiveIntent);
        
        const isProductiveOk = Array.from(whitelistApps.children).some(el => el.textContent === "VS Code");
        this.assert(isProductiveOk, "生产性意图成功将 VS Code 添加至白名单");
        this.assert(recapIntentText.style.color === "var(--color-electric-blue)" || recapIntentText.style.color === "rgb(0, 242, 254)", "生产意图字体显蓝色");

        // 测试场景 B：低价值分心意图
        const unproductiveIntent = "我想看抖音视频和打一把游戏";
        displayAIAnalysisResult(unproductiveIntent);

        const isUnproductiveOk = Array.from(whitelistApps.children).some(el => el.textContent.includes("娱乐/低价值行为"));
        this.assert(isUnproductiveOk, "娱乐意图成功触发强管控逻辑，白名单为空置提示");
        this.assert(recapIntentText.style.color === "var(--color-neon-pink)" || recapIntentText.style.color === "rgb(243, 85, 136)", "娱乐意图字体显粉红警示色");
    },

    /**
     * 测试用例 3：验证打字认知摩擦力校验机制
     * 业务功能：低于 15 个字强力锁死提交按钮，大于等于 15 个字解锁
     */
    testFrictionFulfillment() {
        console.log("正在执行测试：testFrictionFulfillment...");

        // 测试场景 A：输入 5 个字（不达标）
        const eventShort = { target: { value: "我要玩手机" } };
        handleFrictionInput(eventShort);
        this.assert(btnSubmitDeviation.disabled === true, "输入少于15个字，解锁按钮仍然锁定");
        this.assert(charProgressBar.style.backgroundColor === "var(--color-neon-pink)" || charProgressBar.style.backgroundColor === "rgb(243, 85, 136)", "进度条呈现红色警告");

        // 测试场景 B：输入 18 个字（达标）
        const eventLong = { target: { value: "因为老板需要紧急修改这个登陆页的背景色，所以我得看下微信消息" } };
        handleFrictionInput(eventLong);
        this.assert(btnSubmitDeviation.disabled === false, "输入大于等于15个字，解锁按钮成功激活");
        this.assert(charProgressBar.style.backgroundColor === "var(--color-green-success)" || charProgressBar.style.backgroundColor === "rgb(0, 230, 118)", "进度条呈现绿色解锁色");
    },

    // 运行所有测试并渲染到测试浮板中
    runAll() {
        this.results = [];
        console.log("========== 开始执行 MindShield 原型测试套件 ==========");
        
        try {
            this.testSwitchView();
            this.testAIAnalysis();
            this.testFrictionFulfillment();
        } catch (e) {
            this.results.push({ success: false, message: `[CRITICAL ERROR] 测试套件运行崩溃: ${e.message}` });
        }

        console.log("========== 测试套件执行完毕 ==========");
        this.renderResults();
    },

    // 将测试报告渲染到界面左侧，提供直观的工程化质量保障
    renderResults() {
        const testReportBox = document.getElementById('test-report-box');
        if (!testReportBox) return;

        const total = this.results.length;
        const passed = this.results.filter(r => r.success).length;

        let html = `
            <div class="test-header">
                <strong>自动化测试报告</strong>
                <span class="${passed === total ? 'test-status-all-pass' : 'test-status-fail'}">
                    ${passed}/${total} 通过
                </span>
            </div>
            <div class="test-list">
        `;

        this.results.forEach(r => {
            html += `
                <div class="test-log-item ${r.success ? 'pass' : 'fail'}">
                    <i class="${r.success ? 'ri-checkbox-circle-fill' : 'ri-close-circle-fill'}"></i>
                    <span>${r.message}</span>
                </div>
            `;
        });

        html += `</div>`;
        testReportBox.innerHTML = html;
        testReportBox.style.display = "block";
    }
};

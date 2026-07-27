<template>
  <section class="page">
    <p class="eyebrow">演示路径</p>
    <h2>录制业务操作，生成教学流程</h2>
    <p class="lede">主体验：老师在业务系统中操作一遍，系统录制为流程节点；学生学习时自动演示，练习时按节点自行操作，考试时隐藏提示并记录轨迹。</p>
    <div class="metric-grid">
      <RouterLink class="module-card" to="/admin/nodes">
        <span>01</span>
        <strong>标准节点库</strong>
        <p>维护稳定的节点定义和默认完成方式。</p>
      </RouterLink>
      <RouterLink class="module-card" to="/admin/templates">
        <span>02</span>
        <strong>流程模板库</strong>
        <p>把节点组成线性标准流程。</p>
      </RouterLink>
      <RouterLink class="module-card" to="/teacher/lesson">
        <span>03</span>
        <strong>教案编排</strong>
        <p>复制模板并调整教学说明、分值和策略。</p>
      </RouterLink>
      <RouterLink class="module-card" to="/teacher/record">
        <span>04</span>
        <strong>录制编排</strong>
        <p>老师真实操作业务系统，自动生成可编辑流程节点。</p>
      </RouterLink>
      <RouterLink class="module-card" to="/student/runner">
        <span>05</span>
        <strong>引导运行</strong>
        <p>学习自动演示，练习自行操作，考试静默记录。</p>
      </RouterLink>
    </div>
  </section>
</template>

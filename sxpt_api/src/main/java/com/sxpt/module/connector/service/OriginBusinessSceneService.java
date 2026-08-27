package com.sxpt.module.connector.service;

import com.sxpt.module.connector.entity.PlatformLaunchContext;
import com.sxpt.module.connector.vo.OriginBusinessSceneVO;

import java.util.List;

/**
 * 原平台业务场景清单服务。
 *
 * 业务功能：
 * 1. 根据 launchToken 对应的教学上下文返回本次任务的业务场景清单。
 * 2. 支撑原平台首页或遮罩层展示待完成任务，避免学生进入原平台后猜菜单。
 *
 * 关键流程：
 * 1. verify launchToken 成功后传入 PlatformLaunchContext。
 * 2. Service 根据 sceneType 查询练习任务场景或考试题目场景。
 * 3. 返回不包含原平台路径的 businessSceneCode 清单。
 */
public interface OriginBusinessSceneService {

    /**
     * 查询本次启动上下文对应的业务场景清单。
     *
     * @param launchContext 已校验的启动上下文。
     * @return 业务场景清单。
     */
    List<OriginBusinessSceneVO> listScenesForLaunch(PlatformLaunchContext launchContext);
}

package com.sxpt.module.practice.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.practice.dto.FinishPracticeAttemptRequest;
import com.sxpt.module.practice.dto.StartPracticeAttemptRequest;
import com.sxpt.module.practice.entity.PracticeAttempt;
import com.sxpt.module.practice.service.PracticeAttemptService;
import com.sxpt.module.practice.vo.PracticeAttemptVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 练习次数接口。
 *
 * 业务功能：
 * 1. 提供学生开始一次练习的记录创建入口。
 * 2. 提供练习结束回写和按学生任务查询练习次数入口。
 *
 * 关键流程：
 * 1. 开始练习时 Controller 生成应用层主键并调用 Service 计算 attemptNo。
 * 2. 完成练习时 Controller 只传回写字段，Service 负责状态流转和耗时计算。
 */
@RestController
@RequestMapping("/api/v1/practice/attempts")
@ConditionalOnProperty(name = "sxpt.practice.attempt-controller.enabled",
        havingValue = "true", matchIfMissing = true)
public class PracticeAttemptController {

    private final PracticeAttemptService practiceAttemptService;

    public PracticeAttemptController(PracticeAttemptService practiceAttemptService) {
        this.practiceAttemptService = practiceAttemptService;
    }

    /**
     * 开始一次练习。
     *
     * @param request 开始练习请求。
     * @return 已创建的练习次数记录。
     */
    @PostMapping("/start")
    public ApiResult<PracticeAttemptVO> start(@Valid @RequestBody StartPracticeAttemptRequest request) {
        PracticeAttempt saved = practiceAttemptService.startAttempt(toStartEntity(request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 完成一次练习。
     *
     * @param request 完成练习请求。
     * @return 已完成的练习次数记录。
     */
    @PostMapping("/finish")
    public ApiResult<PracticeAttemptVO> finish(@Valid @RequestBody FinishPracticeAttemptRequest request) {
        PracticeAttempt saved = practiceAttemptService.finishAttempt(toFinishEntity(request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 按学生和任务查询练习次数。
     *
     * @param tenantId 租户 ID。
     * @param studentId 学生 ID。
     * @param taskId 任务 ID。
     * @return 练习次数列表。
     */
    @GetMapping
    public ApiResult<List<PracticeAttemptVO>> listByStudentAndTask(@RequestParam String tenantId,
                                                                   @RequestParam String studentId,
                                                                   @RequestParam String taskId) {
        return ApiResult.success(toVOList(practiceAttemptService.listByStudentAndTask(tenantId, studentId, taskId)));
    }

    /**
     * 将开始请求转换为数据库实体。
     *
     * @param request 开始练习请求。
     * @return 练习次数实体。
     */
    private PracticeAttempt toStartEntity(StartPracticeAttemptRequest request) {
        PracticeAttempt attempt = new PracticeAttempt();
        attempt.setId(generateId());
        attempt.setTenantId(request.getTenantId());
        attempt.setExecutionId(request.getExecutionId());
        attempt.setStudentId(request.getStudentId());
        attempt.setClassId(request.getClassId());
        attempt.setTaskId(request.getTaskId());
        attempt.setTeachingPointId(request.getTeachingPointId());
        attempt.setDataInstanceId(request.getDataInstanceId());
        attempt.setCreateBy(request.getStudentId());
        return attempt;
    }

    /**
     * 将完成请求转换为数据库实体。
     *
     * @param request 完成练习请求。
     * @return 练习次数实体。
     */
    private PracticeAttempt toFinishEntity(FinishPracticeAttemptRequest request) {
        PracticeAttempt attempt = new PracticeAttempt();
        attempt.setId(request.getId());
        attempt.setAttemptStatus(request.getAttemptStatus());
        attempt.setEndTime(request.getEndTime());
        attempt.setScore(request.getScore());
        attempt.setMaxScore(request.getMaxScore());
        attempt.setPassFlag(request.getPassFlag());
        attempt.setErrorCount(request.getErrorCount());
        attempt.setHintCount(request.getHintCount());
        attempt.setRollbackCount(request.getRollbackCount());
        attempt.setUpdateBy(request.getUpdateBy());
        return attempt;
    }

    /**
     * 将实体列表转换为返回对象列表。
     *
     * @param attempts 练习次数实体列表。
     * @return 练习次数返回对象列表。
     */
    private List<PracticeAttemptVO> toVOList(List<PracticeAttempt> attempts) {
        List<PracticeAttemptVO> result = new ArrayList<>();
        for (PracticeAttempt attempt : attempts) {
            result.add(toVO(attempt));
        }
        return result;
    }

    /**
     * 将实体转换为返回对象。
     *
     * @param attempt 练习次数实体。
     * @return 练习次数返回对象。
     */
    private PracticeAttemptVO toVO(PracticeAttempt attempt) {
        PracticeAttemptVO vo = new PracticeAttemptVO();
        vo.setId(attempt.getId());
        vo.setTenantId(attempt.getTenantId());
        vo.setExecutionId(attempt.getExecutionId());
        vo.setStudentId(attempt.getStudentId());
        vo.setClassId(attempt.getClassId());
        vo.setTaskId(attempt.getTaskId());
        vo.setTeachingPointId(attempt.getTeachingPointId());
        vo.setDataInstanceId(attempt.getDataInstanceId());
        vo.setAttemptNo(attempt.getAttemptNo());
        vo.setStartTime(attempt.getStartTime());
        vo.setEndTime(attempt.getEndTime());
        vo.setDurationSeconds(attempt.getDurationSeconds());
        vo.setAttemptStatus(attempt.getAttemptStatus());
        vo.setScore(attempt.getScore());
        vo.setMaxScore(attempt.getMaxScore());
        vo.setPassFlag(attempt.getPassFlag());
        vo.setErrorCount(attempt.getErrorCount());
        vo.setHintCount(attempt.getHintCount());
        vo.setRollbackCount(attempt.getRollbackCount());
        vo.setStatus(attempt.getStatus());
        vo.setCreateTime(attempt.getCreateTime());
        return vo;
    }

    /**
     * 生成应用层主键。
     *
     * @return 32 位无横线字符串 ID。
     */
    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}

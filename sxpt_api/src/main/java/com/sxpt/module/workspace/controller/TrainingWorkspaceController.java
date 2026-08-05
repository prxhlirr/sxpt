package com.sxpt.module.workspace.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sxpt.common.api.ApiResult;
import com.sxpt.module.workspace.service.TrainingWorkspaceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Profile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Profile("!test")
public class TrainingWorkspaceController {

    private final TrainingWorkspaceService service;

    public TrainingWorkspaceController(TrainingWorkspaceService service) {
        this.service = service;
    }

    @GetMapping("/training/workspace")
    public ApiResult<JSONObject> getTeacherWorkspace() {
        return ApiResult.success(service.getTeacherWorkspace());
    }

    @PutMapping("/training/workspace")
    public ApiResult<JSONObject> saveTeacherWorkspace(@RequestBody Map<String, Object> state) {
        return ApiResult.success(service.saveTeacherWorkspace(
                JSON.parseObject(JSON.toJSONString(state))
        ));
    }

    @GetMapping("/student/training/workspace")
    public ApiResult<JSONObject> getStudentWorkspace() {
        return ApiResult.success(service.getStudentWorkspace());
    }

    @PutMapping("/student/training/workspace")
    public ApiResult<JSONObject> saveStudentProgress(@RequestBody Map<String, Object> state) {
        return ApiResult.success(service.saveStudentProgress(
                JSON.parseObject(JSON.toJSONString(state))
        ));
    }
}

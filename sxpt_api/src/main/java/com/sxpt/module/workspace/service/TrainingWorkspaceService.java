package com.sxpt.module.workspace.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.common.security.CurrentUserContext;
import com.sxpt.module.workspace.entity.TrainingWorkspaceState;
import com.sxpt.module.workspace.mapper.TrainingWorkspaceStateMapper;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Arrays;

@Service
@Profile("!test")
public class TrainingWorkspaceService {

    private static final String ACTIVE = "ACTIVE";

    private static final List<String> STUDENT_PROGRESS_FIELDS = Arrays.asList(
            "attemptNumber",
            "submissionValues",
            "status",
            "currentStageIndex",
            "completedStageIds",
            "completedPracticeStepIds",
            "practiceStepResults",
            "objectiveScore",
            "remoteScore",
            "startedAt",
            "submittedAt",
            "remoteExecutionId",
            "remoteExecutionStatus",
            "remoteContextLoaded",
            "syncStatus",
            "syncError"
    );

    private final TrainingWorkspaceStateMapper mapper;

    public TrainingWorkspaceService(TrainingWorkspaceStateMapper mapper) {
        this.mapper = mapper;
    }

    public JSONObject getTeacherWorkspace() {
        CurrentUserContext.CurrentUser user = requireScopedUser();
        requireTeacher(user);
        TrainingWorkspaceState workspace = findOwnerWorkspace(user.getTenantId(), user.getUserId());
        return workspace == null ? null : parseWorkspace(workspace);
    }

    @Transactional(rollbackFor = Exception.class)
    public JSONObject saveTeacherWorkspace(JSONObject state) {
        CurrentUserContext.CurrentUser user = requireScopedUser();
        requireTeacher(user);
        if (state == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        state.put("currentRole", user.hasAnyRole("ADMIN") ? "admin" : "teacher");
        TrainingWorkspaceState workspace = findOwnerWorkspace(user.getTenantId(), user.getUserId());
        LocalDateTime now = LocalDateTime.now();
        if (workspace == null) {
            workspace = new TrainingWorkspaceState();
            workspace.setId(UUID.randomUUID().toString().replace("-", ""));
            workspace.setTenantId(user.getTenantId());
            workspace.setOwnerUserId(user.getUserId());
            workspace.setVersionNo(1L);
            workspace.setCreateBy(user.getUserId());
            workspace.setCreateTime(now);
            workspace.setStatus(ACTIVE);
            workspace.setDeleted(Boolean.FALSE);
            workspace.setWorkspaceJson(state.toJSONString());
            workspace.setUpdateBy(user.getUserId());
            workspace.setUpdateTime(now);
            mapper.insert(workspace);
        } else {
            mergeAdvancedStudentProgress(parseWorkspace(workspace), state);
            workspace.setWorkspaceJson(state.toJSONString());
            workspace.setVersionNo((workspace.getVersionNo() == null ? 0L : workspace.getVersionNo()) + 1L);
            workspace.setUpdateBy(user.getUserId());
            workspace.setUpdateTime(now);
            mapper.updateById(workspace);
        }
        return parseWorkspace(workspace);
    }

    private void mergeAdvancedStudentProgress(JSONObject persistedState, JSONObject incomingState) {
        JSONArray persistedTasks = persistedState.getJSONArray("studentTasks");
        JSONArray incomingTasks = incomingState.getJSONArray("studentTasks");
        if (persistedTasks == null || incomingTasks == null) return;
        Map<String, JSONObject> persistedById = new LinkedHashMap<String, JSONObject>();
        for (Object value : persistedTasks) {
            JSONObject task = toObject(value);
            persistedById.put(task.getString("id"), task);
        }
        for (int index = 0; index < incomingTasks.size(); index++) {
            JSONObject incoming = incomingTasks.getJSONObject(index);
            JSONObject persisted = persistedById.get(incoming.getString("id"));
            if (persisted != null && isPersistedProgressNewer(persisted, incoming)) {
                incomingTasks.set(index, persisted);
            }
        }
    }

    private boolean isPersistedProgressNewer(JSONObject persisted, JSONObject incoming) {
        int persistedRank = taskStatusRank(persisted.getString("status"));
        int incomingRank = taskStatusRank(incoming.getString("status"));
        if (persistedRank != incomingRank) {
            return persistedRank > incomingRank;
        }
        if ("GRADED".equalsIgnoreCase(incoming.getString("status"))) {
            return false;
        }
        int persistedStage = persisted.getIntValue("currentStageIndex");
        int incomingStage = incoming.getIntValue("currentStageIndex");
        int persistedCompleted = persisted.getJSONArray("completedStageIds") == null
                ? 0 : persisted.getJSONArray("completedStageIds").size();
        int incomingCompleted = incoming.getJSONArray("completedStageIds") == null
                ? 0 : incoming.getJSONArray("completedStageIds").size();
        return persistedStage > incomingStage || persistedCompleted > incomingCompleted;
    }

    private int taskStatusRank(String status) {
        if ("GRADED".equalsIgnoreCase(status)) return 4;
        if ("SUBMITTED".equalsIgnoreCase(status)) return 3;
        if ("DOING".equalsIgnoreCase(status)) return 2;
        return 1;
    }

    public JSONObject getStudentWorkspace() {
        CurrentUserContext.CurrentUser user = requireScopedUser();
        requireStudent(user);
        return buildStudentWorkspace(user.getTenantId(), user.getUserId());
    }

    @Transactional(rollbackFor = Exception.class)
    public JSONObject saveStudentProgress(JSONObject incomingState) {
        CurrentUserContext.CurrentUser user = requireScopedUser();
        requireStudent(user);
        JSONArray incomingTasks = incomingState == null
                ? null
                : incomingState.getJSONArray("studentTasks");
        if (incomingTasks == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        Map<String, JSONObject> ownUpdates = new LinkedHashMap<String, JSONObject>();
        for (Object value : incomingTasks) {
            JSONObject task = toObject(value);
            if (user.getUserId().equals(task.getString("studentId"))
                    && StringUtils.hasText(task.getString("id"))) {
                ownUpdates.put(task.getString("id"), task);
            }
        }

        List<TrainingWorkspaceState> workspaces = listTenantWorkspaces(user.getTenantId());
        LocalDateTime now = LocalDateTime.now();
        for (TrainingWorkspaceState workspace : workspaces) {
            JSONObject state = parseWorkspace(workspace);
            JSONArray tasks = state.getJSONArray("studentTasks");
            boolean changed = false;
            if (tasks != null) {
                for (int index = 0; index < tasks.size(); index++) {
                    JSONObject existing = tasks.getJSONObject(index);
                    JSONObject update = ownUpdates.get(existing.getString("id"));
                    if (update != null && user.getUserId().equals(existing.getString("studentId"))) {
                        tasks.set(index, mergeStudentProgress(existing, update));
                        changed = true;
                    }
                }
            }
            if (changed) {
                workspace.setWorkspaceJson(state.toJSONString());
                workspace.setVersionNo((workspace.getVersionNo() == null ? 0L : workspace.getVersionNo()) + 1L);
                workspace.setUpdateBy(user.getUserId());
                workspace.setUpdateTime(now);
                mapper.updateById(workspace);
            }
        }
        return buildStudentWorkspace(user.getTenantId(), user.getUserId());
    }

    private JSONObject mergeStudentProgress(JSONObject existing, JSONObject update) {
        JSONObject merged = JSON.parseObject(existing.toJSONString());
        for (String field : STUDENT_PROGRESS_FIELDS) {
            if (update.containsKey(field)) {
                if ("status".equals(field)
                        && !isStudentWritableStatus(update.getString(field))) {
                    continue;
                }
                merged.put(field, update.get(field));
            }
        }
        return merged;
    }

    private boolean isStudentWritableStatus(String status) {
        return "TODO".equalsIgnoreCase(status)
                || "DOING".equalsIgnoreCase(status)
                || "SUBMITTED".equalsIgnoreCase(status);
    }

    private JSONObject buildStudentWorkspace(String tenantId, String studentId) {
        JSONObject result = emptyStudentState();
        Map<String, JSONObject> tasks = new LinkedHashMap<String, JSONObject>();
        Map<String, JSONObject> lessons = new LinkedHashMap<String, JSONObject>();
        Map<String, JSONObject> publishedTasks = new LinkedHashMap<String, JSONObject>();
        Map<String, JSONObject> platforms = new LinkedHashMap<String, JSONObject>();

        for (TrainingWorkspaceState workspace : listTenantWorkspaces(tenantId)) {
            JSONObject source = parseWorkspace(workspace);
            JSONArray sourceTasks = source.getJSONArray("studentTasks");
            if (sourceTasks == null) {
                continue;
            }
            Map<String, Boolean> lessonIds = new LinkedHashMap<String, Boolean>();
            Map<String, Boolean> publishedTaskIds = new LinkedHashMap<String, Boolean>();
            Map<String, Boolean> dataItemIds = new LinkedHashMap<String, Boolean>();
            Map<String, Boolean> ownTaskIds = new LinkedHashMap<String, Boolean>();
            for (Object value : sourceTasks) {
                JSONObject task = toObject(value);
                if (!studentId.equals(task.getString("studentId"))) {
                    continue;
                }
                tasks.put(task.getString("id"), task);
                ownTaskIds.put(task.getString("id"), Boolean.TRUE);
                lessonIds.put(task.getString("lessonId"), Boolean.TRUE);
                publishedTaskIds.put(task.getString("publishedTaskId"), Boolean.TRUE);
                dataItemIds.put(task.getString("dataItemId"), Boolean.TRUE);
            }
            if (lessonIds.isEmpty()) {
                continue;
            }

            JSONArray sourceLessons = source.getJSONArray("lessons");
            if (sourceLessons != null) {
                for (Object value : sourceLessons) {
                    JSONObject lesson = toObject(value);
                    if (lessonIds.containsKey(lesson.getString("id"))) {
                        lessons.put(lesson.getString("id"), lesson);
                    }
                }
            }
            JSONArray sourcePublished = source.getJSONArray("publishedTasks");
            if (sourcePublished != null) {
                for (Object value : sourcePublished) {
                    JSONObject task = toObject(value);
                    if (publishedTaskIds.containsKey(task.getString("id"))) {
                        publishedTasks.put(task.getString("id"), task);
                    }
                }
            }
            copyRelevantMap(source, result, "examSettings", lessonIds, studentId, dataItemIds, ownTaskIds);
            copyRelevantMap(source, result, "groupPlans", lessonIds, studentId, dataItemIds, ownTaskIds);
            copyRelevantMap(source, result, "unitDataPlans", lessonIds, studentId, dataItemIds, ownTaskIds);
            copyRelevantMap(source, result, "dataItems", lessonIds, studentId, dataItemIds, ownTaskIds);
        }

        Map<String, Boolean> platformIds = new LinkedHashMap<String, Boolean>();
        for (JSONObject lesson : lessons.values()) {
            platformIds.put(lesson.getString("businessPlatformId"), Boolean.TRUE);
        }
        for (TrainingWorkspaceState workspace : listTenantWorkspaces(tenantId)) {
            JSONArray sourcePlatforms = parseWorkspace(workspace).getJSONArray("businessPlatforms");
            if (sourcePlatforms == null) continue;
            for (Object value : sourcePlatforms) {
                JSONObject platform = toObject(value);
                if (platformIds.containsKey(platform.getString("id"))) {
                    platforms.put(platform.getString("id"), platform);
                }
            }
        }

        result.put("studentTasks", toArray(tasks));
        result.put("lessons", toArray(lessons));
        result.put("publishedTasks", toArray(publishedTasks));
        result.put("businessPlatforms", toArray(platforms));
        return result;
    }

    private void copyRelevantMap(
            JSONObject source,
            JSONObject target,
            String key,
            Map<String, Boolean> lessonIds,
            String studentId,
            Map<String, Boolean> dataItemIds,
            Map<String, Boolean> ownTaskIds
    ) {
        JSONObject sourceMap = source.getJSONObject(key);
        JSONObject targetMap = target.getJSONObject(key);
        if (sourceMap == null) return;
        for (String lessonId : lessonIds.keySet()) {
            Object value = sourceMap.get(lessonId);
            if (value == null) continue;
            Object copy = JSON.parse(JSON.toJSONString(value));
            if ("groupPlans".equals(key) && copy instanceof JSONObject) {
                JSONObject plan = (JSONObject) copy;
                JSONArray members = plan.getJSONArray("members");
                JSONArray ownMembers = new JSONArray();
                if (members != null) {
                    for (Object memberValue : members) {
                        JSONObject member = toObject(memberValue);
                        if (studentId.equals(member.getString("studentId"))) {
                            ownMembers.add(member);
                        }
                    }
                }
                plan.put("members", ownMembers);
            }
            if ("dataItems".equals(key) && copy instanceof JSONArray) {
                JSONArray ownItems = new JSONArray();
                for (Object itemValue : (JSONArray) copy) {
                    JSONObject item = toObject(itemValue);
                    if (dataItemIds.containsKey(item.getString("id"))) {
                        JSONArray assigned = item.getJSONArray("assignedStudentTaskIds");
                        if (assigned != null) {
                            JSONArray ownAssigned = new JSONArray();
                            for (Object taskId : assigned) {
                                if (taskId != null && ownTaskIds.containsKey(String.valueOf(taskId))) {
                                    ownAssigned.add(taskId);
                                }
                            }
                            item.put("assignedStudentTaskIds", ownAssigned);
                        }
                        ownItems.add(item);
                    }
                }
                copy = ownItems;
            }
            targetMap.put(lessonId, copy);
        }
    }

    private JSONObject emptyStudentState() {
        JSONObject result = new JSONObject(true);
        result.put("currentRole", "student");
        result.put("businessPlatforms", new JSONArray());
        result.put("lessons", new JSONArray());
        result.put("examSettings", new JSONObject(true));
        result.put("groupPlans", new JSONObject(true));
        result.put("unitDataPlans", new JSONObject(true));
        result.put("dataItems", new JSONObject(true));
        result.put("publishedTasks", new JSONArray());
        result.put("studentTasks", new JSONArray());
        result.put("activities", new JSONArray());
        return result;
    }

    private CurrentUserContext.CurrentUser requireScopedUser() {
        CurrentUserContext.CurrentUser user = CurrentUserContext.getRequiredUser();
        if (!StringUtils.hasText(user.getTenantId())) {
            throw new BusinessException(ApiResultCode.UNAUTHORIZED);
        }
        return user;
    }

    private void requireTeacher(CurrentUserContext.CurrentUser user) {
        if (!user.hasAnyRole("ADMIN", "TEACHER")) {
            throw new BusinessException(ApiResultCode.FORBIDDEN);
        }
    }

    private void requireStudent(CurrentUserContext.CurrentUser user) {
        if (!user.hasAnyRole("STUDENT")) {
            throw new BusinessException(ApiResultCode.FORBIDDEN);
        }
    }

    private TrainingWorkspaceState findOwnerWorkspace(String tenantId, String ownerUserId) {
        return mapper.selectOne(new LambdaQueryWrapper<TrainingWorkspaceState>()
                .eq(TrainingWorkspaceState::getTenantId, tenantId)
                .eq(TrainingWorkspaceState::getOwnerUserId, ownerUserId)
                .eq(TrainingWorkspaceState::getStatus, ACTIVE)
                .eq(TrainingWorkspaceState::getDeleted, Boolean.FALSE));
    }

    private List<TrainingWorkspaceState> listTenantWorkspaces(String tenantId) {
        return mapper.selectList(new LambdaQueryWrapper<TrainingWorkspaceState>()
                .eq(TrainingWorkspaceState::getTenantId, tenantId)
                .eq(TrainingWorkspaceState::getStatus, ACTIVE)
                .eq(TrainingWorkspaceState::getDeleted, Boolean.FALSE));
    }

    private JSONObject parseWorkspace(TrainingWorkspaceState workspace) {
        return JSON.parseObject(workspace.getWorkspaceJson());
    }

    private JSONObject toObject(Object value) {
        return value instanceof JSONObject
                ? (JSONObject) value
                : JSON.parseObject(JSON.toJSONString(value));
    }

    private JSONArray toArray(Map<String, JSONObject> values) {
        JSONArray result = new JSONArray();
        result.addAll(values.values());
        return result;
    }
}

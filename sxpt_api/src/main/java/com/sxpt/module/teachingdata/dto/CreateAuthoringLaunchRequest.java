package com.sxpt.module.teachingdata.dto;

import javax.validation.constraints.NotBlank;

/** Request for a fresh teacher authoring business-data launch. */
public class CreateAuthoringLaunchRequest {

    @NotBlank
    private String lessonId;

    @NotBlank
    private String connectorSystemId;

    @NotBlank
    private String businessModuleId;

    /** NORMAL opens the configured module entry; CLASSIC_CASE opens the created case detail. */
    private String generationSource;

    public String getLessonId() {
        return lessonId;
    }

    public void setLessonId(String lessonId) {
        this.lessonId = lessonId;
    }

    public String getConnectorSystemId() {
        return connectorSystemId;
    }

    public void setConnectorSystemId(String connectorSystemId) {
        this.connectorSystemId = connectorSystemId;
    }

    public String getBusinessModuleId() {
        return businessModuleId;
    }

    public void setBusinessModuleId(String businessModuleId) {
        this.businessModuleId = businessModuleId;
    }

    public String getGenerationSource() {
        return generationSource;
    }

    public void setGenerationSource(String generationSource) {
        this.generationSource = generationSource;
    }
}

package com.sxpt.module.teachingdata;

import com.sxpt.module.teachingdata.controller.AuthoringLaunchController;
import com.sxpt.module.teachingdata.service.AuthoringLaunchService;
import com.sxpt.module.teachingdata.vo.AuthoringLaunchVO;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthoringLaunchControllerTests {

    @Test
    void createShouldReturnFreshAuthoringLaunchResult() throws Exception {
        AuthoringLaunchService service = mock(AuthoringLaunchService.class);
        AuthoringLaunchVO result = new AuthoringLaunchVO();
        result.setTenantId("tenant-1");
        result.setLaunchContextId("launch-1");
        result.setDataInstanceId("instance-1");
        result.setRedirectUrl("https://oa.example.com/purchase/apply");
        result.setLaunchUrl("https://oa.example.com/sso?tenantId=tenant-1");
        when(service.createLaunch(any())).thenReturn(result);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthoringLaunchController(service))
                .build();

        mockMvc.perform(post("/api/v1/teaching-data/authoring-launches/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lessonId\":\"lesson-1\",\"connectorSystemId\":\"system-1\",\"businessModuleId\":\"module-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.result.dataInstanceId", is("instance-1")))
                .andExpect(jsonPath("$.result.launchContextId", is("launch-1")));
    }
}

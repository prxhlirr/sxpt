package com.sxpt.module.teachingdata.service;

import com.sxpt.module.teachingdata.dto.CreateAuthoringLaunchRequest;
import com.sxpt.module.teachingdata.vo.AuthoringLaunchVO;

/** Provisions a fresh RECORD instance and creates its teacher capture launch. */
public interface AuthoringLaunchService {

    AuthoringLaunchVO createLaunch(CreateAuthoringLaunchRequest request);
}

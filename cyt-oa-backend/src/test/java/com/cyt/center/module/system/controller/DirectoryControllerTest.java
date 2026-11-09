package com.cyt.center.module.system.controller;

import com.cyt.center.core.security.CurrentUser;
import com.cyt.center.module.system.model.vo.EmployeeDetailVO;
import com.cyt.center.module.system.service.EnterpriseDirectorySyncService;
import com.cyt.center.module.system.service.OrgStructureService;
import com.cyt.center.module.system.service.SysDeptService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DirectoryControllerTest {

    @Test
    void employeeDetailBindsUserIdWithoutCompilerParameterNames() throws Exception {
        var orgStructure = mock(OrgStructureService.class);
        var currentUser = mock(CurrentUser.class);
        var detail = new EmployeeDetailVO();
        detail.setUserId(42L);
        when(orgStructure.detail(42L)).thenReturn(detail);
        var controller = new DirectoryController(mock(EnterpriseDirectorySyncService.class),
                mock(SysDeptService.class), orgStructure, currentUser);
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        // Reproduce IDE builds that omit -parameters, even when Maven retains names.
        mvc.getDispatcherServlet().getWebApplicationContext()
                .getBean(RequestMappingHandlerAdapter.class).setParameterNameDiscoverer(null);

        mvc.perform(get("/admin-api/system/directory/employees/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(42));
        verify(currentUser).requirePermission("system:user:query");
        verify(orgStructure).detail(42L);
    }
}

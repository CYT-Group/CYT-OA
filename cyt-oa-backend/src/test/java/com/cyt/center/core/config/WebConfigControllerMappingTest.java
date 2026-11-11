package com.cyt.center.core.config;

import com.cyt.center.core.exception.AuthException;
import com.cyt.center.core.security.CurrentUser;
import com.cyt.center.module.hrm.controller.admin.employee.WecomHrSyncController;
import com.cyt.center.module.hrm.controller.admin.workspace.HrmWorkspaceController;
import com.cyt.center.module.hrm.service.employee.info.WecomHrSyncService;
import com.cyt.center.module.hrm.service.workspace.HrmWorkspaceService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 使用生产 WebConfig 注册真实 MVC 映射，避免 standaloneSetup 漏掉全局前缀。 */
class WebConfigControllerMappingTest {
    @Configuration(proxyBeanMethods=false)
    @EnableWebMvc
    @Import(WebConfig.class)
    static class MappingConfiguration {
        @Bean CenterProperties properties(){return new CenterProperties();}
        @Bean WecomHrSyncController wecomSync(){return new WecomHrSyncController(mock(WecomHrSyncService.class),mock(CurrentUser.class));}
        @Bean HrmWorkspaceController hrWorkspace(){return new HrmWorkspaceController(mock(HrmWorkspaceService.class));}
    }

    @Test void hrSyncAndWorkspaceHaveExactlyOneAdminPrefixInActualSpringMapping(){
        try(var context=new AnnotationConfigWebApplicationContext()){
            context.setServletContext(new MockServletContext());
            context.register(MappingConfiguration.class);context.refresh();
            var mapping=context.getBean(RequestMappingHandlerMapping.class);
            var allPaths=new HashSet<String>();var syncPaths=new HashSet<String>();
            mapping.getHandlerMethods().forEach((info,handler)->{
                allPaths.addAll(info.getPatternValues());
                if(handler.getBeanType()==WecomHrSyncController.class){
                    syncPaths.addAll(info.getPatternValues());
                    assertEquals(Set.of(RequestMethod.POST),info.getMethodsCondition().getMethods());
                }
            });
            assertEquals(Set.of("/admin-api/hrm/employee/wecom/sync","/admin-api/hrm/employee/wecom/confirm"),syncPaths);
            assertTrue(allPaths.contains("/admin-api/hrm/workspace/departments"));
            assertTrue(allPaths.stream().noneMatch(path->path.startsWith("/admin-api/admin-api/")),allPaths.toString());
        }
    }

    @Test void noAdminPackageControllerDeclaresTheGloballyAddedPrefix() throws Exception {
        var scanner=new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        var duplicated=new ArrayList<String>();
        for(var bean:scanner.findCandidateComponents("com.cyt.center.module")){
            String name=Objects.requireNonNull(bean.getBeanClassName());
            if(!name.contains(".controller.admin"))continue;
            var type=Class.forName(name,false,getClass().getClassLoader());
            var annotation=AnnotatedElementUtils.findMergedAnnotation(type,RequestMapping.class);
            if(annotation==null)continue;
            var paths=new HashSet<>(Arrays.asList(annotation.path()));paths.addAll(Arrays.asList(annotation.value()));
            for(String path:paths)if(path.equals("/admin-api")||path.startsWith("/admin-api/"))duplicated.add(name+" -> "+path);
        }
        assertTrue(duplicated.isEmpty(),"controller.admin 的前缀由 WebConfig 添加，不能重复声明："+duplicated);
    }

    @Test void syncAndConfirmStillRequireBothEmployeePermissions(){
        var service=mock(WecomHrSyncService.class);var current=mock(CurrentUser.class);
        var controller=new WecomHrSyncController(service,current);
        controller.sync();controller.confirm(new WecomHrSyncController.Confirm(42L,"birthday","2026-01-01","2026-01-02"));
        var permissions=inOrder(current);
        permissions.verify(current).requirePermission("hrm:employee:query");permissions.verify(current).requirePermission("hrm:employee:update");
        permissions.verify(current).requirePermission("hrm:employee:query");permissions.verify(current).requirePermission("hrm:employee:update");
        verify(service).sync();verify(service).confirm(42L,"birthday","2026-01-01","2026-01-02");
    }

    @Test void missingEitherPermissionStopsBeforeAnySyncOrConfirmation(){
        for(String permission:List.of("hrm:employee:query","hrm:employee:update")){
            var service=mock(WecomHrSyncService.class);var current=mock(CurrentUser.class);
            doThrow(AuthException.forbidden("无权限")).when(current).requirePermission(permission);
            var controller=new WecomHrSyncController(service,current);
            assertThrows(AuthException.class,controller::sync);
            assertThrows(AuthException.class,()->controller.confirm(new WecomHrSyncController.Confirm(42L,"birthday","2026-01-01","2026-01-02")));
            verifyNoInteractions(service);
        }
    }
}

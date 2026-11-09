package com.cyt.center.core.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局兜底：Controller 不写 try-catch，异常统一转成 Result。
 * 鉴权失败保持 HTTP 200 + code 401/403，前端按 code 决定跳登录还是提示，不必区分传输层错误。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        return Result.fail(e.getMessage());
    }

    /** 移植自 ruoyi 框架的业务异常，带业务错误码 */
    @ExceptionHandler(ServiceException.class)
    public Result<Void> handleService(ServiceException e) {
        return Result.fail(e.getCode() == null ? 500 : e.getCode(), e.getMessage());
    }

    @ExceptionHandler(AuthException.class)
    public Result<Void> handleAuth(AuthException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLogin(NotLoginException e) {
        return Result.fail(AuthException.UNAUTHORIZED, "登录状态已失效，请重新登录");
    }

    @ExceptionHandler({ NotPermissionException.class, NotRoleException.class })
    public Result<Void> handleForbidden(Exception e) {
        return Result.fail(AuthException.FORBIDDEN, "没有该操作权限");
    }

    /** Spring 6 里 MethodArgumentNotValidException 继承自 BindException，一个入口即可覆盖参数绑定校验 */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("请求参数不合法");
        return Result.fail(message);
    }

    /** 缺参、参数解析失败属于请求不合法，不该冒到「服务器内部错误」里。 */
    @ExceptionHandler(ServletRequestBindingException.class)
    public Result<Void> handleBadRequest(ServletRequestBindingException e) {
        log.warn("[handleBadRequest][{}]", e.getMessage());
        return Result.fail("请求参数不合法");
    }

    /** 类型不匹配（id=abc）同样落在上面那句「参数解析失败」里，但它不是 ServletRequestBindingException 的子类 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("[handleTypeMismatch][参数 {} 期望类型 {}]", e.getName(),
                e.getRequiredType() == null ? "未知" : e.getRequiredType().getSimpleName());
        return Result.fail("参数 " + e.getName() + " 取值不合法");
    }

    /** 路径写错会走到静态资源解析，以前冒成「服务器内部错误」+ ERROR 堆栈，探测脚本一跑满屏红 */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResource(NoResourceFoundException e) {
        log.warn("[handleNoResource][{} {}]", e.getHttpMethod(), e.getResourcePath());
        return Result.fail(404, "接口不存在");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnexpected(Exception e) {
        log.error("未预期的异常", e);
        return Result.fail("服务器内部错误");
    }

}

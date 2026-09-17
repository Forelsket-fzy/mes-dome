package com.fzy.mes.common.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * D33：派工 / 报工操作审计日志（SLF4J）。
 */
@Slf4j
@Aspect
@Component
public class OperationAuditAspect {

	@Pointcut("execution(* com.fzy.mes.module.dispatch.controller.DispatchController.dispatch(..))")
	public void dispatchPointcut() {
	}

	@Pointcut("execution(* com.fzy.mes.module.report.controller.ReportController.submit(..))")
	public void reportPointcut() {
	}

	@Around("dispatchPointcut() || reportPointcut()")
	public Object around(ProceedingJoinPoint pjp) throws Throwable {
		long start = System.currentTimeMillis();
		String operator = resolveOperator();
		String method = pjp.getSignature().toShortString();
		try {
			Object result = pjp.proceed();
			log.info("[AUDIT] ok operator={} method={} args={} costMs={}",
					operator, method, Arrays.toString(pjp.getArgs()), System.currentTimeMillis() - start);
			return result;
		} catch (Throwable ex) {
			log.warn("[AUDIT] fail operator={} method={} args={} costMs={} err={}",
					operator, method, Arrays.toString(pjp.getArgs()),
					System.currentTimeMillis() - start, ex.getMessage());
			throw ex;
		}
	}

	private String resolveOperator() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated()) {
			return "anonymous";
		}
		return String.valueOf(auth.getPrincipal());
	}

}

package com.fzy.mes.module.auth.service;

import com.fzy.mes.module.auth.dto.AuthSession;

/**
 * 用户会话加载（Cache-Aside），与 Spring Security 认证链解耦，避免循环依赖。
 */
public interface AuthSessionService {

    AuthSession loadFromDb(String username);

    AuthSession getByUsername(String username);

}

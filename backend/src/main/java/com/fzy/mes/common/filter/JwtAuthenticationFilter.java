package com.fzy.mes.common.filter;

import com.fzy.mes.common.utils.JwtAccessClaims;
import com.fzy.mes.common.utils.JwtUtil;
import com.fzy.mes.module.auth.config.AccessTokenBlacklist;
import com.fzy.mes.module.auth.dto.AuthSession;
import com.fzy.mes.module.auth.service.AuthSessionService;
import com.fzy.mes.module.auth.vo.LoginUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthSessionService authSessionService;
    private final JwtUtil jwtUtil;
    private final AccessTokenBlacklist accessTokenBlacklist;

    public JwtAuthenticationFilter(AuthSessionService authSessionService, JwtUtil jwtUtil,
                                   AccessTokenBlacklist accessTokenBlacklist) {
        this.authSessionService = authSessionService;
        this.jwtUtil = jwtUtil;
        this.accessTokenBlacklist = accessTokenBlacklist;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (isPermitAllPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = JwtUtil.resolveBearerToken(request.getHeader("Authorization"));
        if (token == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "token不能为空");
            return;
        }

        Optional<JwtAccessClaims> claimsOpt = jwtUtil.verifyAccessToken(token);
        if (claimsOpt.isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "token无效或已过期");
            return;
        }

        JwtAccessClaims claims = claimsOpt.get();
        if (accessTokenBlacklist.isBlacklisted(claims.jti())) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "token已失效");
            return;
        }

        AuthSession authSession = authSessionService.getByUsername(claims.username());
        if (authSession == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "用户不存在");
            return;
        }

        String role = authSession.getRole();
        List<GrantedAuthority> authorityList = (role == null || role.isBlank())
                ? Collections.emptyList()
                : List.of(new SimpleGrantedAuthority(role));

        UserDetails userDetails = new LoginUser(
                authSession.getId(),
                claims.username(),
                "",
                authSession.getEnabled(),
                authorityList);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, token, authorityList);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private boolean isPermitAllPath(String path) {
        return "/api/auth/login".equals(path)
                || "/api/auth/register".equals(path)
                || "/api/auth/refresh".equals(path)
                || "/api/auth/logout".equals(path);
    }

}

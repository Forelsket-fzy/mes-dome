package com.fzy.mes.common.filter;

import com.fzy.mes.common.utils.JwtUtil;
import com.fzy.mes.module.auth.dto.AuthSession;
import com.fzy.mes.module.auth.service.AuthService;
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
import java.util.Map;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthService authService;

    public JwtAuthenticationFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String usr = request.getRequestURI();
        if ("/api/auth/login".equals(usr) || "/api/auth/register".equals(usr)) {
            filterChain.doFilter(request, response);
        }else {
            //从header中读取token
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring("Bearer ".length());
            }

            //验证token
            if (token == null || token.isEmpty()) {
               response.sendError(HttpServletResponse.SC_UNAUTHORIZED,"token不能为空");
               return;
            }

            if (!JwtUtil.verifyToken(token)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "token已经失效");
                return;
            }

            //从token中获取信息
            Map<String, Object> claims = JwtUtil.parseClaims(token);

            String username = claims.get("username").toString();

            AuthSession authSession = authService.getByUsername(username);
            if (authSession == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "用户不存在");
                return;
            }

            Long id = authSession.getId();
            Boolean enabled = authSession.getEnabled();
            String role = authSession.getRole();
            List<GrantedAuthority> authorityList = (role == null || role.isBlank())
                    ? Collections.emptyList()
                    : List.of(new SimpleGrantedAuthority(role));

            UserDetails userDetails = new LoginUser(id, username, "", enabled, authorityList);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, token, authorityList);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        }

    }

}

package com.fzy.mes.common.filter;

import com.fzy.mes.common.utils.JwtUtil;
import com.fzy.mes.module.auth.vo.LoginUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JwtAuthenticationFilter extends OncePerRequestFilter {


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        //判断路径是否为登录 如果是 放行
        String usr = request.getRequestURI();
        if ("/api/auth/login".equals(usr) || "/api/auth/register".equals(usr)) {
            logger.info("当前用户访问路径为"+ usr );
        }else{
            //从header中读取token
            String token = request.getHeader("Authorization");

            //验证token
            if(token == null || token.isEmpty()) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED,"token不能为空");
                return;
            }

            if (!JwtUtil.verifyToken(token)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED,"token已经失效");
                return;
            }

            //从token中获取信息
            Map<String,Object> claims = JwtUtil.parseClaims(token);

            String username = claims.get("username").toString();


            //从缓存中获取权限
            List<GrantedAuthority> authorityList = Collections.emptyList();

            //从缓存中获取信息
            Long id = 1L;
            Boolean enabled = true;

            //构建UserDetails对象 （不需要密码）
            UserDetails userDetails = new LoginUser(id,username,"",enabled,authorityList);

            //构建已认证的Authentication对象username
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails,token,authorityList);

            //存入SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

        }

        filterChain.doFilter(request, response);

    }


}

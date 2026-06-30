package com.fzy.mes.common.config;

import com.fzy.mes.common.filter.JwtAuthenticationFilter;
import com.fzy.mes.module.auth.service.AuthService;
import com.fzy.mes.module.cache.service.CacheService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * D5 实现 JWT 过滤器后，在此链上接入鉴权。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	//加密  security默认BCrypt加密
	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	//jwt过滤器
	@Bean
	public JwtAuthenticationFilter jwtAuthenticationFilter(AuthService authService) {
		return new JwtAuthenticationFilter(authService);
	}

	//认证管理器
	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
		return config.getAuthenticationManager();
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http,AuthService authService) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health", "/actuator/info").permitAll()
						.requestMatchers("/api/auth/login","/api/auth/register").permitAll()
						.anyRequest().authenticated());


		http.addFilterBefore(jwtAuthenticationFilter(authService), UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

}

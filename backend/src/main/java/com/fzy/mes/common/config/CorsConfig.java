package com.fzy.mes.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // ① 允许所有请求路径
                .allowedOriginPatterns("*") // ② 允许所有域名访问（生产环境请指定具体域名）
                .allowedMethods("GET", "POST", "PUT", "DELETE") // ③ 允许的请求方法
                .allowedHeaders("*") // ④ 允许的请求头
                .allowCredentials(true) // ⑤ 是否允许发送Cookie
                .maxAge(3600); // ⑥ 预检请求的缓存时间（秒）
    }
}
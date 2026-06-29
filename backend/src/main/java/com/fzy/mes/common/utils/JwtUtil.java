package com.fzy.mes.common.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.auth0.jwt.interfaces.Verification;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtil {


    private static final String secret = "fangzhengyuan";

    /*
     * 生成Token
     * */
    public static String generateToken(String username) {
        Map<String, Object> header=new HashMap<>();
        header.put("alg","HS256");
        header.put("typ","JWT");

        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("username", username);

        return JWT.create()
                //设置头数据
                .withHeader(header)
                //设置jwt的payload数据
                .withClaim("claims", claimsMap)
                .withIssuer("fangzhengyuan")//设置签发者
                .withSubject("kindergarten")//设置主题
                .withExpiresAt(new Date(System.currentTimeMillis() + 1000 * 60 * 5))//设置过期时间
                .withIssuedAt(new Date())//设置签发时间
                .sign(Algorithm.HMAC256(secret));
    }

    /*
     * 解析Token
     * */
    public static Map<String, Object> parseClaims(String token) {
        return JWT.require(Algorithm.HMAC256(secret))
                .build()
                .verify(token)
                .getClaim("claims")
                .asMap();
    }

    /*
     * 验证Token
     * */
    public static boolean verifyToken(String token) {
        try {
            // String
            Verification verification = JWT.require(Algorithm.HMAC256(secret));
            JWTVerifier build = verification.build();
            //如果解析没有抛出异常就代表成功
            build.verify(token);
            return true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

}


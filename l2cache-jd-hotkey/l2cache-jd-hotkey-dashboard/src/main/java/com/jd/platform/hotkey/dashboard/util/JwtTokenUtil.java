package com.jd.platform.hotkey.dashboard.util;

import io.jsonwebtoken.*;
import org.apache.logging.log4j.util.Base64Util;
import org.apache.tomcat.util.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;
import java.security.Key;
import java.util.Date;


public class JwtTokenUtil {

    private static Logger log = LoggerFactory.getLogger(JwtTokenUtil.class);

    public static final Integer EXPIRATION_TIME = 3600*1000*24*7;
    public static final String SECRET = "happy";
    public static final String TOKEN_PREFIX = "hk";
    public static final String AUTH_HEADER_KEY = "Authorization";

    /**
     * 解析jwt
     * @param jsonWebToken
     * @return
     */
    public static Claims parseJWT(String jsonWebToken) {
        try {
            return Jwts.parser().setSigningKey(DatatypeConverter.parseBase64Binary(SECRET)).parseClaimsJws(jsonWebToken).getBody();
        } catch (ExpiredJwtException  eje) {
            log.error("===== Token过期 =====", eje);
            throw new RuntimeException();
        } catch (Exception e){
            log.error("===== token解析异常 =====", e);
            throw new RuntimeException();
        }
    }

    /**
     * 构建jwt
     * @param userId
     * @param username
     * @param role
     * @return
     */
    public static String createJWT(Integer userId, String username, String role, String appName, String nickName) {
        try {
            // 使用HS256加密算法
            SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
            long nowMillis = System.currentTimeMillis();
            //生成签名密钥
            byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(SECRET);
            Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());

            //userId是重要信息，进行加密下
            String encryId = Base64Util.encode(userId.toString());
            //添加构成JWT的参数
            JwtBuilder builder = Jwts.builder().setHeaderParam("type", "JWT")
                    .claim("userId", encryId)
                    .claim("role", role)
                    .claim("nickName", nickName)
                    .setSubject(username)           // 代表这个JWT的主体，即它的所有人
                 //   .setIssuer(audience.getClientId())              // 代表这个JWT的签发主体；
                    .setIssuedAt(new Date())        // 是一个时间戳，代表这个JWT的签发时间；
                //    .setAudience(audience.getName())          // 代表这个JWT的接收对象；
                    .signWith(signatureAlgorithm, signingKey);
            //添加Token过期时间
            int TTLMillis = EXPIRATION_TIME;
            if (TTLMillis >= 0) {
                long expMillis = nowMillis + TTLMillis;
                Date exp = new Date(expMillis);
                builder.setExpiration(exp); // 是一个时间戳，代表这个JWT的过期时间；
                      //  .setNotBefore(now); // 是一个时间戳，代表这个JWT生效的开始时间，意味着在这个时间之前验证JWT是会失败的
            }

            //生成JWT
            return builder.compact();
        } catch (Exception e) {
            log.error("签名失败", e);
            throw new RuntimeException();
        }
    }



    /**
     * 从token中获取role
     * @param token
     * @return
     */
    public static String getRole(String token){
        return parseJWT(token).get("role", String.class);
    }



    /**
     * 从token中获取用户名
     * @param token
     * @return
     */
    public static String getUsername(String token){
        return parseJWT(token).getSubject();
    }

    /**
     * 从token中获取用户ID
     * @param token
     * @return
     */
    public static String getUserId(String token){
        String userId = parseJWT(token).get("userId", String.class);
        return new String(Base64.decodeBase64(userId));
    }


    public static Claims claims(String token){
        return parseJWT(token);
    }

    /**
     * 是否已过期
     * @param token
     * @return
     */
    public static boolean isExpiration(String token) {
        return parseJWT(token).getExpiration().before(new Date());
    }

    public static String getAuthHeader(HttpServletRequest request){
        Cookie[] cookies = request.getCookies();
        for(Cookie cookie : cookies){
            if("token".equals(cookie.getName())){
                return cookie.getValue();
            }
        }
        return null;
    }

}

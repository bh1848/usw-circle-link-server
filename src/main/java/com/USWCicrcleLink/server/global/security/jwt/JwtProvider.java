package com.USWCicrcleLink.server.global.security.jwt;

import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.errortype.TokenException;
import com.USWCicrcleLink.server.global.exception.errortype.UserException;
import com.USWCicrcleLink.server.global.security.details.CustomLeaderDetails;
import com.USWCicrcleLink.server.global.security.details.CustomUserDetails;
import com.USWCicrcleLink.server.global.security.details.service.UserDetailsServiceManager;
import com.USWCicrcleLink.server.global.security.jwt.domain.TokenValidationResult;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import java.nio.charset.StandardCharsets;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtProvider {
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    private static final String CLUB_UUID_CLAIM = "clubUUID";
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String REFRESH_TOKEN_KEY_PREFIX = "refreshToken:";
    private static final String REFRESH_TOKEN_COOKIE_TEMPLATE = REFRESH_TOKEN_COOKIE_NAME + "=%s; Path=/; HttpOnly; Max-Age=%d; SameSite=Strict; Secure";
    private static final String REFRESH_TOKEN_COOKIE_DELETE_VALUE = REFRESH_TOKEN_COOKIE_NAME + "=; Path=/; HttpOnly; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; SameSite=Strict";
    private static final long ACCESS_TOKEN_EXPIRATION_TIME = 1_800_000L;
    private static final long REFRESH_TOKEN_EXPIRATION_TIME = 604_800_000L;

    private final UserDetailsServiceManager userDetailsServiceManager;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.secret.key}")
    private String secretKeyString;

    private Key secretKey;

    @PostConstruct
    protected void init() {
        this.secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(UUID uuid, HttpServletResponse response) {
        UserDetails userDetails = userDetailsServiceManager.loadUserByUuid(uuid);
        Claims claims = Jwts.claims().setSubject(uuid.toString());

        UUID clubUUID = extractClubUuid(userDetails);
        if (clubUUID != null) {
            claims.put(CLUB_UUID_CLAIM, clubUUID.toString());
        }

        Date now = new Date();
        String accessToken = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + ACCESS_TOKEN_EXPIRATION_TIME))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        response.setHeader(AUTHORIZATION_HEADER, BEARER_PREFIX + accessToken);
        return accessToken;
    }

    public UserDetails getUserDetails(String accessToken) {
        UUID uuid = getUUIDFromAccessToken(accessToken);
        return userDetailsServiceManager.loadUserByUuid(uuid);
    }

    public TokenValidationResult validateAccessToken(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            return TokenValidationResult.INVALID;
        }

        try {
            Claims claims = getClaims(accessToken);
            if (claims == null) {
                return TokenValidationResult.INVALID;
            }
            return claims.getExpiration().before(new Date()) ? TokenValidationResult.EXPIRED : TokenValidationResult.VALID;
        } catch (ExpiredJwtException exception) {
            return TokenValidationResult.EXPIRED;
        } catch (MalformedJwtException | SignatureException | IllegalArgumentException exception) {
            return TokenValidationResult.INVALID;
        }
    }

    public String resolveAccessToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    public Authentication getAuthentication(String accessToken) {
        UserDetails userDetails = getUserDetails(accessToken);
        List<SimpleGrantedAuthority> authorities = userDetails.getAuthorities().stream()
                .map(authority -> new SimpleGrantedAuthority(authority.getAuthority()))
                .toList();
        return new UsernamePasswordAuthenticationToken(userDetails, "", authorities);
    }

    public String createRefreshToken(UUID uuid, HttpServletResponse response) {
        deleteRefreshToken(uuid);

        String newRefreshToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(getRefreshTokenKey(newRefreshToken), uuid.toString(), REFRESH_TOKEN_EXPIRATION_TIME, TimeUnit.MILLISECONDS);

        setRefreshTokenCookie(response, newRefreshToken);
        log.debug("새로운 Refresh Token 발급 - UUID: {}", uuid);
        return newRefreshToken;
    }

    public void validateRefreshToken(String refreshToken, HttpServletRequest request) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new TokenException(ExceptionType.INVALID_TOKEN);
        }

        boolean existsInRedis = redisTemplate.opsForValue().get(getRefreshTokenKey(refreshToken)) != null;
        if (!existsInRedis) {
            log.warn("Refresh Token 검증 실패 - IP: {} | 요청 경로: {}", request.getRemoteAddr(), request.getRequestURI());
            throw new TokenException(ExceptionType.INVALID_TOKEN);
        }

        log.debug("Refresh Token 검증 성공");
    }

    public UUID getUUIDFromRefreshToken(String refreshToken) {
        String storedUuid = redisTemplate.opsForValue().get(getRefreshTokenKey(refreshToken));
        if (storedUuid == null) {
            throw new UserException(ExceptionType.INVALID_TOKEN);
        }
        return UUID.fromString(storedUuid);
    }

    public void deleteRefreshToken(UUID uuid) {
        log.debug("리프레시 토큰 삭제 진행 - UUID: {}", uuid);

        Set<String> keys = redisTemplate.keys(REFRESH_TOKEN_KEY_PREFIX + '*');
        if (keys == null) {
            return;
        }

        for (String key : keys) {
            String storedUuid = redisTemplate.opsForValue().get(key);
            if (uuid.toString().equals(storedUuid)) {
                redisTemplate.delete(key);
                log.debug("기존 Refresh Token 삭제 완료 - Key: {}", key);
                break;
            }
        }
    }

    public String resolveRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        response.setHeader("Set-Cookie", REFRESH_TOKEN_COOKIE_DELETE_VALUE);
        log.debug("클라이언트 쿠키에서 리프레시 토큰 삭제 완료");
    }

    private UUID getUUIDFromAccessToken(String accessToken) {
        return UUID.fromString(getClaims(accessToken).getSubject());
    }

    private Claims getClaims(String jwtToken) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(jwtToken)
                .getBody();
    }

    private UUID extractClubUuid(UserDetails userDetails) {
        if (userDetails instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getClubUUIDs().isEmpty() ? null : customUserDetails.getClubUUIDs().get(0);
        }
        if (userDetails instanceof CustomLeaderDetails customLeaderDetails) {
            return customLeaderDetails.getClubUUID();
        }
        return null;
    }

    private String getRefreshTokenKey(String refreshToken) {
        return REFRESH_TOKEN_KEY_PREFIX + refreshToken;
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        int maxAge = (int) (REFRESH_TOKEN_EXPIRATION_TIME / 1000);
        response.setHeader("Set-Cookie", String.format(REFRESH_TOKEN_COOKIE_TEMPLATE, refreshToken, maxAge));
    }
}


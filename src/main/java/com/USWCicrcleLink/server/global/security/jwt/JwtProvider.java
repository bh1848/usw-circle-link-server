package com.USWCicrcleLink.server.global.security.jwt;

import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.errortype.TokenException;
import com.USWCicrcleLink.server.global.security.details.CustomLeaderDetails;
import com.USWCicrcleLink.server.global.security.details.CustomUserDetails;
import com.USWCicrcleLink.server.global.security.details.service.UserDetailsServiceManager;
import com.USWCicrcleLink.server.global.security.jwt.domain.Role;
import com.USWCicrcleLink.server.global.security.jwt.domain.TokenValidationResult;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtProvider {
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    private static final String CLUB_UUID_CLAIM = "clubUUID";
    private static final String ROLE_CLAIM = "role";
    private static final long ACCESS_TOKEN_EXPIRATION_TIME = 1_800_000L;

    private final UserDetailsServiceManager userDetailsServiceManager;

    @Value("${jwt.secret.key}")
    private String secretKeyString;

    private Key secretKey;

    @PostConstruct
    protected void init() {
        this.secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(UUID uuid, Role role, HttpServletResponse response) {
        UserDetails userDetails = userDetailsServiceManager.loadUserByUuidAndRole(uuid, role);
        Claims claims = Jwts.claims().setSubject(uuid.toString());
        claims.put(ROLE_CLAIM, role.name());

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
        UUID uuid = getUUIDFromAccessToken(accessToken);
        Role role = getRoleFromAccessToken(accessToken);
        UserDetails userDetails = userDetailsServiceManager.loadUserByUuidAndRole(uuid, role);
        List<SimpleGrantedAuthority> authorities = userDetails.getAuthorities().stream()
                .map(authority -> new SimpleGrantedAuthority(authority.getAuthority()))
                .toList();
        return new UsernamePasswordAuthenticationToken(userDetails, "", authorities);
    }

    private UUID getUUIDFromAccessToken(String accessToken) {
        return UUID.fromString(getClaims(accessToken).getSubject());
    }

    public Role getRoleFromAccessToken(String accessToken) {
        String role = getClaims(accessToken).get(ROLE_CLAIM, String.class);
        if (!StringUtils.hasText(role)) {
            throw new TokenException(ExceptionType.INVALID_TOKEN);
        }

        try {
            return Role.valueOf(role);
        } catch (IllegalArgumentException exception) {
            throw new TokenException(ExceptionType.INVALID_TOKEN);
        }
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
}

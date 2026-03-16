package com.USWCicrcleLink.server.global.security.jwt.refresh.store;

import com.USWCicrcleLink.server.global.security.jwt.domain.Role;
import com.USWCicrcleLink.server.global.security.jwt.refresh.domain.RefreshTokenSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RefreshTokenStore {
    private static final String REFRESH_TOKEN_TOKEN_KEY_PREFIX = "refreshToken:token:";
    private static final String REFRESH_TOKEN_USER_KEY_PREFIX = "refreshToken:user:";
    private static final String VALUE_DELIMITER = "|";

    private final RedisTemplate<String, String> redisTemplate;

    public void save(RefreshTokenSession session) {
        redisTemplate.opsForValue().set(
                getRefreshTokenTokenKey(session.refreshToken()),
                buildStoredValue(session.uuid(), session.role()),
                session.expirationTime(),
                TimeUnit.MILLISECONDS
        );
        redisTemplate.opsForValue().set(
                getRefreshTokenUserKey(session.uuid()),
                session.refreshToken(),
                session.expirationTime(),
                TimeUnit.MILLISECONDS
        );
    }

    public Optional<RefreshTokenSession> findByRefreshToken(String refreshToken) {
        String storedValue = redisTemplate.opsForValue().get(getRefreshTokenTokenKey(refreshToken));
        if (!StringUtils.hasText(storedValue)) {
            return Optional.empty();
        }

        String[] tokenParts = storedValue.split("\\|", 2);
        if (tokenParts.length != 2) {
            return Optional.empty();
        }

        try {
            Long expirationTime = redisTemplate.getExpire(getRefreshTokenTokenKey(refreshToken), TimeUnit.MILLISECONDS);
            if (expirationTime == null || expirationTime <= 0) {
                return Optional.empty();
            }

            return Optional.of(new RefreshTokenSession(
                    refreshToken,
                    UUID.fromString(tokenParts[0]),
                    Role.valueOf(tokenParts[1]),
                    expirationTime
            ));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public Optional<String> findRefreshTokenByUser(UUID uuid) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(getRefreshTokenUserKey(uuid)));
    }

    public void deleteByUser(UUID uuid) {
        String userKey = getRefreshTokenUserKey(uuid);
        String refreshToken = redisTemplate.opsForValue().get(userKey);
        if (!StringUtils.hasText(refreshToken)) {
            return;
        }

        redisTemplate.delete(userKey);
        redisTemplate.delete(getRefreshTokenTokenKey(refreshToken));
    }

    private String buildStoredValue(UUID uuid, Role role) {
        return uuid + VALUE_DELIMITER + role.name();
    }

    private String getRefreshTokenTokenKey(String refreshToken) {
        return REFRESH_TOKEN_TOKEN_KEY_PREFIX + refreshToken;
    }

    private String getRefreshTokenUserKey(UUID uuid) {
        return REFRESH_TOKEN_USER_KEY_PREFIX + uuid;
    }
}

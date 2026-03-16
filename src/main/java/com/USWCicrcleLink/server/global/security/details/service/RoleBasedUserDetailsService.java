package com.USWCicrcleLink.server.global.security.details.service;

import com.USWCicrcleLink.server.global.security.jwt.domain.Role;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

public interface RoleBasedUserDetailsService {
    Role getSupportedRole();
    UserDetails loadUserByUuid(UUID uuid);
}

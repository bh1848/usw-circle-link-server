package com.USWCicrcleLink.server.global.security.details.service;

import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.errortype.UserException;
import com.USWCicrcleLink.server.global.security.jwt.domain.Role;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UserDetailsServiceManager {

    private final Map<Role, RoleBasedUserDetailsService> userDetailsServices;

    public UserDetailsServiceManager(List<RoleBasedUserDetailsService> userDetailsServices) {
        this.userDetailsServices = new EnumMap<>(Role.class);
        userDetailsServices.forEach(service -> this.userDetailsServices.put(service.getSupportedRole(), service));
    }

    public UserDetails loadUserByUuidAndRole(UUID uuid, Role role) {
        RoleBasedUserDetailsService service = userDetailsServices.get(role);
        if (service == null) {
            throw new UserException(ExceptionType.USER_NOT_EXISTS);
        }

        return service.loadUserByUuid(uuid);
    }
}

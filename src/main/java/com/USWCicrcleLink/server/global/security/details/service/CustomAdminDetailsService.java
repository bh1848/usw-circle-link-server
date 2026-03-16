package com.USWCicrcleLink.server.global.security.details.service;

import com.USWCicrcleLink.server.admin.admin.repository.AdminRepository;
import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.errortype.UserException;
import com.USWCicrcleLink.server.global.security.details.CustomAdminDetails;
import com.USWCicrcleLink.server.global.security.jwt.domain.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomAdminDetailsService implements RoleBasedUserDetailsService {

    private final AdminRepository adminRepository;

    @Override
    public Role getSupportedRole() {
        return Role.ADMIN;
    }

    @Override
    public UserDetails loadUserByUuid(UUID uuid) {
        return adminRepository.findByAdminUUID(uuid)
                .map(CustomAdminDetails::new)
                .orElseThrow(() -> new UserException(ExceptionType.USER_NOT_EXISTS));
    }
}

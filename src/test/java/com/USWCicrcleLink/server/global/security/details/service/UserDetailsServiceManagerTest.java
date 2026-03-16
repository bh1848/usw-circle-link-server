package com.USWCicrcleLink.server.global.security.details.service;

import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.errortype.UserException;
import com.USWCicrcleLink.server.global.security.jwt.domain.Role;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceManagerTest {

    @Mock
    private RoleBasedUserDetailsService adminUserDetailsService;

    @Mock
    private RoleBasedUserDetailsService userUserDetailsService;

    @Mock
    private RoleBasedUserDetailsService leaderUserDetailsService;

    @Nested
    class loadUserByUuidAndRole_테스트 {

        @Test
        void Role_ADMIN이면_Admin용_UserDetailsService를_호출한다() {
            UUID uuid = UUID.randomUUID();
            UserDetails expectedUserDetails = mock(UserDetails.class);

            given(adminUserDetailsService.getSupportedRole()).willReturn(Role.ADMIN);
            given(userUserDetailsService.getSupportedRole()).willReturn(Role.USER);
            given(leaderUserDetailsService.getSupportedRole()).willReturn(Role.LEADER);
            given(adminUserDetailsService.loadUserByUuid(uuid)).willReturn(expectedUserDetails);

            UserDetailsServiceManager manager = new UserDetailsServiceManager(
                    List.of(adminUserDetailsService, userUserDetailsService, leaderUserDetailsService)
            );

            UserDetails actualUserDetails = manager.loadUserByUuidAndRole(uuid, Role.ADMIN);

            assertThat(actualUserDetails).isSameAs(expectedUserDetails);
            then(adminUserDetailsService).should().loadUserByUuid(uuid);
        }

        @Test
        void Role_USER이면_User용_UserDetailsService를_호출한다() {
            UUID uuid = UUID.randomUUID();
            UserDetails expectedUserDetails = mock(UserDetails.class);

            given(adminUserDetailsService.getSupportedRole()).willReturn(Role.ADMIN);
            given(userUserDetailsService.getSupportedRole()).willReturn(Role.USER);
            given(leaderUserDetailsService.getSupportedRole()).willReturn(Role.LEADER);
            given(userUserDetailsService.loadUserByUuid(uuid)).willReturn(expectedUserDetails);

            UserDetailsServiceManager manager = new UserDetailsServiceManager(
                    List.of(adminUserDetailsService, userUserDetailsService, leaderUserDetailsService)
            );

            UserDetails actualUserDetails = manager.loadUserByUuidAndRole(uuid, Role.USER);

            assertThat(actualUserDetails).isSameAs(expectedUserDetails);
            then(userUserDetailsService).should().loadUserByUuid(uuid);
        }

        @Test
        void Role_LEADER이면_Leader용_UserDetailsService를_호출한다() {
            UUID uuid = UUID.randomUUID();
            UserDetails expectedUserDetails = mock(UserDetails.class);

            given(adminUserDetailsService.getSupportedRole()).willReturn(Role.ADMIN);
            given(userUserDetailsService.getSupportedRole()).willReturn(Role.USER);
            given(leaderUserDetailsService.getSupportedRole()).willReturn(Role.LEADER);
            given(leaderUserDetailsService.loadUserByUuid(uuid)).willReturn(expectedUserDetails);

            UserDetailsServiceManager manager = new UserDetailsServiceManager(
                    List.of(adminUserDetailsService, userUserDetailsService, leaderUserDetailsService)
            );

            UserDetails actualUserDetails = manager.loadUserByUuidAndRole(uuid, Role.LEADER);

            assertThat(actualUserDetails).isSameAs(expectedUserDetails);
            then(leaderUserDetailsService).should().loadUserByUuid(uuid);
        }

        @Test
        void 지원하지_않는_Role이면_UserException이_발생한다() {
            UUID uuid = UUID.randomUUID();

            given(userUserDetailsService.getSupportedRole()).willReturn(Role.USER);
            given(leaderUserDetailsService.getSupportedRole()).willReturn(Role.LEADER);

            UserDetailsServiceManager manager = new UserDetailsServiceManager(
                    List.of(userUserDetailsService, leaderUserDetailsService)
            );

            assertThatThrownBy(() -> manager.loadUserByUuidAndRole(uuid, Role.ADMIN))
                    .isInstanceOf(UserException.class)
                    .extracting(exception -> ((UserException) exception).getExceptionType())
                    .isEqualTo(ExceptionType.USER_NOT_EXISTS);
        }
    }
}

package com.chida.sampriti.protal.backend_ws.listener;

import com.chida.sampriti.protal.backend_ws.dto.UserDto;
import com.chida.sampriti.protal.backend_ws.security.UserPrincipal;
import com.chida.sampriti.protal.backend_ws.service.LoginAttemptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationSuccessListener {
    private LoginAttemptService loginAttemptService;

    @Autowired
    public AuthenticationSuccessListener(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        if(principal instanceof UserPrincipal) {
            UserPrincipal user = (UserPrincipal) event.getAuthentication().getPrincipal();
            loginAttemptService.evictUserFromLoginAttemptCache(user.getUsername());

        }
    }
}

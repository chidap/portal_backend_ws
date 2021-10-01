package com.chida.sampriti.protal.backend_ws.enumeration;

import com.chida.sampriti.protal.backend_ws.constant.Authority;
import com.chida.sampriti.protal.backend_ws.constant.Authority.*;

import static com.chida.sampriti.protal.backend_ws.constant.Authority.*;


public enum Role {
    ROLE_USER(USER_AUTHORITIES),
    ROLE_MEMBER(MEMBER_AUTHORITIES),
    ROLE_EC(EC_AUTHORITIES),
    ROLE_ADMIN(ADMIN_AUTHORITIES);

    private String[] authorities;

    Role(String... authorities) {
        this.authorities = authorities;
    }

    public String[] getAuthorities() {
        return authorities;
    }
}

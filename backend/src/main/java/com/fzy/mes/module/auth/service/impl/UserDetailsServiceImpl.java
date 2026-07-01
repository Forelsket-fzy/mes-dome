package com.fzy.mes.module.auth.service.impl;

import com.fzy.mes.module.auth.dto.AuthSession;
import com.fzy.mes.module.auth.service.AuthSessionService;
import com.fzy.mes.module.auth.vo.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private AuthSessionService authSessionService;

    @Override
    public LoginUser loadUserByUsername(String username) throws UsernameNotFoundException {
        AuthSession user = authSessionService.getByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }

        String role = user.getRole();
        List<GrantedAuthority> authorityList = (role == null || role.isBlank())
                ? Collections.emptyList()
                : List.of(new SimpleGrantedAuthority(role));

        return new LoginUser(user.getId(), username, user.getPassword(), user.getEnabled(), authorityList);
    }

}

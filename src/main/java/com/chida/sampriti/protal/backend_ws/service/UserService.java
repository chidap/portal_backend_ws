package com.chida.sampriti.protal.backend_ws.service;

import com.chida.sampriti.protal.backend_ws.dto.UserDto;
import com.chida.sampriti.protal.backend_ws.exception.domain.EmailExistException;
import com.chida.sampriti.protal.backend_ws.exception.domain.UsernameExistException;

import java.util.List;

public interface UserService {
    UserDto registerUser(UserDto userDetails) throws EmailExistException, UsernameExistException;

    List<UserDto> getUser();

    UserDto findUserByUsername(String username);

    UserDto findUserByEmail(String email);
}

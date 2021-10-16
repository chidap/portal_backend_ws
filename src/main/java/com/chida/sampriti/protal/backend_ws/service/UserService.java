package com.chida.sampriti.protal.backend_ws.service;

import com.chida.sampriti.protal.backend_ws.dto.UserDto;
import com.chida.sampriti.protal.backend_ws.exception.domain.EmailExistException;
import com.chida.sampriti.protal.backend_ws.exception.domain.EmailNotFoundException;
import com.chida.sampriti.protal.backend_ws.exception.domain.UsernameExistException;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.List;

public interface UserService {
    UserDto registerUser(UserDto userDetails) throws EmailExistException, UsernameExistException, MessagingException;

    List<UserDto> getUser();

    UserDto findUserByUsername(String username);

    UserDto findUserByEmail(String email);

    UserDto addUser(UserDto userDetails, MultipartFile profileImage) throws EmailExistException, UsernameExistException, MessagingException, IOException;

    UserDto updateUser(String currentUsername, UserDto userDetails, MultipartFile profileImage) throws EmailExistException, UsernameExistException, MessagingException, IOException;

    void deleteUser(String userName);

    UserDto updateProfileImage(String Username, MultipartFile profileImage) throws EmailExistException, UsernameExistException, IOException;

    void resetPassword(String email) throws MessagingException, EmailNotFoundException;
}

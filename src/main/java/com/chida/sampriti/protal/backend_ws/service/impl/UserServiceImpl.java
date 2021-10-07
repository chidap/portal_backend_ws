package com.chida.sampriti.protal.backend_ws.service.impl;

import com.chida.sampriti.protal.backend_ws.data.UserEntity;
import com.chida.sampriti.protal.backend_ws.dto.UserDto;
import com.chida.sampriti.protal.backend_ws.enumeration.Role;
import com.chida.sampriti.protal.backend_ws.exception.domain.EmailExistException;
import com.chida.sampriti.protal.backend_ws.exception.domain.UsernameExistException;
import com.chida.sampriti.protal.backend_ws.repository.UserRepository;
import com.chida.sampriti.protal.backend_ws.security.UserPrincipal;
import com.chida.sampriti.protal.backend_ws.service.EmailService;
import com.chida.sampriti.protal.backend_ws.service.LoginAttemptService;
import com.chida.sampriti.protal.backend_ws.service.UserService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.mail.MessagingException;
import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.chida.sampriti.protal.backend_ws.constant.UserImplConstant.*;

@Service
@Transactional
@Qualifier("userDetailsService")
public class UserServiceImpl implements UserService, UserDetailsService {
    private Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);
    private UserRepository userRepository;
    private BCryptPasswordEncoder passwordEncoder;
    private LoginAttemptService loginAttemptService;
    private EmailService emailService;

    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                           BCryptPasswordEncoder passwordEncoder,
                           LoginAttemptService loginAttemptService,
                           EmailService emailService ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
        this.emailService = emailService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity userEntity = userRepository.findUserByUserName(username);
        if (userEntity == null) {
            LOGGER.error(NO_USER_FOUND_BY_USERNAME + username);
            throw new UsernameNotFoundException(NO_USER_FOUND_BY_USERNAME + username);
        } else {
            validateLoginAttempts(userEntity);
            userEntity.setLastLoginDateDisplay(userEntity.getLastLoginDate());
            userEntity.setLastLoginDate(new Date());
            userRepository.save(userEntity);
            ModelMapper modelMapper = new ModelMapper();
            modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
            UserDto userDto = modelMapper.map(userEntity, UserDto.class);
            UserPrincipal userPrincipal = new UserPrincipal(userDto);
            LOGGER.info("Returning found User by name:" + username);
            return userPrincipal;
        }
    }

    private void validateLoginAttempts(UserEntity user) {
        if(user.isNotLocked()) {
            if(loginAttemptService.hasExceededMaxAttempts(user.getUserName())) {
                user.setNotLocked(false);
            } else {
                user.setNotLocked(true);
            }
        } else {
            loginAttemptService.evictUserFromLoginAttemptCache(user.getUserName());
        }
    }

    @Override
    public UserDto registerUser(UserDto userDetails) throws UsernameNotFoundException, EmailExistException, UsernameExistException, MessagingException {
        validateNewUsernameAndEmail(StringUtils.EMPTY, userDetails.getUserName(), userDetails.getEmail());
        userDetails.setMemberId(generateMemberId());
        String password = generatePassword();
        String encodedPassword = encodePassword(password);
        userDetails.setDateOfJoin(new Date());
        userDetails.setPassword(encodedPassword);
        userDetails.setActive(true);
        userDetails.setNotLocked(true);
        userDetails.setRole(Role.ROLE_USER.name());
        userDetails.setAuthorities(Role.ROLE_USER.getAuthorities());
        userDetails.setProfileImageUrl(getTemporaryProfileImageUrl());

        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserEntity userEntity = modelMapper.map(userDetails, UserEntity.class);
        userRepository.save(userEntity);
        LOGGER.info("New user password: " + password);
        UserDto returnValue = modelMapper.map(userEntity, UserDto.class);
        emailService.sendNewPasswordEmail(userDetails.getFirstName(), password, userDetails.getEmail());
        return returnValue;
    }

    private String getTemporaryProfileImageUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(DEFAULT_USER_IMAGE_PATH).toUriString();
    }

    private String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    private String generatePassword() {
        return RandomStringUtils.randomAlphanumeric(10);
    }

    private String generateMemberId() {
        return RandomStringUtils.randomNumeric(10);
    }

    private UserDto validateNewUsernameAndEmail(String currentUsername, String newUsername, String newEmail)
            throws UsernameNotFoundException, UsernameExistException, EmailExistException {

        UserDto userByNewUsername = findUserByUsername(newUsername);
        UserDto userByNewEmail = findUserByEmail(newEmail);
        if(StringUtils.isNotBlank(currentUsername)) {
            UserDto currentUser = findUserByUsername(currentUsername);
            if(currentUser == null) {
                throw new UsernameNotFoundException(NO_USER_FOUND_BY_USERNAME + currentUsername);
            }
            if(userByNewUsername != null && !currentUser.getMemberId().equals(userByNewUsername.getMemberId())) {
                throw new UsernameExistException(USERNAME_ALREADY_EXIST);
            }
            if(userByNewEmail != null && !currentUser.getMemberId().equals(userByNewEmail.getMemberId())) {
                throw new EmailExistException(EMAIL_ALREADY_EXIST);
            }
            return currentUser;
        } else {
             if(userByNewUsername != null) {
                throw new UsernameExistException(USERNAME_ALREADY_EXIST);
            }
            if(userByNewEmail != null ) {
                throw new EmailExistException(EMAIL_ALREADY_EXIST);
            }
            return null;
        }
    }

    @Override
    public List<UserDto> getUser() {
        List<UserEntity> users = userRepository.findAll();
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        List<UserDto> returnUsers = users.stream().map(user -> modelMapper.map(user, UserDto.class)).collect(Collectors.toList());
        return returnUsers;
    }

    @Override
    public UserDto findUserByUsername(String username) {
        UserEntity user = userRepository.findUserByUserName(username);
        if (user != null) {
            ModelMapper modelMapper = new ModelMapper();
            modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
            UserDto returnUser = modelMapper.map(user, UserDto.class);
            return returnUser;
        }
        return null;
    }

    @Override
    public UserDto findUserByEmail(String email) {
        UserEntity user = userRepository.findUserByEmail(email);
        if (user != null) {
            ModelMapper modelMapper = new ModelMapper();
            modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
            UserDto returnUser = modelMapper.map(user, UserDto.class);
            return returnUser;
        }
        return null;
    }
}

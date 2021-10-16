package com.chida.sampriti.protal.backend_ws.service.impl;

import com.chida.sampriti.protal.backend_ws.data.UserEntity;
import com.chida.sampriti.protal.backend_ws.dto.UserDto;
import com.chida.sampriti.protal.backend_ws.enumeration.Role;
import com.chida.sampriti.protal.backend_ws.exception.domain.EmailExistException;
import com.chida.sampriti.protal.backend_ws.exception.domain.EmailNotFoundException;
import com.chida.sampriti.protal.backend_ws.exception.domain.UsernameExistException;
import com.chida.sampriti.protal.backend_ws.repository.UserRepository;
import com.chida.sampriti.protal.backend_ws.security.UserPrincipal;
import com.chida.sampriti.protal.backend_ws.service.EmailService;
import com.chida.sampriti.protal.backend_ws.service.LoginAttemptService;
import com.chida.sampriti.protal.backend_ws.service.UserService;
import com.fasterxml.jackson.annotation.JsonSubTypes;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.mail.MessagingException;
import javax.transaction.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.chida.sampriti.protal.backend_ws.constant.FileConstant.*;
import static com.chida.sampriti.protal.backend_ws.constant.UserImplConstant.*;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

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
        userDetails.setDateOfJoin(new Date());
        userDetails.setPassword(encodePassword(password));
        userDetails.setActive(true);
        userDetails.setNotLocked(true);
        userDetails.setRole(Role.ROLE_USER.name());
        userDetails.setAuthorities(Role.ROLE_USER.getAuthorities());
        userDetails.setProfileImageUrl(getTemporaryProfileImageUrl(userDetails.getUserName()));

        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserEntity userEntity = modelMapper.map(userDetails, UserEntity.class);
        userRepository.save(userEntity);
        LOGGER.info("New user password: " + password);
        UserDto returnValue = modelMapper.map(userEntity, UserDto.class);
        emailService.sendNewPasswordEmail(userDetails.getFirstName(), password, userDetails.getEmail());
        return returnValue;
    }

    @Override
    public UserDto addUser(UserDto userDetails, MultipartFile profileImage) throws EmailExistException, UsernameExistException, MessagingException, IOException {
        validateNewUsernameAndEmail(StringUtils.EMPTY, userDetails.getUserName(), userDetails.getEmail());
        userDetails.setMemberId(generateMemberId());
        String password = generatePassword();
        userDetails.setDateOfJoin(new Date());
        userDetails.setPassword(encodePassword(password));
        userDetails.setActive(true);
        userDetails.setNotLocked(true);
        userDetails.setRole(getRoleEnumName(userDetails.getRole()).name());
        userDetails.setAuthorities(getRoleEnumName(userDetails.getRole()).getAuthorities());
        userDetails.setProfileImageUrl(getTemporaryProfileImageUrl(userDetails.getUserName()));

        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserEntity userEntity = modelMapper.map(userDetails, UserEntity.class);
        userRepository.save(userEntity);
        saveProfileImage(userDetails, profileImage);
        LOGGER.info("New user password: " + password);
        UserDto returnValue = modelMapper.map(userEntity, UserDto.class);
        emailService.sendNewPasswordEmail(userDetails.getFirstName(), password, userDetails.getEmail());
        return returnValue;
    }


    @Override
    public UserDto updateUser(String currentUsername, UserDto userDetails, MultipartFile profileImage) throws EmailExistException, UsernameExistException, MessagingException, IOException {
        UserDto currentUser = validateNewUsernameAndEmail(currentUsername, userDetails.getUserName(), userDetails.getEmail());
        userDetails.setRole(getRoleEnumName(userDetails.getRole()).name());
        userDetails.setAuthorities(getRoleEnumName(userDetails.getRole()).getAuthorities());

        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserEntity userEntity = modelMapper.map(currentUser, UserEntity.class);
        userRepository.save(userEntity);
        saveProfileImage(currentUser, profileImage);
        return currentUser;
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

    @Override
    public void deleteUser(String userName) {
        UserEntity user = userRepository.findUserByUserName(userName);
        if (user != null) {
            Long id = user.getId();
            userRepository.deleteById(id);
        }
    }

    @Override
    public UserDto updateProfileImage(String username, MultipartFile profileImage) throws EmailExistException, UsernameExistException, IOException {
        UserDto userDto = validateNewUsernameAndEmail(username, null, null);
        saveProfileImage(userDto, profileImage);
        return userDto;
    }

    @Override
    public void resetPassword(String email) throws MessagingException, EmailNotFoundException {
        UserEntity userEntity = userRepository.findUserByEmail(email);
        if(userEntity == null) {
            throw new EmailNotFoundException(No_USER_FOUND_BY_EMAIL + email);
        }
        String password = generatePassword();
        userEntity.setPassword(encodePassword(password));
        userRepository.save(userEntity);
        emailService.sendNewPasswordEmail(userEntity.getFirstName(), password, userEntity.getEmail());
    }

    private String getTemporaryProfileImageUrl(String username) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(DEFAULT_USER_IMAGE_PATH + username).toUriString();
    }

    private void saveProfileImage(UserDto userDetails, MultipartFile profileImage) throws IOException {
        if(profileImage != null) {
            Path userFolder = Paths.get(USER_FOLDER + userDetails.getUserName()).toAbsolutePath().normalize();
            if(!Files.exists(userFolder)) {
                Files.createDirectories(userFolder);
                LOGGER.info(DIRECTORY_CREATED + userFolder);
            }
            Files.deleteIfExists(Paths.get(userFolder + userDetails.getUserName() + DOT + JPG_EXTENSION));
            Files.copy(profileImage.getInputStream(), userFolder.resolve(userDetails.getUserName() + DOT + JPG_EXTENSION),REPLACE_EXISTING);
            userDetails.setProfileImageUrl(setProfileImageUrl(userDetails.getUserName()));

            ModelMapper modelMapper = new ModelMapper();
            modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
            UserEntity userEntity = modelMapper.map(userDetails, UserEntity.class);
            userRepository.save(userEntity);
            LOGGER.info(FILE_SAVED_IN_THE_SYSTEM + profileImage.getOriginalFilename());
        }
    }

    private String setProfileImageUrl(String userName) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(USER_IMAGE_PATH + userName + FORWARD_SLASH
        + userName + DOT + JPG_EXTENSION).toUriString();
    }

    private Role getRoleEnumName(String role) {
        return Role.valueOf(role.toUpperCase(Locale.ROOT));
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

}

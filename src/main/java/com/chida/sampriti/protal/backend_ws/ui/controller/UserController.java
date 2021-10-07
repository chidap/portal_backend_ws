package com.chida.sampriti.protal.backend_ws.ui.controller;

import com.chida.sampriti.protal.backend_ws.dto.UserDto;
import com.chida.sampriti.protal.backend_ws.exception.domain.EmailExistException;
import com.chida.sampriti.protal.backend_ws.exception.domain.ExceptionHandling;
import com.chida.sampriti.protal.backend_ws.exception.domain.UsernameExistException;
import com.chida.sampriti.protal.backend_ws.exception.domain.UsernameNotFoundException;
import com.chida.sampriti.protal.backend_ws.security.UserPrincipal;
import com.chida.sampriti.protal.backend_ws.service.UserService;
import com.chida.sampriti.protal.backend_ws.ui.model.CreateUserRequestModel;
import com.chida.sampriti.protal.backend_ws.ui.model.CreateUserResponseModel;
import com.chida.sampriti.protal.backend_ws.utility.JWTTokenProvider;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import javax.mail.MessagingException;
import javax.validation.Valid;

import static com.chida.sampriti.protal.backend_ws.constant.SecurityConstant.JWT_TOKEN_HEADER;

@RestController
@RequestMapping(path = {"/", "/user"})
public class UserController extends ExceptionHandling {
    private UserService userService;
    private AuthenticationManager authenticationManager;
    private JWTTokenProvider jwtTokenProvider;

    @Autowired
    public UserController(UserService userService, AuthenticationManager authenticationManager, JWTTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<CreateUserResponseModel> registerUser(@Valid @RequestBody CreateUserRequestModel userDetails)
            throws UsernameNotFoundException, UsernameExistException, EmailExistException, MessagingException {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserDto newUser = modelMapper.map(userDetails, UserDto.class);

        UserDto createdUser = userService.registerUser(newUser);
        CreateUserResponseModel returnUser = modelMapper.map(createdUser, CreateUserResponseModel.class);
        return ResponseEntity.status(HttpStatus.CREATED).body(returnUser);
    }

    @PostMapping("/login")
    public ResponseEntity<CreateUserResponseModel> login(@RequestBody CreateUserRequestModel userDetails) {
        authenticateUser(userDetails.getUserName(), userDetails.getPassword());
        UserDto loginUser = userService.findUserByUsername(userDetails.getUserName());
        UserPrincipal userPrincipal = new UserPrincipal(loginUser);
        HttpHeaders jwtHeader = getJwtHeader(userPrincipal);

        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        CreateUserResponseModel returnUser = modelMapper.map(loginUser, CreateUserResponseModel.class);
        return new ResponseEntity<>(returnUser, jwtHeader, HttpStatus.OK);
    }

    private HttpHeaders getJwtHeader(UserPrincipal userPrincipal) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(JWT_TOKEN_HEADER, jwtTokenProvider.generateJwtToken(userPrincipal));
        return headers;
    }

    private void authenticateUser(String userName, String password) {
        authenticationManager.authenticate( new UsernamePasswordAuthenticationToken(userName, password));
    }
}

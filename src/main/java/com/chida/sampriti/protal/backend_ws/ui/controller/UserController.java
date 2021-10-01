package com.chida.sampriti.protal.backend_ws.ui.controller;

import com.chida.sampriti.protal.backend_ws.dto.UserDto;
import com.chida.sampriti.protal.backend_ws.exception.domain.EmailExistException;
import com.chida.sampriti.protal.backend_ws.exception.domain.ExceptionHandling;
import com.chida.sampriti.protal.backend_ws.exception.domain.UsernameExistException;
import com.chida.sampriti.protal.backend_ws.exception.domain.UsernameNotFoundException;
import com.chida.sampriti.protal.backend_ws.service.UserService;
import com.chida.sampriti.protal.backend_ws.ui.model.CreateUserRequestModel;
import com.chida.sampriti.protal.backend_ws.ui.model.CreateUserResponseModel;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping(path = {"/", "/user"})
public class UserController extends ExceptionHandling {
    private UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<CreateUserResponseModel> registerUser(@Valid @RequestBody CreateUserRequestModel userDetails)
            throws UsernameNotFoundException, UsernameExistException, EmailExistException {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserDto newUser = modelMapper.map(userDetails, UserDto.class);

        UserDto createdUser = userService.registerUser(newUser);
        CreateUserResponseModel returnUser = modelMapper.map(createdUser, CreateUserResponseModel.class);
        return ResponseEntity.status(HttpStatus.CREATED).body(returnUser);
    }
}

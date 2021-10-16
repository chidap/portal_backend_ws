package com.chida.sampriti.protal.backend_ws.ui.controller;

import com.chida.sampriti.protal.backend_ws.dto.UserDto;
import com.chida.sampriti.protal.backend_ws.exception.domain.*;
import com.chida.sampriti.protal.backend_ws.security.UserPrincipal;
import com.chida.sampriti.protal.backend_ws.service.UserService;
import com.chida.sampriti.protal.backend_ws.ui.model.CreateUserRequestModel;
import com.chida.sampriti.protal.backend_ws.ui.model.CreateUserResponseModel;
import com.chida.sampriti.protal.backend_ws.ui.response.HttpResponse;
import com.chida.sampriti.protal.backend_ws.utility.JWTTokenProvider;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.MessagingException;
import javax.validation.Valid;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static com.chida.sampriti.protal.backend_ws.constant.FileConstant.*;
import static com.chida.sampriti.protal.backend_ws.constant.SecurityConstant.JWT_TOKEN_HEADER;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.IMAGE_JPEG_VALUE;

@RestController
@RequestMapping(path = {"/", "/user"})
public class UserController extends ExceptionHandling {
    public static final String EMAIL_SENT = "An email with a new password was sent to: ";
    public static final String USER_DELETED_SUCCESSFULLY = "USER_DELETED_SUCCESSFULLY";
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

    @PostMapping("/add")
    public ResponseEntity<CreateUserResponseModel> addUser(@RequestParam("firstName") String firstName,
                                                           @RequestParam("lastName") String lastName,
                                                           @RequestParam("email") String email,
                                                           @RequestParam("userName") String userName,
                                                           @RequestParam("role") String role,
                                                           @RequestParam("isActive") String isActive,
                                                           @RequestParam("isNotLocked") String isNotLocked,
                                                           @RequestParam(value = "profileImage", required = false) MultipartFile profileImage) throws EmailExistException, MessagingException, IOException, UsernameExistException {
        UserDto newUser = new UserDto();
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        newUser.setEmail(email);
        newUser.setUserName(userName);
        newUser.setActive(Boolean.parseBoolean(isActive));
        newUser.setNotLocked(Boolean.parseBoolean(isNotLocked));
        UserDto createdUser = userService.addUser(newUser, profileImage);

        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        CreateUserResponseModel returnUser = modelMapper.map(createdUser, CreateUserResponseModel.class);
        return ResponseEntity.status(HttpStatus.CREATED).body(returnUser);
    }


    @PostMapping("/update")
    public ResponseEntity<CreateUserResponseModel> updateUser(@RequestParam("currentUserName") String currentUserName,
                                                           @RequestParam("firstName") String firstName,
                                                           @RequestParam("lastName") String lastName,
                                                           @RequestParam("email") String email,
                                                           @RequestParam("userName") String userName,
                                                           @RequestParam("role") String role,
                                                           @RequestParam("isActive") String isActive,
                                                           @RequestParam("isNotLocked") String isNotLocked,
                                                           @RequestParam(value = "profileImage", required = false) MultipartFile profileImage) throws EmailExistException, MessagingException, IOException, UsernameExistException {
        UserDto newUser = new UserDto();
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        newUser.setEmail(email);
        newUser.setUserName(userName);
        newUser.setActive(Boolean.parseBoolean(isActive));
        newUser.setNotLocked(Boolean.parseBoolean(isNotLocked));
        UserDto updatedUser = userService.updateUser(currentUserName, newUser, profileImage);

        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        CreateUserResponseModel returnUser = modelMapper.map(updatedUser, CreateUserResponseModel.class);
        return new ResponseEntity<>(returnUser, OK);
    }

    @GetMapping("/find/{userName}")
    public ResponseEntity<CreateUserResponseModel> getUser(@PathVariable("userName") String userName) {
        UserDto user = userService.findUserByUsername(userName);
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        CreateUserResponseModel returnUser = modelMapper.map(user, CreateUserResponseModel.class);
        return new ResponseEntity<>(returnUser, OK);
    }

    @GetMapping("/list")
    public ResponseEntity<List<CreateUserResponseModel>> getAllUser() {
        List<UserDto> users = userService.getUser();
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        List<CreateUserResponseModel> returnUsers = users.stream().map(user -> modelMapper.map(user, CreateUserResponseModel.class)).collect(Collectors.toList());
        return new ResponseEntity<>(returnUsers, OK);
    }

    @GetMapping("/resetPassword/{email}")
    public ResponseEntity<HttpResponse> resetPassword(@PathVariable("email") String email) throws EmailNotFoundException, MessagingException {
        userService.resetPassword(email);
        return response(OK, EMAIL_SENT + email);
    }

    @DeleteMapping("/delete/{userName}")
    @PreAuthorize("hasAnyAuthority('user:delete')")
    public ResponseEntity<HttpResponse> deleteUser(@PathVariable("userName") String userName) {
        userService.deleteUser(userName);
        return response(NO_CONTENT, USER_DELETED_SUCCESSFULLY);
    }

    @PostMapping("/updateProfileImage")
    public ResponseEntity<CreateUserResponseModel> updateProfileImage(@RequestParam("userName") String userName, @RequestParam(value = "profileImage") MultipartFile profileImage) throws EmailExistException, MessagingException, IOException, UsernameExistException {
        UserDto user = userService.updateProfileImage(userName, profileImage);
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        CreateUserResponseModel returnUser = modelMapper.map(user, CreateUserResponseModel.class);
        return new ResponseEntity<>(returnUser, OK);
    }

    @GetMapping(path = "/image/{userName}/{fileName}", produces = IMAGE_JPEG_VALUE)
    public byte[] getProfileImage(@PathVariable("userName") String userName, @PathVariable("fileName") String fileName) throws IOException {
        return Files.readAllBytes(Paths.get(USER_FOLDER + userName + FORWARD_SLASH + fileName));
    }

    @GetMapping(path = "/image/profile/{userName}", produces = IMAGE_JPEG_VALUE)
    public byte[] getTempProfileImage(@PathVariable("userName") String userName ) throws IOException {
        URL url = new URL(TEMP_PROFILE_IMAGE_BASE_URL + userName);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (InputStream inputStream = url.openStream()) {
            int bytesRead;
            byte[] chunk = new byte[1024];
            while((bytesRead = inputStream.read(chunk)) > 0 ) {
                byteArrayOutputStream.write(chunk, 0, bytesRead);
            }
        }
        return byteArrayOutputStream.toByteArray();
    }


    private ResponseEntity<HttpResponse> response(HttpStatus httpStatus, String message) {
        HttpResponse body = new HttpResponse(httpStatus.value(), httpStatus, httpStatus.getReasonPhrase().toUpperCase(), message.toUpperCase() );
        return new ResponseEntity<>(body, httpStatus);
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
        return new ResponseEntity<>(returnUser, jwtHeader, OK);
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

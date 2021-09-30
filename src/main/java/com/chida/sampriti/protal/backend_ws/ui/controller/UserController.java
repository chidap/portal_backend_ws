package com.chida.sampriti.protal.backend_ws.ui.controller;

import com.chida.sampriti.protal.backend_ws.exception.domain.EmailExistException;
import com.chida.sampriti.protal.backend_ws.exception.domain.ExceptionHandling;
import com.chida.sampriti.protal.backend_ws.exception.domain.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = {"/", "/user"})
public class UserController extends ExceptionHandling {

    @GetMapping("/home")
    public String showUser() throws UsernameNotFoundException {
        //return "application works";
        throw new UsernameNotFoundException("This User was not found");
    }
}

package com.neu.demo.controller;

import com.neu.demo.security.AuthenticateService;
import com.nimbusds.jose.JOSEException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author lyupingdu
 * @date 2019-07-29.
 */
@RestController
@RequestMapping("/authenticate")
public class AuthController {

    @PostMapping()
    public ResponseEntity<Object> authenticate() throws JOSEException {
        System.out.println("auth");
        return ResponseEntity.status(HttpStatus.OK).body("token: " + AuthenticateService.generatePrivateKey());
    }

}

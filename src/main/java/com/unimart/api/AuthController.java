package com.unimart.api;

import com.unimart.api.dto.AuthDtos.RequestCodeRequest;
import com.unimart.api.dto.AuthDtos.SignUpRequest;
import com.unimart.api.dto.AuthDtos.VerifyCodeRequest;
import com.unimart.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/sign-up")
    public Object signUp(@Valid @RequestBody SignUpRequest request) {
        return authService.signUp(request.email(), request.displayName());
    }

    @PostMapping("/request-code")
    public Object requestCode(@Valid @RequestBody RequestCodeRequest request) {
        return authService.requestCode(request.email());
    }

    @PostMapping("/verify")
    public Object verify(@Valid @RequestBody VerifyCodeRequest request) {
        return authService.verifyCode(request.email(), request.code());
    }
}

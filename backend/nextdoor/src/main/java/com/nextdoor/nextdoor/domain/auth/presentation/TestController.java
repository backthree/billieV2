package com.nextdoor.nextdoor.domain.auth.presentation;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestController {

    @GetMapping("/social-login")
    public String socialLogin() {
        return "forward:social-login.html";
    }
}

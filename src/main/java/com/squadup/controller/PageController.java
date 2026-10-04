package com.squadup.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Classic Spring MVC {@code @Controller}: it returns a view name instead of a response body.
 * The web UI itself is static HTML under src/main/resources/static.
 */
@Controller
public class PageController {

    /** The home page redirects to login.html by itself when nobody is logged in. */
    @GetMapping("/")
    public String index() {
        return "redirect:/home.html";
    }
}

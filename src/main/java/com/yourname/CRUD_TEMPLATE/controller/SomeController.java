package com.yourname.CRUD_TEMPLATE.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * REST Controller to handle HTTP requests.
 * Rename this class to reflect its purpose, e.g., PersonController if it manages Person entities.
 */
@RestController
public class SomeController {

    @RequestMapping("/")
    public String index() {
        return "Greetings from Spring Boot!";
    }

    // Define other endpoints and request methods here
}

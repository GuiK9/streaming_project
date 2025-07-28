package com.streaming.backend.utilities;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ProfileChecker {
    public ProfileChecker(Environment env) {
        System.out.println("Enviroments active: " + String.join(", ", env.getActiveProfiles()));
    }
}

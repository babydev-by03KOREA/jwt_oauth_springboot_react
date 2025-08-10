package com.common.api.login;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LoginApplication {

    public static void main(String[] args) {
        io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.load();
        System.setProperty("MYAPP_KAKAO_CLIENT_ID", dotenv.get("MYAPP_KAKAO_CLIENT_ID"));
        System.setProperty("MYAPP_KAKAO_CLIENT_SECRET", dotenv.get("MYAPP_KAKAO_CLIENT_SECRET"));
        System.setProperty("MYAPP_GOOGLE_CLIENT_ID", dotenv.get("MYAPP_GOOGLE_CLIENT_ID"));
        System.setProperty("MYAPP_GOOGLE_CLIENT_SECRET", dotenv.get("MYAPP_GOOGLE_CLIENT_SECRET"));
        SpringApplication.run(LoginApplication.class, args);
    }

}

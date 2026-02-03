package dev.gmpark.cors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;



@SpringBootApplication
public class CorsApplication {

    public static int AI_SERVER_PORT = 8000; // 기본값

    public static void main(String[] args) {
        SpringApplication.run(CorsApplication.class, args);
    }
}

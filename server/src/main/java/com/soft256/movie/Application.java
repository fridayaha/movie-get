package com.soft256.movie;

import com.soft256.movie.controller.MovieControllerUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        MovieControllerUtil.loadMovieInfo();
    }

    @Bean
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }
}
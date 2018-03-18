package com.soft256.movie;

import com.soft256.movie.controller.MovieControllerUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableScheduling
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);

        //加载已经保存的电影信息
        MovieControllerUtil.loadMovieInfo();

        //初始化电影地址解析引擎
        MovieControllerUtil.initEngines();
    }

    @Bean
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }
}
package com.soft256.movie.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soft256.movie.engine.DownloadPathFetchEngine;
import org.apache.commons.io.FileUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@Controller
@EnableAutoConfiguration
public class MovieAPIController {

    private final String MOVIE_NAME_KEY = "title";

    private final String MOVIE_ID_KEY = "id";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private RestTemplate restTemplate;

    @RequestMapping("/v2/**")
    @ResponseBody
    String doubanMovieAPIFetch(HttpServletRequest request, HttpServletResponse response) {
        String originalUrl = request.getRequestURI();

        Map<String, String[]> parameterMap = request.getParameterMap();
        String parameterURL = "";
        if (parameterMap.size() > 0) {
            StringBuffer parameterBuffer = new StringBuffer("?");
            for (String parameterName : parameterMap.keySet()) {
                parameterBuffer.append(parameterName).append("=").append(parameterMap.get(parameterName)[0]).append("&");
            }
            parameterURL = parameterBuffer.toString();
        }

        String doubanUrl = "https://api.douban.com".concat(originalUrl.replace("/movieapi", "")).concat(parameterURL);
        final String responseEntity = restTemplate.getForObject(doubanUrl, String.class, request.getParameterMap());
        if (originalUrl.indexOf("subject") > -1) {
            String content = responseEntity;
            ObjectMapper mapper = new ObjectMapper();
            try {
                Map<String, Object> subject = mapper.readValue(content, Map.class);
                List<String> downloadPaths = fetchDownloadPath(subject);
                if (null != downloadPaths && downloadPaths.size() > 0) {
                    subject.put("downloads", downloadPaths);
                }

                return mapper.writeValueAsString(subject);
            } catch (IOException e) {
                logger.error("Parse response entity error,request url is " + doubanUrl, e);
            }
        } else {
            final long lastAutoFetchingTime = MovieControllerUtil.getAutoFetchTime();
            final long nowTime = new Date().getTime();
            long betweenHours = ((nowTime - lastAutoFetchingTime) / (1000 * 60 * 60));
            if (betweenHours > MovieControllerUtil.AUTO_FETCH_BETWEEN_HOURS) {
                new Thread("AutoFetchingThread") {
                    @Override
                    public void run() {
                        MovieControllerUtil.setAutoFetchTime(nowTime);
                        autoFetch(responseEntity);
                    }
                }.start();
            } else {
                logger.info("Auto fetching thread does not start,last start time " + lastAutoFetchingTime);
            }
        }
        return responseEntity;
    }

    public void autoFetch(String subjectsContent) {
        logger.info("Start auto fetch movie download path.");
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map m = mapper.readValue(subjectsContent, Map.class);
            List<Map<String, Object>> subjects = (List<Map<String, Object>>) m.get("subjects");
            for (Map<String, Object> subject : subjects) {
                fetchDownloadPath(subject);
            }
        } catch (IOException e) {
            logger.error("Parse response entity error", e);
        }
        logger.info("End auto fetch movie download path.");
    }


    private List<String> fetchDownloadPath(Map<String, Object> subject) {
        String movieId = (String) subject.get(MOVIE_ID_KEY);
        String movieName = (String) subject.get(MOVIE_NAME_KEY);
        List<String> downloadPaths = MovieControllerUtil.getMovieDownloadPathCache().get(movieId);
        if (null == downloadPaths) {
            downloadPaths = new ArrayList<>(MovieControllerUtil.getEngines().size() * 3);
        }
        if (downloadPaths.size() == 0) {
            List<DownloadPathFetchEngine> engines = MovieControllerUtil.getEngines();
            for (DownloadPathFetchEngine engine : engines) {
                logger.info("Start fetch movie named " + movieName + " from site " + engine.getMainPage());
//                if (downloadPaths.size() > 0) {
//                    logger.info("Fetch movie named " + movieName + " success!");
//                }
                downloadPaths.addAll(engine.fetchDownloadPath(movieName));
                logger.info("End fetch movie named " + movieName + " from site " + engine.getMainPage());
            }
            if (downloadPaths.size() > 0) {
                MovieControllerUtil.getMovieDownloadPathCache().put(movieId, downloadPaths);
                MovieControllerUtil.saveMovieInfo();
            }
        }
        return downloadPaths;
    }

    /**
     * 最新电影列表
     */
    @RequestMapping("/v2/movie/newest")
    @ResponseBody
    public String newestMovies() {
        try {
            return FileUtils.readFileToString(MovieControllerUtil.MOVIE_NEWEST_FILE);
        } catch (IOException e) {
            logger.info("Return newest movie list error ", e);
        }
        return "{}";
    }
}
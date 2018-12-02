package com.soft256.movie.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soft256.movie.engine.DownloadPathFetchEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Controller
@EnableAutoConfiguration
public class MovieAPIController {

    private final String MOVIE_NAME_KEY = "title";

    private final String MOVIE_ID_KEY = "id";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private List<DownloadPathFetchEngine> engines;

    @RequestMapping("/v2/**")
    @ResponseBody
    String doubanMovieAPIFetch(HttpServletRequest request) {
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
                List<String> downloadPaths = fetchDownloadPath(subject, mapper);
                downloadPaths.sort(Comparator.naturalOrder());
                if (null != downloadPaths && downloadPaths.size() > 0) {
                    subject.put("downloads", downloadPaths);
                }
                return mapper.writeValueAsString(subject);
            } catch (IOException e) {
                logger.error("Parse response entity error,request url is " + doubanUrl, e);
            }
        } else {
            if (redisTemplate.hasKey(MovieControllerConstants.FETCH_TIME_KEY)) {
                logger.info("Auto fetching thread does not start,because not more than " + MovieControllerConstants.AUTO_FETCH_BETWEEN_HOURS + " hour.");
            } else {
                new Thread("AutoFetchingThread") {
                    @Override
                    public void run() {
                        redisTemplate.expire(MovieControllerConstants.FETCH_TIME_KEY, MovieControllerConstants.AUTO_FETCH_BETWEEN_HOURS, TimeUnit.HOURS);
                        autoFetch(responseEntity);
                    }
                }.start();
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
                fetchDownloadPath(subject, mapper);
            }
        } catch (IOException e) {
            logger.error("Parse response entity error", e);
        }
        logger.info("End auto fetch movie download path.");
    }


    private List<String> fetchDownloadPath(Map<String, Object> subject, ObjectMapper mapper) {
        String movieId = (String) subject.get(MOVIE_ID_KEY);
        String movieName = (String) subject.get(MOVIE_NAME_KEY);
        List<String> downloadPaths = new ArrayList<>(engines.size() * 3);
        if (redisTemplate.hasKey(MovieControllerConstants.MOVIE_ID_PREFIX.concat(movieId))) {
            String movieContent = redisTemplate.opsForValue().get(MovieControllerConstants.MOVIE_ID_PREFIX.concat(movieId));
            Map contentMap = null;
            try {
                contentMap = mapper.readValue(movieContent, Map.class);
            } catch (IOException e) {
                logger.error("Read movie content from redis error movie id is" + movieId);
            }
            if (contentMap != null)
                downloadPaths.addAll((List) contentMap.get("downloads"));
        }

        if (downloadPaths.size() == 0) {
            for (DownloadPathFetchEngine engine : engines) {
                logger.info("Start fetch movie named " + movieName + " from site " + engine.getMainPage());
                List<String> fetchResult = engine.fetchDownloadPath(movieName);
                if (fetchResult.size() > 0) {
                    logger.info("Fetch movie named " + movieName + " success!");
                }
                downloadPaths.addAll(fetchResult);
                logger.info("End fetch movie named " + movieName + " from site " + engine.getMainPage());
            }
            if (downloadPaths.size() > 0) {
                subject.put("downloads", downloadPaths);
                try {
                    redisTemplate.opsForValue().set(movieId, mapper.writeValueAsString(subject));
                } catch (Exception e) {
                    logger.error("Save download path to redis error..");
                }
            }
        }
        return downloadPaths;
    }

    /**
     * 最新电影列表
     */
    @RequestMapping("/v2/movie/newest")
    @ResponseBody
    public String newestMovies(@RequestParam int start, @RequestParam int count) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Set<String> keys = redisTemplate.keys(MovieControllerConstants.MOVIE_ID_PREFIX + "*");
            ValueOperations<String, String> ops = redisTemplate.opsForValue();
            List<String> sortedKeys = CollectionUtils.arrayToList(StringUtils.sortStringArray(keys.toArray(new String[keys.size()])));
            int total = sortedKeys.size();
            List<String> sortedSubKeys = sortedKeys.subList(start > total ? total : start, count > total - start ? total : count);
            Map<String, Object> contentMap = new HashMap<>();
            List<Object> subSubjects = new ArrayList(count);
            for (String key : sortedSubKeys) {
                String value = ops.get(key);
                subSubjects.add(mapper.readValue(value, HashMap.class));
            }
            contentMap.put("subjects", subSubjects);
            contentMap.put("count", subSubjects.size());
            contentMap.put("start", start);
            contentMap.put("total", total);
            contentMap.put("subjects", subSubjects);
            return mapper.writeValueAsString(contentMap);
        } catch (IOException e) {
            logger.info("Return newest movie list error ", e);
        }
        return "{}";
    }

    /**
     * 获取最新电影
     */
    @RequestMapping("/v2/movie/fetch")
    @ResponseBody
    public String fetchNewestMovies() {
        for (DownloadPathFetchEngine engine : engines) {
            engine.fetchNewestMovieList();
        }
        return "success";
    }
}
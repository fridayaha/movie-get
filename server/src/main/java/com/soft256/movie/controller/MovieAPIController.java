package com.soft256.movie.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@EnableAutoConfiguration
public class MovieAPIController {

    private final String MOVIE_NAME_KEY = "title";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private RestTemplate restTemplate;

    @RequestMapping("/v2/**")
    @ResponseBody
    String doubanMovieAPIFeatch(HttpServletRequest request, HttpServletResponse response) {
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

    private List<String> fetchSearchResults(String movieName) throws Exception {
        logger.info("Fetch movie named " + movieName);
        List<String> downloadPaths = new ArrayList<String>(5);
        String dy2018Url = "https://www.dy2018.com/e/search/";
        String searchURL = dy2018Url + "index.php";

        HttpHeaders headers = new HttpHeaders();
        MediaType type = new MediaType(MediaType.APPLICATION_FORM_URLENCODED, Charset.forName("gb2312"));
        headers.setContentType(type);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.add("keyboard", movieName);
        params.add("show", "title,smalltext");
        params.add("tempid", "1");
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(params, headers);
        RestTemplate restTemplate1 = new RestTemplate();
        restTemplate1.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("gb2312")));
        String searchResult = restTemplate1.postForObject(searchURL, entity, String.class);

        List<String> hrefLink = getLink(searchResult.toLowerCase());
        if (hrefLink.size() == 1 && hrefLink.get(0).indexOf("javascript:") == -1) {
            fetchMovieInfoPath(movieName, dy2018Url + hrefLink.get(0).split("\"")[1], downloadPaths);
        }
        if (downloadPaths.size() > 0) {
            logger.info("Fetch movie named " + movieName + " success!");
        }
        return downloadPaths;
    }

    private void fetchMovieInfoPath(String movieName, String resultUrl, List<String> downloadList) {
        try {
            Document doc = Jsoup.connect(resultUrl).timeout(30000).get();
            Elements links = doc.select("a[class=\"ulink\"]");

            for (Element element : links) {
                //TODO:优化匹配算法
                String link = element.attr("href");
                String movieTitle = element.attr("title");
                if (MovieControllerUtil.nameMatch(movieName, movieTitle)) {
                    downloadList.addAll(fetchDownloadPath("https://www.dy2018.com/" + link));
                    break;
                }
            }
        } catch (IOException e) {
            logger.error("Fetch movie info error, request url is " + resultUrl, e);
        }
    }

    /**
     * 从详情页中分析下载链接
     *
     * @param resultUrl
     * @return
     */
    private List<String> fetchDownloadPath(String resultUrl) {
        List<String> downloadPath = new ArrayList<String>(5);
        try {
            Document doc = Jsoup.connect(resultUrl).get();
            Elements links = doc.select("a[href]");
            for (Element element : links) {
                if (element.attr("href").startsWith("ftp")) {
                    downloadPath.add(element.attr("href"));
                }
            }

        } catch (IOException e) {
            logger.error("Fetch download info error, request url is " + resultUrl, e);
        }
        return downloadPath;
    }


    /**
     * @param s
     * @return 获得链接
     */
    public List<String> getLink(final String s) {
        String regex;
        final List<String> list = new ArrayList<String>();
        regex = "<a[^>]*href=(\"([^\"]*)\"|\'([^\']*)\'|([^\\s>]*))[^>]*>(.*?)</a>";
        final Pattern pa = Pattern.compile(regex, Pattern.DOTALL);
        final Matcher ma = pa.matcher(s);
        while (ma.find()) {
            list.add(ma.group());
        }
        return list;
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
        String movieId = (String) subject.get("id");
        String movieName = (String) subject.get(MOVIE_NAME_KEY);
        List<String> downloadPaths = MovieControllerUtil.getMovieDownloadPathCache().get(movieId);
        if (null == downloadPaths || downloadPaths.size() == 0) {
            try {
                downloadPaths = fetchSearchResults(movieName);
            } catch (Exception e) {
                logger.info("Fetch movie named " + movieName + " error!", e);
                //TODO:后续加入重试机制
            }
            if (null != downloadPaths && downloadPaths.size() > 0) {
                MovieControllerUtil.getMovieDownloadPathCache().put(movieId, downloadPaths);
                MovieControllerUtil.saveMovieInfo();
            }
        }
        return downloadPaths;
    }
}
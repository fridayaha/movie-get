package com.soft256.movie.engine.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soft256.movie.controller.MovieControllerUtil;
import com.soft256.movie.engine.DownloadPathFetchEngine;
import org.apache.commons.io.FileUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

/**
 * Created by friday on 2018/3/17.
 */
public class FetchEngineForYgdy8 implements DownloadPathFetchEngine {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String MAIN_PAGE_URL = "http://www.ygdy8.com";

    public List<String> fetchDownloadPath(String movieName) {
        String encodedName = movieName;
        try {
            encodedName = URLEncoder.encode(movieName, "GBK");
        } catch (UnsupportedEncodingException e) {
            //忽略，不可能转码失败
        }
        String searchURL = "http://s.ygdy8.com/plus/so.php?kwtype=0&searchtype=title&keyword=" + encodedName;
        Connection connection = Jsoup.connect(searchURL);
        connection.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.87 Safari/537.36");
        connection.header("Content-type", "text/html; charset=gb2312");
        List<String> downloadPaths = new ArrayList<>(5);
        try {
            Document document = connection.timeout(30000).get();
            Elements elements = document.select("div[class=\"co_content8\"]");
            elements.forEach(contentElement -> {
                Elements urlLinks = contentElement.getElementsByTag("a");
                for (int i = 0; i < urlLinks.size(); i++) {
                    Element hrefLinkElement = urlLinks.get(i);
                    if (hrefLinkElement.html().indexOf("《<font color=\"red\">" + movieName + "</font>》") > -1) {
                        parserMovieDetail(MAIN_PAGE_URL + hrefLinkElement.attr("href"), downloadPaths);
                        break;
                    }
                }
            });
        } catch (IOException e) {
            logger.error("Fetch download path error, request url is " + searchURL, e);
        }

        return downloadPaths;
    }

    /**
     * 解析电影详情
     *
     * @param resultUrl
     * @param downloadList
     */
    private void parserMovieDetail(String resultUrl, List<String> downloadList) {
        try {
            Document doc = Jsoup.connect(resultUrl).timeout(30000).get();
            Element movieInfoElement = doc.getElementById("Zoom");
            Elements linkElements = movieInfoElement.getElementsByTag("a");
            linkElements.forEach(linkElement -> {
                String link = linkElement.attr("href");
                if (link.indexOf("magnet:?xt") > -1) {
                    downloadList.add(link.substring(0, link.indexOf("&")));
                } else if (link.startsWith("ftp")) {
                    downloadList.add(link);
                }
            });
        } catch (IOException e) {
            logger.error("Parser movie info error, request url is " + resultUrl, e);
        }
    }


    public void fetchNewestMovieList() {
        String searchURL = MAIN_PAGE_URL + "/html/gndy/dyzz/index.html";
        String doubanMovieSearch = "https://api.douban.com/v2/movie/search?q={q}&start=0&count=1";

        Map<String, Object> totalsubject = new HashMap<>();
        List totalSubjects = new ArrayList();

        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper mapper = new ObjectMapper();

        Connection connection = Jsoup.connect(searchURL);
        Document document = null;
        try {
            document = connection.timeout(30000).get();
        } catch (IOException e) {
            logger.error("Enter movie search main page error.", e);
        }
        if (null == document) {
            return;
        }
        Elements movieLinks = document.select("a[class=\"ulink\"]");
        movieLinks.forEach(hrefLinkElement -> {
            String movieName = hrefLinkElement.text();
            String realMovieName = StringUtils.trimWhitespace(movieName.substring(movieName.indexOf("《") + 1, movieName.indexOf("》")));
            Map<String, Object> parameterMap = new HashMap<>();
            parameterMap.put("q", realMovieName);
            try {
                final String responseEntity = restTemplate.getForObject(doubanMovieSearch, String.class, parameterMap);
                Map<String, Object> searchResult = mapper.readValue(responseEntity, Map.class);
                List subjects = (List) searchResult.get("subjects");
                Map subjectMap = (Map) subjects.get(0);
                //解析电影下载链接
                List<String> downloadPaths = new ArrayList<>(5);
                parserMovieDetail(MAIN_PAGE_URL + hrefLinkElement.attr("href"), downloadPaths);
                subjectMap.put("downloads", downloadPaths);
                totalSubjects.add(subjectMap);
            } catch (Exception e) {
                logger.error("Parser movie info error.", e);
            }
        });
        totalsubject.put("subjects", totalSubjects);
        totalsubject.put("count", 20);
        totalsubject.put("start", 0);
        totalsubject.put("total", totalSubjects.size());
        totalsubject.put("last_update_time", new Date());

        try {
            FileUtils.writeStringToFile(MovieControllerUtil.MOVIE_NEWEST_FILE, mapper.writeValueAsString(totalsubject));
        } catch (Exception e) {
            logger.error("Write newest movie list to file error", e);
        }
    }

    @Override
    public String getMainPage() {
        return MAIN_PAGE_URL;
    }
}

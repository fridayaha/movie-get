package com.soft256.movie.engine.impl;

import com.soft256.movie.engine.DownloadPathFetchEngine;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by friday on 2018/3/17.
 */
@Service("Cililianc")
public class FetchEngineForCililianc implements DownloadPathFetchEngine {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String MAIN_PAGE_URL = "http://cililianc.com/";
    private int tempCount = 0;

    //最多处理前三条记录
    private final int MAX_COUNT = 3;

    @Override
    public List<String> fetchDownloadPath(String movieName) {

        List<String> downloadList = new ArrayList<>(MAX_COUNT);

        String searchURL = MAIN_PAGE_URL + "list/" + movieName + "/1/time.html";
        try {
            Connection connection = Jsoup.connect(searchURL);
            connection.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.87 Safari/537.36");
            Document doc = connection.timeout(30000).get();
            Elements links = doc.select("div[class=\"dInfo\"]");
            tempCount = 0;
            links.forEach(element -> {
                if (tempCount < MAX_COUNT) {
                    downloadList.add(StringUtils.trimAllWhitespace(element.getElementsByTag("a").get(0).attr("href")));
                    tempCount++;
                }
            });

        } catch (IOException e) {
            logger.error("Fetch download info error, request url is " + searchURL, e);
        }
        return downloadList;
    }

    @Override
    public String getMainPage() {
        return MAIN_PAGE_URL;
    }
}

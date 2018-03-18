package com.soft256.movie.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soft256.movie.engine.DownloadPathFetchEngine;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Created by friday on 2018/2/10.
 */
public class MovieControllerUtil {

    //自动抓取时间间隔
    public static final long AUTO_FETCH_BETWEEN_HOURS = 1;

    private final static Logger logger = LoggerFactory.getLogger(MovieControllerUtil.class);

    private final static Map<String, List<String>> MOVIE_DOWNLOAD_PATH_CACHE = new HashMap<String, List<String>>();

    private final static File MOVIE_CACHE_FILE = new File("/home/movieCache.json");
    public final static File MOVIE_NEWEST_FILE = new File("/home/movieNewest.json");

    private static String KEY_AUTO_FETCH_START_TIME = "startTime";

    private final static List<DownloadPathFetchEngine> ENGINES = new ArrayList<>();

    public static Map<String, List<String>> getMovieDownloadPathCache() {
        return MOVIE_DOWNLOAD_PATH_CACHE;
    }

    public static void loadMovieInfo() {
        if (MOVIE_CACHE_FILE.exists()) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                String content = FileUtils.readFileToString(MOVIE_CACHE_FILE);
                MOVIE_DOWNLOAD_PATH_CACHE.putAll(mapper.readValue(content, MOVIE_DOWNLOAD_PATH_CACHE.getClass()));
            } catch (Exception e) {
                logger.error("Load movie info error", e);
            }
        }
    }

    public static void saveMovieInfo() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String content = mapper.writeValueAsString(MOVIE_DOWNLOAD_PATH_CACHE);
            FileUtils.writeStringToFile(MOVIE_CACHE_FILE, content);
        } catch (Exception e) {
            logger.error("Save movie info error", e);
        }
    }

    public static long getAutoFetchTime() {
        if (MOVIE_DOWNLOAD_PATH_CACHE.containsKey(KEY_AUTO_FETCH_START_TIME)) {
            return Long.parseLong(MOVIE_DOWNLOAD_PATH_CACHE.get(KEY_AUTO_FETCH_START_TIME).get(0));
        }
        return 0L;
    }

    public static void setAutoFetchTime(long time) {
        //缓存数据结构为JSON的List，起始时间暂时与之保持一致
        List<String> timeContent = new ArrayList<String>(1);
        timeContent.add(String.valueOf(time));
        MOVIE_DOWNLOAD_PATH_CACHE.put(KEY_AUTO_FETCH_START_TIME, timeContent);
    }

    /**
     * 获取下载路径解析引擎
     *
     * @return
     */
    public static List<DownloadPathFetchEngine> getEngines() {
        return ENGINES;
    }

    public static void initEngines() {
        InputStream enginesConfResource = Thread.currentThread().getContextClassLoader().getResourceAsStream("engine.conf");
        if (null != enginesConfResource) {
            Properties properties = new Properties();
            try {
                properties.load(enginesConfResource);
            } catch (IOException e) {
                e.printStackTrace();
            }

            for (Object key : properties.keySet()) {
                String engineClassName = (String) key;
                try {
                    Object engineInstance = Class.forName(engineClassName).newInstance();
                    if (engineInstance instanceof DownloadPathFetchEngine) {
                        ENGINES.add((DownloadPathFetchEngine) engineInstance);
                    }
                } catch (Exception e) {
                    logger.error("Init engine named " + engineClassName + " error", e);
                }
            }

            //对处理优先级进行排序
            ENGINES.sort((o1, o2) -> {
                        return properties.get(o1.getClass().getName()).toString().compareTo(properties.get(o2.getClass().getName()).toString());
                    }
            );
        }
    }
}

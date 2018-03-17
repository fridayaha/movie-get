package com.soft256.movie.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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

    private static String KEY_AUTO_FETCH_START_TIME = "startTime";

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
     * 判断电影名称是否相同
     *
     * @param movieName
     * @param movieTitle
     * @return
     */
    public static boolean nameMatch(String movieName, String movieTitle) {
        String fetchMovieName = movieTitle.substring(movieTitle.indexOf("《") + 1, movieTitle.indexOf("》")).trim();
        if (movieName.equals(fetchMovieName)) {
            return true;
        }
        if (movieName.replace("：", "").equals(fetchMovieName.replace(":", ""))) {
            return true;
        }
        return false;
    }
}

package com.soft256.movie.engine;

import java.util.List;

/**
 * Created by friday on 2018/3/17.
 */
public interface DownloadPathFetchEngine {
    List<String> fetchDownloadPath(String movieName);

    String getMainPage();


    /**
     * 获取最新上线电影列表
     */
    default void fetchNewestMovieList() {
    }
}

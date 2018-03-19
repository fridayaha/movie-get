package com.soft256.movie.controller;

import com.soft256.movie.engine.DownloadPathFetchEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by friday on 2018/3/18.
 * 定时任务相关
 */
@Component
public class ScheduledJobController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 每天凌晨一点执行更新任务
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void autoFetchNewestMovieList() {
        logger.info("Start fetch newest movie list at " + System.currentTimeMillis());
        List<DownloadPathFetchEngine> engines = MovieControllerUtil.getEngines();
        for (DownloadPathFetchEngine engine : engines) {
            //TODO:将所有获取的结果汇总去重
            engine.fetchNewestMovieList();
        }
        logger.info("End fetch newest movie list at " + System.currentTimeMillis());
    }
}

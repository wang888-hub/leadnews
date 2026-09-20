package com.aaliyun.leadnews.behavior.service;
import com.xxl.job.core.handler.annotation.XxlJob;import org.springframework.stereotype.Component;
@Component public class HotArticleRebuildJob {private final HotArticleRebuildService service;public HotArticleRebuildJob(HotArticleRebuildService s){service=s;}@XxlJob("hotArticleRebuildJob")public void execute(){service.rebuild();}}

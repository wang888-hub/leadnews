package com.aaliyun.leadnews.article.service;
import com.xxl.job.core.handler.annotation.XxlJob;import org.springframework.stereotype.Component;
@Component public class PublishCompensationJob{private final ArticlePublishService service;public PublishCompensationJob(ArticlePublishService s){service=s;}@XxlJob("articlePublishCompensation")public void compensate(){service.compensate();}}

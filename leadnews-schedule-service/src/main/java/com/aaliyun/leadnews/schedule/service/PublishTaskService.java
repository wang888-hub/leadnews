package com.aaliyun.leadnews.schedule.service;

import com.aaliyun.leadnews.feign.article.ArticleInternalClient;
import com.aaliyun.leadnews.model.foundation.ScheduleTaskCommand;
import com.aaliyun.leadnews.schedule.config.ScheduleProperties;
import com.aaliyun.leadnews.schedule.domain.ScheduleTask;
import com.aaliyun.leadnews.schedule.mapper.ScheduleTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

@Service
public class PublishTaskService {
    public static final String FUTURE_KEY = "schedule:publish:future";
    private static final Logger log = LoggerFactory.getLogger(PublishTaskService.class);

    private final ScheduleTaskMapper tasks;
    private final StringRedisTemplate redis;
    private final ArticleInternalClient article;
    private final ScheduleProperties properties;
    private final TaskExecutor executor;

    public PublishTaskService(ScheduleTaskMapper tasks,
                              StringRedisTemplate redis,
                              ArticleInternalClient article,
                              ScheduleProperties properties,
                              @Qualifier("publishTaskExecutor") TaskExecutor executor) {
        this.tasks = tasks;
        this.redis = redis;
        this.article = article;
        this.properties = properties;
        this.executor = executor;
    }

    @Transactional
    public Long create(ScheduleTaskCommand command) {
        ScheduleTask existing = tasks.selectOne(new LambdaQueryWrapper<ScheduleTask>()
                .eq(ScheduleTask::getTaskType, "ARTICLE_PUBLISH")
                .eq(ScheduleTask::getBusinessId, command.articleId())
                .eq(ScheduleTask::getDeleted, false));
        if (existing != null) {
            enqueueBestEffort(existing);
            return existing.getId();
        }

        ScheduleTask task = new ScheduleTask();
        task.setTaskType("ARTICLE_PUBLISH");
        task.setBusinessId(command.articleId());
        task.setExecuteTime(command.executeTime());
        task.setStatus("WAITING");
        task.setRetryCount(0);
        try {
            tasks.insert(task);
        } catch (DuplicateKeyException exception) {
            ScheduleTask raced = tasks.selectOne(new LambdaQueryWrapper<ScheduleTask>()
                    .eq(ScheduleTask::getTaskType, "ARTICLE_PUBLISH")
                    .eq(ScheduleTask::getBusinessId, command.articleId())
                    .eq(ScheduleTask::getDeleted, false));
            if (raced == null) {
                throw exception;
            }
            enqueueBestEffort(raced);
            return raced.getId();
        }
        enqueueBestEffort(task);
        return task.getId();
    }

    @PostConstruct
    public void restore() {
        recoverExpiredRunning();
        enqueueRecoverable(null);
    }

    @Scheduled(fixedDelayString = "${leadnews.schedule.scan-interval:PT1S}")
    public void scan() {
        long now = System.currentTimeMillis();
        Set<String> ids = redis.opsForZSet().rangeByScore(
                FUTURE_KEY, 0, now, 0, properties.scanBatchSize());
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (String value : ids) {
            long taskId = Long.parseLong(value);
            Long removed = redis.opsForZSet().remove(FUTURE_KEY, value);
            if (removed == null || removed != 1L) {
                continue;
            }
            submit(taskId);
        }
    }

    @Scheduled(fixedDelayString = "${leadnews.schedule.reconcile-interval:PT1M}")
    public void reconcile() {
        recoverExpiredRunning();
        // Tasks are normally inserted into ZSet at creation time. This bounded-horizon
        // reconciliation also repairs a Redis flush without scanning distant tasks forever.
        enqueueRecoverable(LocalDateTime.now().plus(properties.reconcileInterval()));
    }

    private void recoverExpiredRunning() {
        LocalDateTime stale = LocalDateTime.now().minus(properties.runningTimeout());
        tasks.update(null, new LambdaUpdateWrapper<ScheduleTask>()
                .eq(ScheduleTask::getStatus, "RUNNING")
                .lt(ScheduleTask::getStartedTime, stale)
                .lt(ScheduleTask::getRetryCount, properties.maxRetries())
                .set(ScheduleTask::getStatus, "WAITING")
                .set(ScheduleTask::getStartedTime, null)
                .set(ScheduleTask::getLastError, "RUNNING_LEASE_TIMEOUT"));
    }

    private void enqueueRecoverable(LocalDateTime dueBefore) {
        long cursor = 0;
        while (true) {
            LambdaQueryWrapper<ScheduleTask> query = new LambdaQueryWrapper<ScheduleTask>()
                    .gt(ScheduleTask::getId, cursor)
                    .in(ScheduleTask::getStatus, List.of("WAITING", "READY"))
                    .lt(ScheduleTask::getRetryCount, properties.maxRetries())
                    .eq(ScheduleTask::getDeleted, false)
                    .orderByAsc(ScheduleTask::getId)
                    .last("LIMIT " + properties.scanBatchSize());
            if (dueBefore != null) {
                query.le(ScheduleTask::getExecuteTime, dueBefore);
            }
            List<ScheduleTask> page = tasks.selectList(query);
            if (page.isEmpty()) {
                return;
            }
            page.forEach(this::enqueueBestEffort);
            cursor = page.getLast().getId();
            if (page.size() < properties.scanBatchSize()) {
                return;
            }
        }
    }

    void submit(long taskId) {
        try {
            executor.execute(() -> executeSafely(taskId));
        } catch (RuntimeException rejected) {
            log.warn("Publish executor rejected taskId={}, returning it to schedule queue", taskId);
            requeue(taskId);
        }
    }

    private void executeSafely(long taskId) {
        try {
            execute(taskId);
        } catch (RuntimeException unexpected) {
            log.error("Unexpected scheduled publish failure taskId={}", taskId, unexpected);
            requeue(taskId);
        }
    }

    public void execute(Long id) {
        ScheduleTask task = tasks.selectById(id);
        if (task == null || Set.of("SUCCESS", "CANCELLED").contains(task.getStatus())) {
            return;
        }
        if (task.getExecuteTime().isAfter(LocalDateTime.now())) {
            enqueueBestEffort(task);
            return;
        }

        int claimed = tasks.update(null, new LambdaUpdateWrapper<ScheduleTask>()
                .eq(ScheduleTask::getId, id)
                .in(ScheduleTask::getStatus, List.of("WAITING", "READY", "FAILED"))
                .lt(ScheduleTask::getRetryCount, properties.maxRetries())
                .set(ScheduleTask::getStatus, "RUNNING")
                .set(ScheduleTask::getStartedTime, LocalDateTime.now()));
        if (claimed != 1) {
            return;
        }

        try {
            article.publish(task.getBusinessId());
            tasks.update(null, new LambdaUpdateWrapper<ScheduleTask>()
                    .eq(ScheduleTask::getId, id)
                    .eq(ScheduleTask::getStatus, "RUNNING")
                    .set(ScheduleTask::getStatus, "SUCCESS")
                    .set(ScheduleTask::getStartedTime, null)
                    .set(ScheduleTask::getLastError, null));
        } catch (Exception exception) {
            ScheduleTask current = tasks.selectById(id);
            int retry = current.getRetryCount() + 1;
            String status = retry >= properties.maxRetries() ? "FAILED" : "WAITING";
            LocalDateTime next = LocalDateTime.now().plus(properties.retryDelay());
            tasks.update(null, new LambdaUpdateWrapper<ScheduleTask>()
                    .eq(ScheduleTask::getId, id)
                    .eq(ScheduleTask::getStatus, "RUNNING")
                    .set(ScheduleTask::getStatus, status)
                    .set(ScheduleTask::getRetryCount, retry)
                    .set(ScheduleTask::getExecuteTime, next)
                    .set(ScheduleTask::getStartedTime, null)
                    .set(ScheduleTask::getLastError, safe(exception)));
            if ("WAITING".equals(status)) {
                current.setExecuteTime(next);
                enqueueBestEffort(current);
            }
            log.warn("Scheduled article publish failed taskId={} retry={}", id, retry, exception);
        }
    }

    private void requeue(long taskId) {
        try {
            ScheduleTask task = tasks.selectById(taskId);
            if (task != null && Set.of("WAITING", "READY", "FAILED").contains(task.getStatus())
                    && task.getRetryCount() < properties.maxRetries()) {
                enqueue(task);
            }
        } catch (RuntimeException exception) {
            log.error("Unable to return publish task to Redis; reconciliation will recover taskId={}",
                    taskId, exception);
        }
    }

    private void enqueueBestEffort(ScheduleTask task) {
        try {
            enqueue(task);
        } catch (RuntimeException exception) {
            log.warn("Unable to enqueue publish task; MySQL reconciliation will retry taskId={}",
                    task.getId(), exception);
        }
    }

    private void enqueue(ScheduleTask task) {
        long score = task.getExecuteTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        redis.opsForZSet().add(FUTURE_KEY, String.valueOf(task.getId()), score);
    }

    private String safe(Exception exception) {
        String message = exception.getMessage() == null
                ? exception.getClass().getSimpleName() : exception.getMessage();
        return message.substring(0, Math.min(500, message.length()));
    }
}

package com.aaliyun.leadnews.feign.schedule;

import com.aaliyun.leadnews.model.foundation.ScheduleTaskCommand;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "leadnews-schedule-service", path = "/internal/schedule/tasks")
public interface ScheduleInternalClient {
    @PostMapping Long create(@Valid @RequestBody ScheduleTaskCommand command);
}

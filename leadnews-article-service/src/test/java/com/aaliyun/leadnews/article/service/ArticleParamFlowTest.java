package com.aaliyun.leadnews.article.service;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.aaliyun.leadnews.article.mapper.ArticleContentMapper;
import com.aaliyun.leadnews.article.mapper.ArticleMapper;
import com.aaliyun.leadnews.article.mapper.ChannelMapper;
import com.aaliyun.leadnews.common.exception.BusinessException;
import com.aaliyun.leadnews.common.sentinel.SentinelResources;
import com.aaliyun.leadnews.feign.behavior.BehaviorInternalClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ArticleParamFlowTest {
    @AfterEach void clearRules() { ParamFlowRuleManager.loadRules(List.of()); }

    @Test void articleDetailDeclaresFirstArgumentHotParamAndUnifiedBlockHandler() throws Exception {
        SentinelResource annotation = ArticleApplicationService.class.getMethod("detail", Long.class)
                .getAnnotation(SentinelResource.class);
        assertThat(annotation.value()).isEqualTo(SentinelResources.ARTICLE_DETAIL);
        assertThat(annotation.blockHandler()).isEqualTo("detailBlocked");

        var service = service();
        assertThatThrownBy(() -> service.detailBlocked(1L, mock(BlockException.class)))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getCode()).isEqualTo(42901));
    }

    @Test void differentArticleIdsHaveIndependentParamQuota() throws Exception {
        ParamFlowRule rule = new ParamFlowRule(SentinelResources.ARTICLE_DETAIL)
                .setParamIdx(0).setCount(1).setGrade(RuleConstant.FLOW_GRADE_QPS);
        ParamFlowRuleManager.loadRules(List.of(rule));

        try (Entry ignored = SphU.entry(SentinelResources.ARTICLE_DETAIL, EntryType.IN, 1, 1L)) { }
        assertThatThrownBy(() -> SphU.entry(SentinelResources.ARTICLE_DETAIL, EntryType.IN, 1, 1L))
                .isInstanceOf(BlockException.class);
        try (Entry ignored = SphU.entry(SentinelResources.ARTICLE_DETAIL, EntryType.IN, 1, 2L)) {
            assertThat(ignored).isNotNull();
        }
    }

    private ArticleApplicationService service() {
        return new ArticleApplicationService(mock(ArticleMapper.class), mock(ArticleContentMapper.class),
                mock(ChannelMapper.class), new ObjectMapper(), mock(BehaviorInternalClient.class), new SimpleMeterRegistry());
    }
}

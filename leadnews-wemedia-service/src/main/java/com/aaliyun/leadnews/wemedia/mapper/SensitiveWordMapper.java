package com.aaliyun.leadnews.wemedia.mapper;
import com.aaliyun.leadnews.wemedia.domain.SensitiveWord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import java.util.List;
public interface SensitiveWordMapper extends BaseMapper<SensitiveWord> {
 @Select("SELECT version FROM wm_sensitive_word_version WHERE id=1") long version();
 @Update("UPDATE wm_sensitive_word_version SET version=version+1 WHERE id=1") int incrementVersion();
 @Select("SELECT word FROM wm_sensitive_word WHERE enabled=1 ORDER BY id") List<String> enabledWords();
}

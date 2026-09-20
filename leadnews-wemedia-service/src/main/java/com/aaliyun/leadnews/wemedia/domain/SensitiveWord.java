package com.aaliyun.leadnews.wemedia.domain;
import com.baomidou.mybatisplus.annotation.*;
@TableName("wm_sensitive_word")
public class SensitiveWord {
 @TableId(type=IdType.AUTO) private Long id; private String word; private Boolean enabled;
 public Long getId(){return id;} public String getWord(){return word;} public void setWord(String v){word=v;}
 public Boolean getEnabled(){return enabled;} public void setEnabled(Boolean v){enabled=v;}
}

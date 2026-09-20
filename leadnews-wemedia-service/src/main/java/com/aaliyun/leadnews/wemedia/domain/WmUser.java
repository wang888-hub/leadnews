package com.aaliyun.leadnews.wemedia.domain;
import com.aaliyun.leadnews.common.persistence.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
@TableName("wm_user") public class WmUser extends BaseEntity {
 @TableId(type=IdType.AUTO) private Long id; @TableField("ap_user_id") private Long apUserId; private String name; @TableField("password_hash") private String passwordHash; private String nickname; private String phone; private String status;
 public Long getId(){return id;} public Long getApUserId(){return apUserId;} public String getName(){return name;} public String getPasswordHash(){return passwordHash;} public String getNickname(){return nickname;} public String getPhone(){return phone;} public String getStatus(){return status;}
}

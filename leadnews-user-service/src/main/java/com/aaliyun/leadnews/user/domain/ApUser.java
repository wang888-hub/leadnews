package com.aaliyun.leadnews.user.domain;

import com.aaliyun.leadnews.common.persistence.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;

@TableName("ap_user")
public class ApUser extends BaseEntity {
    @TableId(type=IdType.AUTO) private Long id;
    private String name; private String phone;
    @TableField("password_hash") private String passwordHash;
    private String image; private Integer sex; private Boolean certification;
    @TableField("identity_authenticated") private Boolean identityAuthenticated;
    private String status; private String flag;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
    public String getPasswordHash(){return passwordHash;} public void setPasswordHash(String v){passwordHash=v;}
    public String getImage(){return image;} public void setImage(String v){image=v;}
    public Integer getSex(){return sex;} public void setSex(Integer v){sex=v;}
    public Boolean getCertification(){return certification;} public void setCertification(Boolean v){certification=v;}
    public Boolean getIdentityAuthenticated(){return identityAuthenticated;} public void setIdentityAuthenticated(Boolean v){identityAuthenticated=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getFlag(){return flag;} public void setFlag(String v){flag=v;}
}

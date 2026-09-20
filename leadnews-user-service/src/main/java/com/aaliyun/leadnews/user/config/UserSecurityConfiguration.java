package com.aaliyun.leadnews.user.config;
import com.aaliyun.leadnews.common.persistence.AuditMetaObjectHandler;
import com.aaliyun.leadnews.common.security.JwtTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
@Configuration
public class UserSecurityConfiguration {
 @Bean JwtTokenService jwtTokenService(@Value("${JWT_SECRET}") String secret){return new JwtTokenService(secret);}
 @Bean BCryptPasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
 @Bean AuditMetaObjectHandler auditMetaObjectHandler(){return new AuditMetaObjectHandler();}
 @Bean MybatisPlusInterceptor mybatisPlusInterceptor(){MybatisPlusInterceptor i=new MybatisPlusInterceptor();i.addInnerInterceptor(new OptimisticLockerInnerInterceptor());return i;}
}

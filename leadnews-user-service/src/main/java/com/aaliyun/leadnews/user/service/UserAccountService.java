package com.aaliyun.leadnews.user.service;

import com.aaliyun.leadnews.common.api.CommonErrorCode;
import com.aaliyun.leadnews.common.context.UserContext;
import com.aaliyun.leadnews.common.exception.BusinessException;
import com.aaliyun.leadnews.common.security.JwtTokenService;
import com.aaliyun.leadnews.user.domain.ApUser;
import com.aaliyun.leadnews.user.mapper.ApUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class UserAccountService {
 private final ApUserMapper mapper; private final BCryptPasswordEncoder passwords; private final JwtTokenService jwt;
 public UserAccountService(ApUserMapper m,BCryptPasswordEncoder p,JwtTokenService j){mapper=m;passwords=p;jwt=j;}
 public LoginResult login(String phone,String password){
  ApUser u=mapper.selectOne(new LambdaQueryWrapper<ApUser>().eq(ApUser::getPhone,phone).eq(ApUser::getDeleted,false));
  if(u==null||!"ACTIVE".equals(u.getStatus())||!passwords.matches(password,u.getPasswordHash())) throw new BusinessException(CommonErrorCode.AUTH_UNAUTHORIZED);
  long expires=7200; return new LoginResult(jwt.createToken(u.getId(),"APP_USER",Duration.ofSeconds(expires)),expires,view(u));
 }
 public UserView current(){return view(requireCurrent());}
 public UserView update(String name,String image,Integer sex){ApUser u=requireCurrent();u.setName(name);u.setImage(image);u.setSex(sex);mapper.updateById(u);return view(u);}
 private ApUser requireCurrent(){ApUser u=mapper.selectById(UserContext.getUserId());if(u==null)throw new BusinessException(CommonErrorCode.AUTH_UNAUTHORIZED);return u;}
 private UserView view(ApUser u){return new UserView(u.getId(),u.getName(),u.getPhone(),u.getImage(),u.getSex(),u.getFlag());}
 public record LoginResult(String token,long expiresIn,UserView user){}
 public record UserView(Long id,String name,String phone,String image,Integer sex,String flag){}
}

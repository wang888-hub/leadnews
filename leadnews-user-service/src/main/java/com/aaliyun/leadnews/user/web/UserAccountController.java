package com.aaliyun.leadnews.user.web;
import com.aaliyun.leadnews.common.api.ResponseResult;
import com.aaliyun.leadnews.user.service.UserAccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/user")
public class UserAccountController {
 private final UserAccountService service; public UserAccountController(UserAccountService s){service=s;}
 @PostMapping("/login") public ResponseResult<UserAccountService.LoginResult> login(@Valid @RequestBody LoginRequest r){return ResponseResult.success(service.login(r.phone(),r.password()));}
 @GetMapping("/me") public ResponseResult<UserAccountService.UserView> me(){return ResponseResult.success(service.current());}
 @PutMapping("/me") public ResponseResult<UserAccountService.UserView> update(@Valid @RequestBody UpdateRequest r){return ResponseResult.success(service.update(r.name(),r.image(),r.sex()));}
 public record LoginRequest(@NotBlank @Pattern(regexp="\\d{11}") String phone,@NotBlank @Size(min=6,max=72) String password){}
 public record UpdateRequest(@NotBlank @Size(max=64) String name,@Size(max=500) String image,@Min(0) @Max(2) Integer sex){}
}

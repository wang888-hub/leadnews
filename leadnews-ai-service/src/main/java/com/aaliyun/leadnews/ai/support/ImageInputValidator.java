package com.aaliyun.leadnews.ai.support;
import com.aaliyun.leadnews.ai.config.AiProperties;import com.aaliyun.leadnews.ai.exception.AiExceptions;import com.aaliyun.leadnews.ai.model.AiImageInput;import org.springframework.stereotype.Component;import java.net.*;import java.util.Set;
@Component public class ImageInputValidator {
 private static final Set<String> TYPES=Set.of("image/jpeg","image/png","image/webp"); private final AiProperties p;
 public ImageInputValidator(AiProperties p){this.p=p;}
 public void validate(AiImageInput in){
  if(in==null||!TYPES.contains(in.mimeType()))throw new AiExceptions.InvalidImage("Unsupported image MIME type");
  boolean bytes=in.bytes()!=null&&in.bytes().length>0, url=in.url()!=null;if(bytes==url)throw new AiExceptions.InvalidImage("Provide exactly one image source");
  if(bytes&&in.bytes().length>p.maxImageBytes())throw new AiExceptions.InvalidImage("Image exceeds configured size limit");
  if(url){URI u=in.url();String host=u.getHost();if(!Set.of("http","https").contains(u.getScheme())||host==null||!p.allowedImageHosts().contains(host)||isPrivateUntrusted(host))throw new AiExceptions.InvalidImage("Image URL host is not allowed");}
 }
 private boolean isPrivateUntrusted(String host){try{InetAddress a=InetAddress.getByName(host);return (a.isAnyLocalAddress()||a.isLoopbackAddress()||a.isSiteLocalAddress())&&!p.allowedImageHosts().contains(host);}catch(Exception e){return true;}}
}

package com.aaliyun.leadnews.ai.support;
import org.springframework.core.io.ClassPathResource;import org.springframework.stereotype.Component;import java.io.IOException;import java.nio.charset.StandardCharsets;
@Component public class PromptCatalog {
 public String load(String name){try{return new ClassPathResource("prompts/"+name+".st").getContentAsString(StandardCharsets.UTF_8);}catch(IOException e){throw new IllegalStateException("Prompt resource unavailable: "+name,e);}}
}

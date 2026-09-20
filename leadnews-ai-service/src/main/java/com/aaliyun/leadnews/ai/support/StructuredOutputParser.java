package com.aaliyun.leadnews.ai.support;
import com.aaliyun.leadnews.ai.exception.AiExceptions;import jakarta.validation.Validator;import org.springframework.ai.converter.BeanOutputConverter;import org.springframework.stereotype.Component;
@Component public class StructuredOutputParser {
 private final Validator validator;public StructuredOutputParser(Validator validator){this.validator=validator;}
 public <T>T parse(String text,Class<T> type){try{T value=new BeanOutputConverter<>(type).convert(text);if(value==null||!validator.validate(value).isEmpty())throw new IllegalArgumentException();return value;}catch(Exception e){throw new AiExceptions.OutputParse();}}
}

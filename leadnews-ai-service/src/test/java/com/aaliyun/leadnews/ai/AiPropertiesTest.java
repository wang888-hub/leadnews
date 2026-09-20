package com.aaliyun.leadnews.ai;import jakarta.validation.Validation;import org.junit.jupiter.api.Test;import static org.assertj.core.api.Assertions.assertThat;
class AiPropertiesTest {@Test void validConfiguration(){try(var f=Validation.buildDefaultValidatorFactory()){assertThat(f.getValidator().validate(TestFixtures.properties(java.time.Duration.ofSeconds(1),1,2))).isEmpty();}}}

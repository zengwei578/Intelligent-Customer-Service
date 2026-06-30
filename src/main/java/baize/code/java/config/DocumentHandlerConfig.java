package baize.code.java.config;

import com.alibaba.cloud.ai.transformer.splitter.RecursiveCharacterTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DocumentHandlerConfig {

    @Bean
    public RecursiveCharacterTextSplitter textSplitter() {
        return new RecursiveCharacterTextSplitter(500);
    }
}

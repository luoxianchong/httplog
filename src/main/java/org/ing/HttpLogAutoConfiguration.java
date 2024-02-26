package org.ing;

import org.ing.filter.HttpLogFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author ing
 * @since 2024/2/24
 */
@Configuration
@EnableConfigurationProperties( HttpLogProperties.class)
public class HttpLogAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "http.log",value = "enable",matchIfMissing = true)
    public HttpLogFilter httpLogFilter(){
        return new HttpLogFilter();
    }
}

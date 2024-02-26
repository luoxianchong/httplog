package org.ing;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author ing
 * @since 2024/2/24
 */
@ConfigurationProperties(prefix = "http.log")
public class HttpLogProperties {

    private boolean enable;

    private String[] urlPatterns;

    private String[] excludeUrlPatterns;

    public String[] getExcludeUrlPatterns() {
        return excludeUrlPatterns;
    }

    public void setExcludeUrlPatterns(String[] excludeUrlPatterns) {
        this.excludeUrlPatterns = excludeUrlPatterns;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String[] getUrlPatterns() {
        return urlPatterns;
    }

    public void setUrlPatterns(String[] urlPatterns) {
        this.urlPatterns = urlPatterns;
    }



}

/*
 * HttpLogFilter.java
 * Copyright 2023 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package org.ing.filter;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.ing.HttpLogProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.annotation.Resource;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author ing
 * @since 2023/11/29
 */
@WebFilter(filterName = "httpLogFilter")
public class HttpLogFilter implements Filter {
    private final Logger                 log   = LoggerFactory.getLogger(HttpLogFilter.class);
    private final ThreadLocal<StopWatch> watch = ThreadLocal.withInitial(() -> new StopWatch());
    private static final AntPathMatcher PATH_MATCHER               = new AntPathMatcher();

    @Resource
    private HttpLogProperties properties;


    @Override
    public void init(FilterConfig filterConfig) {
        log.info("=====================HTTP请求日志过滤器初始化================");
    }

    @Override
    public void destroy() {
        log.info("================HTTP请求日志过滤器销毁================");
    }

    private void startRecord(ServletRequest request) {
        watch.get().reset();
        watch.get().start();
        MDC.put("traceid", (String) request.getAttribute("trace-id"));
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            startRecord(request);
            if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
                doLog((HttpServletRequest) request, (HttpServletResponse) response, chain);
            } else {
                chain.doFilter(request, response);
            }
        } finally {
            watch.remove();
        }
    }

    private void doLog(HttpServletRequest rawRequest, HttpServletResponse rawResponse, FilterChain chain) throws ServletException, IOException {
        ContentCachingRequestWrapper request = new ContentCachingRequestWrapper(rawRequest);
        ContentCachingResponseWrapper response = new ContentCachingResponseWrapper(rawResponse);
        chain.doFilter(request, response);
        try {
            watch.get().stop();
            String uri = request.getRequestURI();
            if (support(uri)) {
                String url = request.getMethod() + ":" + (StringUtils.isEmpty(request.getQueryString()) ? uri : (uri + "?" + request.getQueryString()));
                String payload = new String(request.getContentAsByteArray(), StandardCharsets.UTF_8).replaceAll("\\s+", "");
                String responseBody = contentToString(response.getContentInputStream(), StandardCharsets.UTF_8.name()).replaceAll("\\s+", "");
                log.info("HTTP>>> {}  ,payload:{}  ,response:{} ----------{}ms ", url, payload, responseBody, watch.get().getTime());
            }
        } catch (RuntimeException e) {
             log.warn("log filter error "+e.getMessage(),e);
        } finally {
            response.copyBodyToResponse();
        }
    }

    private boolean support(String uri){
        if(Objects.nonNull(properties.getExcludeUrlPatterns())){
            for (String excludeUrlPattern : properties.getExcludeUrlPatterns()) {
                if(PATH_MATCHER.match(excludeUrlPattern,uri)){
                    return false;
                }
            }
        }
        if(Objects.nonNull(properties.getUrlPatterns())){
            for (String urlPattern : properties.getUrlPatterns()) {
                if(PATH_MATCHER.match(urlPattern,uri)){
                    return true;
                }
            }
        }

        return false;
    }


    private String contentToString(InputStream input, String encoding) throws IOException {
        StringWriter sw = new StringWriter();
        if (encoding == null) {
            copyLarge(new InputStreamReader(input), sw);
        } else {
            copyLarge(new InputStreamReader(input, encoding), sw);
        }
        return sw.toString();
    }

    private void copyLarge(Reader input, Writer output) throws IOException {
        char[] buffer = new char[4096];
        long count;
        int n;
        for (count = 0L; -1 != (n = input.read(buffer)); count += n) {
            output.write(buffer, 0, n);
        }
    }

}

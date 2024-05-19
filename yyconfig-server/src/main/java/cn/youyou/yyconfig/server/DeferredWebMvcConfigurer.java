package cn.youyou.yyconfig.server;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
public class DeferredWebMvcConfigurer implements WebMvcConfigurer {

    @Bean
    public ThreadPoolTaskExecutor deferredTaskExecutor() {
        ThreadPoolTaskExecutor deferredExecutor = new ThreadPoolTaskExecutor();
        deferredExecutor.setCorePoolSize(10);
        deferredExecutor.setQueueCapacity(100);
        deferredExecutor.setMaxPoolSize(25);
        return deferredExecutor;
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(deferredTaskExecutor());
        // deferredResult 超时参数
        configurer.setDefaultTimeout(10000L);
    }

    @ControllerAdvice
    static class GlobalExceptionHandler {
        @ResponseStatus(HttpStatus.NOT_MODIFIED)
        @ResponseBody
        @ExceptionHandler(AsyncRequestTimeoutException.class) //捕获特定异常。这里捕获DeferredResult异步超时异常
        public void handleAsyncRequestTimeoutException(AsyncRequestTimeoutException e, HttpServletRequest request) {
            // 先简单处理了，后续再优化
            log.error("handleAsyncRequestTimeoutException");
        }
    }

}

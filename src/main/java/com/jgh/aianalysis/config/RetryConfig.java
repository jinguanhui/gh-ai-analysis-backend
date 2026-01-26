package com.jgh.aianalysis.config;

import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.jgh.aianalysis.exception.TimeOutException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

@Configuration
public class RetryConfig {

    @Bean
    public Retryer<Boolean> retryer() {
        return RetryerBuilder.<Boolean>newBuilder()
                .retryIfResult(Objects::isNull)
                .retryIfExceptionOfType(IOException.class)
                .retryIfExceptionOfType(RejectedExecutionException.class)
                .retryIfExceptionOfType(TimeOutException.class)
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .withWaitStrategy(WaitStrategies.fixedWait(10, TimeUnit.SECONDS))
                .withRetryListener(new MyRetryListener<>())
                .build();
    }

}

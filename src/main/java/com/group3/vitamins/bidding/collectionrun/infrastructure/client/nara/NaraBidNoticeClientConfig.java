package com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

// 나라장터 외부 API 호출에 필요한 HTTP Client를 구성합니다.
@Configuration
@EnableConfigurationProperties(NaraBidNoticeClientProperties.class)
public class NaraBidNoticeClientConfig {

    @Bean("naraWebClient")
    public WebClient naraWebClient(
            WebClient.Builder builder,
            NaraBidNoticeClientProperties properties
    ) {
        HttpClient httpClient = HttpClient.create()
                .option(
                        ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        Math.toIntExact(properties.connectTimeout().toMillis())
                )
                .doOnConnected(connection -> connection.addHandlerLast(
                        new ReadTimeoutHandler(
                                properties.readTimeout().toMillis(),
                                TimeUnit.MILLISECONDS
                        )
                ));

        return builder
                .baseUrl(properties.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
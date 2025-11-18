package com.dbsyncer.metadata.config;

import com.dbsyncer.connectors.client.KafkaConnectClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConnectClientConfiguration {

    @Bean(destroyMethod = "close")
    public KafkaConnectClient kafkaConnectClient(ConnectProperties props) {
        return new KafkaConnectClient(props.getRestUrl());
    }
}


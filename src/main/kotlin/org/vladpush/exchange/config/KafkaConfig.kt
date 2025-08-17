package org.vladpush.exchange.config

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
class KafkaConfig {

    @Value("\${exchange.kafka.topic.orders}")
    lateinit var ordersTopicName: String

    @Bean
    fun orderEventsTopic(): NewTopic {
        return TopicBuilder.name(ordersTopicName)
            .partitions(3)
            .replicas(1)
            .build()
    }
}
package io.xdag.txs.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class Producer {

    //    KafkaTemplate<String, String> kafkaTemplate;
    String topic;

    KafkaProducer<String, String> producer;

//    public Producer(KafkaTemplate<String, String> kafkaTemplate, String topic) {
//        this.kafkaTemplate = kafkaTemplate;
//        this.topic = topic;
//    }

    public Producer(KafkaProducer<String, String> producer, String topic) {
        this.producer = producer;
        this.topic = topic;
    }

    public void sendMsg(String k, String v) {
//        kafkaTemplate.send(new ProducerRecord<String, String>(topic, k, v));
        producer.send(new ProducerRecord<>(topic, k, v));
    }

}

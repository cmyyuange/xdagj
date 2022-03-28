package io.xdag.txs.kafka.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

public class KafkaConfiguration {

    //生产者配置
    private static Map<String, Object> senderProps() {
        Map<String, Object> props = new HashMap<>();
        //连接地址
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        //重试，0为不启用重试机制
        props.put(ProducerConfig.RETRIES_CONFIG, 1);
        //控制批处理大小，单位为字节
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        //批量发送，延迟为1毫秒，启用该功能能有效减少生产者发送消息次数，从而提高并发量
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        //生产者可以使用的总内存字节来缓冲等待发送到服务器的记录
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 1024000);
        //键的序列化方式
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        //值的序列化方式
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return props;
    }

    //根据senderProps填写的参数创建生产者工厂
//    public static ProducerFactory<String, String> producerFactory() {
//        return new DefaultKafkaProducerFactory<>(senderProps());
//    }

    public static KafkaProducer<String, String> getProducer() {
        return new KafkaProducer<String, String>(senderProps());
    }

    //kafkaTemplate实现了Kafka发送接收等功能
//    public static KafkaTemplate<String, String> kafkaTemplate() {
//        KafkaTemplate<String, String> template = new KafkaTemplate<String, String>(producerFactory());
//        return template;
//    }
}

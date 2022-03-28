package io.xdag.txs;

import static io.xdag.txs.constants.Constants.XDAG_TOPIC_BLOCK;

import io.xdag.txs.kafka.config.KafkaConfiguration;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

//public class KafkaTest {
//
//    @Test
//    public void testProducer() throws Exception {
//        Producer producer = new Producer(KafkaConfiguration.getProducer(), XDAG_TOPIC_BLOCK);
//        int partiton = 0;
//        try {
//            for (int i = 0; i < 10; i++) {
//                String key = null;
//                String data = "The msg is " + i;
//                // 注意填写您创建的topic名称。另外，生产消息的API有多个，具体参见Kafka官网或者下文的生产消息代码。
//                producer.sendMsg("helo", "wold");
//                System.out.println("produce msg:" + data);
//            }
//        } catch (Exception e) {
//            // TODO: 异常处理
//            e.printStackTrace();
//        }
//    }
//}

public class KafkaTest implements Runnable {

    private final KafkaProducer<String, String> producer;
    private final String topic;

    public KafkaTest(String topicName) {
//        Properties props = new Properties();
//        props.put("bootstrap.servers", "127.0.0.1:9092");
//        props.put("acks", "all");
//        props.put("retries", 0);
//        props.put("batch.size", 16384);
//        props.put("key.serializer", StringSerializer.class.getName());
//        props.put("value.serializer", StringSerializer.class.getName());
//        this.producer = new KafkaProducer<String, String>(props);
        this.producer = KafkaConfiguration.getProducer();
        this.topic = topicName;
    }

    public static void main(String args[]) {
        KafkaTest test = new KafkaTest(XDAG_TOPIC_BLOCK);
        Thread thread = new Thread(test);
        thread.start();
    }

    @Override
    public void run() {
        int messageNo = 2000;
        try {
            for (; ; ) {
                String messageStr = "123你好123123sdfsdf，这是第" + messageNo + "条数据";
                producer.send(new ProducerRecord<String, String>(topic, "Message", messageStr));
                //生产了100条就打印
                if (messageNo % 100 == 0) {
                    System.out.println("发送的信息:" + messageStr);
                }
                //生产1000条就退出
                if (messageNo % 3000 == 0) {
                    System.out.println("成功发送了" + messageNo + "条");
                    break;
                }
                messageNo++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            producer.close();
        }
    }
}

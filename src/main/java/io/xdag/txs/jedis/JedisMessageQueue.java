package io.xdag.txs.jedis;

import redis.clients.jedis.Jedis;

public class JedisMessageQueue implements IMessageQueue {

    private Publisher publisher;


    public JedisMessageQueue(Jedis publishJedis, String channel) {
        this.publisher = new Publisher(publishJedis, channel);
    }

    @Override
    public void putMessage(String message) {
        publisher.publishMessage(message);
    }

    @Override
    public String getMessage() {
        return null;
    }
}

package io.xdag.txs.jedis;

import redis.clients.jedis.Jedis;

public class Publisher {

    private Jedis publisherJedis;
    private String channel;

    public Publisher(Jedis publishJedis, String channel) {
        this.publisherJedis = publishJedis;
        this.channel = channel;
    }

    public void publishMessage(String message) {
        this.publisherJedis.publish(channel, message);
    }
}

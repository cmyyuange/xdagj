package io.xdag.txs.jedis;

import java.util.Set;
import redis.clients.jedis.Jedis;

public class PubClient {

    private Jedis jedis;
    private String CONSTANT_CLIENTSET = "clientSet";

    public PubClient(String host, int port) {
        jedis = new Jedis(host, port);
    }

    private void put(String message) {
        Set<String> subClients = jedis.smembers(CONSTANT_CLIENTSET);
        for (String clientKey : subClients) {
            jedis.rpush(clientKey, message);
        }
    }

    public void pub(String channel, String message) {
        Long txid = jedis.incr("MAXID");
        String content = txid + "/" + message;
        this.put(content);
        jedis.publish(channel, message);
    }

    public void close(String channel) {
        jedis.publish(channel, "quit");
        jedis.del(channel);
    }
}

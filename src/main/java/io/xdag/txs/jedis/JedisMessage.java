package io.xdag.txs.jedis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class JedisMessage {

    public static final String CHANNEL = "mychannel";
    public static final String HOST = "127.0.0.1";
    public static final int PORT = 6379;
    private static final Logger logger = LoggerFactory.getLogger(JedisMessage.class);
    private final static JedisPoolConfig POOL_CONFIG = new JedisPoolConfig();
    private final static JedisPool JEDIS_POOL = new JedisPool(POOL_CONFIG, HOST, PORT, 0);

    public static void main(String[] args) {
        try {
            final Jedis publisherJedis = JEDIS_POOL.getResource();
            JedisMessageQueue redisMessageQueue = new JedisMessageQueue(publisherJedis, CHANNEL);
            redisMessageQueue.putMessage("hhello");
        } catch (JedisConnectionException e) {
            logger.debug("Redis server not found");
        }
    }

}

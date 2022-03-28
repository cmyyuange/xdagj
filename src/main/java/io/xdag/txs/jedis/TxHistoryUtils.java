//package io.xdag.txs;
//
//import com.google.gson.FieldNamingPolicy;
//import com.google.gson.Gson;
//import com.google.gson.GsonBuilder;
//import io.xdag.config.Constants.MessageType;
//import io.xdag.core.Address;
//import io.xdag.core.Block;
//import io.xdag.core.XdagBlock;
//import io.xdag.listener.Listener;
//import io.xdag.listener.Message;
//import java.math.BigInteger;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.stream.Collectors;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import redis.clients.jedis.Jedis;
//import redis.clients.jedis.JedisPool;
//import redis.clients.jedis.JedisPoolConfig;
//import redis.clients.jedis.exceptions.JedisConnectionException;
//
//public class TxHistoryUtils implements Listener {
//
//
//    public static final String CHANNEL = "txHistory";
//    public static final String HOST = "127.0.0.1";
//    public static final int PORT = 6379;
//    private static final Logger logger = LoggerFactory.getLogger(JedisMessage.class);
//    private final static JedisPoolConfig POOL_CONFIG = new JedisPoolConfig();
//    private final static JedisPool JEDIS_POOL = new JedisPool(POOL_CONFIG, HOST, PORT, 0);
//
//    private static final Gson gson = new GsonBuilder()
//            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
//
//    private IMessageQueue messageQueue;
//
//
//    public TxHistoryUtils() {
//        try {
//            final Jedis publisherJedis = JEDIS_POOL.getResource();
//            messageQueue = new JedisMessageQueue(publisherJedis, CHANNEL);
//        } catch (JedisConnectionException e) {
//            logger.debug("Redis server not found");
//        }
//    }
//
//
//    @Override
//    public void onMessage(Message message) {
//
//        if (!type.equals(MessageType.NEW_LINK)) {
//            return;
//        }
//
//        Block block = new Block(new XdagBlock(message.getData().toArray()));
//
//        List<Address> inputs = block.getInputs().stream()
//                .filter(input -> input.getAmount().compareTo(BigInteger.ZERO) > 0).collect(Collectors.toList());
//
//        List<Address> outputs = block.getOutputs().stream()
//                .filter(output -> output.getAmount().compareTo(BigInteger.ZERO) > 0).collect(Collectors.toList());
//
//        if (inputs.size() == 0 || outputs.size() == 0) {
//            return;
//        }
//
//        String txHash = block.getHash().toUnprefixedHexString();
//        List<String> froms = new ArrayList<>();
//        List<String> tos = new ArrayList<>();
//        for (Address input : inputs) {
//            froms.add(input.getHashLow().toUnprefixedHexString());
//        }
//        for (Address output : outputs) {
//            tos.add(output.getHashLow().toUnprefixedHexString());
//        }
//        String data = gson.toJson(new TxMessage(txHash, froms, tos));
//
//        messageQueue.putMessage(data);
//    }
//}

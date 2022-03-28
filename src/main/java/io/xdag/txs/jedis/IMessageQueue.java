package io.xdag.txs.jedis;

public interface IMessageQueue {

    void putMessage(String message);

    String getMessage();
}

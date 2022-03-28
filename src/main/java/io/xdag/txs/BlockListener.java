package io.xdag.txs;

import static io.xdag.config.Constants.MessageType.NEW_LINK;
import static io.xdag.config.Constants.MessageType.PRE_TOP;
import static io.xdag.txs.constants.Constants.NEWBLOCK;
import static io.xdag.txs.constants.Constants.UPDATEBLOCK;
import static io.xdag.txs.constants.Constants.XDAG_TOPIC_BLOCK;

import com.google.gson.Gson;
import io.xdag.core.Block;
import io.xdag.core.XdagBlock;
import io.xdag.listener.BlockMessage;
import io.xdag.listener.Listener;
import io.xdag.listener.Message;
import io.xdag.listener.UpdateBlockMessage;
import io.xdag.txs.components.BlockInfo;
import io.xdag.txs.components.UpdateBlockInfo;
import io.xdag.txs.kafka.Producer;
import io.xdag.txs.kafka.config.KafkaConfiguration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class BlockListener implements Listener, Runnable {

    private static final Gson gson = new Gson();
    private final LinkedBlockingQueue<Message> queue = new LinkedBlockingQueue<>();
    private final Producer producer;
    private boolean isRunning = false;


    public BlockListener() {
        this.producer = new Producer(KafkaConfiguration.getProducer(), XDAG_TOPIC_BLOCK);
    }

    public BlockListener(Producer producer) {
        this.producer = producer;
    }

    public void start() {
        if (!this.isRunning) {
            this.isRunning = true;
            new Thread(this, "xdag-producer").start();
        }
    }


    @Override
    public void onMessage(Message message) {
        if (message instanceof BlockMessage) {
            if (message.getType().equals(PRE_TOP)) {
                return;
            }
        }
        queue.offer(message);
    }

    @Override
    public void run() {
        this.isRunning = true;
        while (this.isRunning) {
            try {
                Message message = queue.poll(50, TimeUnit.MILLISECONDS);
                if (message instanceof UpdateBlockMessage) {
                    processUpdateMessage(message);
                } else if (message instanceof BlockMessage) {
                    processNewBlockMessage(message);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    void processUpdateMessage(Message message) {
        UpdateBlockInfo updateBlockInfo = getUpdateInfoFromMessage((UpdateBlockMessage) message);
        String value = gson.toJson(updateBlockInfo);
        producer.sendMsg(UPDATEBLOCK, value);
    }

    private UpdateBlockInfo getUpdateInfoFromMessage(UpdateBlockMessage message) {
        return message.getData();
    }

    private BlockInfo getBlockInfoFromMessage(BlockMessage message) {
        Block block = new Block(new XdagBlock(message.getData().toArray()));
        BlockInfo blockInfo = BlockInfo.transferBlockInfo(block);
        return blockInfo;
    }

    public void stop() {
        if (this.isRunning) {
            this.isRunning = false;
        }
    }

    void processNewBlockMessage(Message message) {
        if (message.getType().equals(NEW_LINK)) {
            BlockInfo blockInfo = getBlockInfoFromMessage((BlockMessage) message);
            String value = gson.toJson(blockInfo);
            producer.sendMsg(NEWBLOCK, value);
        }
    }
}

package io.xdag.txs.constants;

public interface Constants {

    // kafka topics
    String XDAG_TOPIC_BLOCK = "xdag_topics_block";
    //    String XDAG_TOPIC_BLOCK = "test";
    String XDAG_TOPIC_NETWORK = "xdag_topics_network";

    // kafka keys for xdagj
    String NEWBLOCK = "new_block";
    String UPDATEBLOCK = "update_block";
}

package io.xdag.txs.components;

import lombok.Data;

@Data
public class LinkBlockInfo {

    String hash; //block hash
    int type; // 0 fee 1 input 2 output
    long amount; // amount

}

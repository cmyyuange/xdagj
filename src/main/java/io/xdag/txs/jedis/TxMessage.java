package io.xdag.txs.jedis;

import java.util.List;
import lombok.Data;


@Data
public class TxMessage {

    private String txHash;
    private List<String> froms;
    private List<String> tos;

    public TxMessage(String txHash, List<String> froms, List<String> tos) {
        this.txHash = txHash;
        this.froms = froms;
        this.tos = tos;
    }

}

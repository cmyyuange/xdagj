package io.xdag.txs.components;

import static io.xdag.cli.Commands.getStateByFlags;

import io.xdag.config.BlockState;
import io.xdag.config.BlockType;
import io.xdag.core.BlockInfo;
import lombok.Data;
import org.apache.tuweni.bytes.Bytes32;

@Data
public class UpdateBlockInfo { // record all update properties

    String hash;
    String state; // block state 0 main 1 reject 2 accept 3 pending
    long height; // block height
    long balance; // block balance
    int type; // block type  0. mainblock  1. wallet 2. transaction


    String refHash; // who ref me


    /**
     * get update properties from blockinfo
     *
     * @param blockInfo
     * @return
     */
    public static UpdateBlockInfo transferBlockInfo(BlockInfo blockInfo) {
        UpdateBlockInfo info = new UpdateBlockInfo();

        info.setHash(Bytes32.wrap(blockInfo.getHash()).toUnprefixedHexString());
        info.setState(getStateByFlags(blockInfo.flags));
        info.setHeight(blockInfo.getHeight());
        info.setBalance(blockInfo.getAmount());
        if (info.getState().equals(BlockState.MAIN.getDesc())) {
            info.setType(BlockType.MAIN.getCode()); //block type wallet main transaction
        }
        String refHash = blockInfo.getRef() == null ? "" : Bytes32.wrap(blockInfo.getRef()).toUnprefixedHexString();
        info.setRefHash(refHash);
        return info;
    }
}
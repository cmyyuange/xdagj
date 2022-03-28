package io.xdag.txs.components;

import static io.xdag.cli.Commands.getStateByFlags;
import static io.xdag.utils.BasicUtils.hash2Address;

import cn.hutool.core.collection.CollectionUtil;
import io.xdag.config.BlockType;
import io.xdag.core.Address;
import io.xdag.core.Block;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class BlockInfo { // record init properties

    String hash; // block hash
    String address; // block address
    String state; // block state 0 main 1 reject 2 accept 3 pending
    long height; // block height
    long time; // block create Time
    long amount; // block balance
    BigInteger diff; // block difficulty
    String remark; // block remark
    int type; // block type  0. wallet address 1. mainblock 2. transaction
    List<LinkBlockInfo> links;

    /**
     * get initial properties from a new block
     *
     * @param block
     * @return
     */
    public static BlockInfo transferBlockInfo(Block block) {
        BlockInfo blockInfo = new BlockInfo();
        blockInfo.setHash(block.getHash().toUnprefixedHexString());
        blockInfo.setAddress(hash2Address(block.getHash()));
        blockInfo.setState(getStateByFlags(block.getInfo().getFlags()));
        blockInfo.setHeight(block.getInfo().getHeight());
        blockInfo.setTime(block.getTimestamp());
        blockInfo.setAmount(block.getInfo().getAmount());
        blockInfo.setDiff(block.getInfo().getDifficulty());
        String remark = block.getInfo().getRemark() == null ? "" : new String(block.getInfo().getRemark());
        blockInfo.setRemark(remark);
        blockInfo.setLinks(processLink(block));
        blockInfo.setType(processType(block));
        return blockInfo;
    }

    /**
     * get links address from a new block
     *
     * @param block
     * @return
     */
    private static List<LinkBlockInfo> processLink(Block block) {
        List<LinkBlockInfo> res = new ArrayList<>();
        List<Address> inputs = block.getInputs();
        List<Address> outputs = block.getOutputs();
        if (!CollectionUtil.isEmpty(inputs)) {
            for (Address input : inputs) {
                LinkBlockInfo linkBlockInfo = new LinkBlockInfo();
                linkBlockInfo.setAmount(input.getAmount().longValue());
                linkBlockInfo.setHash(input.getHashLow().toUnprefixedHexString());
                linkBlockInfo.setType(1);
                res.add(linkBlockInfo);
            }
        }

        if (!CollectionUtil.isEmpty(outputs)) {
            for (Address output : outputs) {
                LinkBlockInfo linkBlockInfo = new LinkBlockInfo();
                linkBlockInfo.setAmount(output.getAmount().longValue());
                linkBlockInfo.setHash(output.getHashLow().toUnprefixedHexString());
                linkBlockInfo.setType(2);
                res.add(linkBlockInfo);
            }
        }

        return res;
    }

    /**
     * judge a block is a wallet or a transaction
     *
     * @param block
     * @return
     */
    private static int processType(Block block) {
        // init wallet type
        int type = BlockType.WALLET.getCode();
        // if block have in-sigs means it is a transaction
        if (!CollectionUtil.isEmpty(block.getInsigs())) {
            type = BlockType.TRANSACTION.getCode();
        }
        return type;
    }

}
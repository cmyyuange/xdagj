package io.xdag.listener;


import io.xdag.config.Constants.MessageType;
import io.xdag.core.BlockInfo;
import io.xdag.txs.components.UpdateBlockInfo;

public class UpdateBlockMessage implements Message {

    UpdateBlockInfo updateInfo;
    MessageType type;
    BlockInfo blockInfo;

    public UpdateBlockMessage(BlockInfo blockInfo, MessageType type) {
//        this.updateInfo = UpdateBlockInfo.transferBlockInfo(blockInfo);
        this.type = type;
        this.blockInfo = blockInfo;
    }

    @Override
    public MessageType getType() {
        return type;
    }

    @Override
    public UpdateBlockInfo getData() {
        if (updateInfo == null) {
            this.updateInfo = UpdateBlockInfo.transferBlockInfo(blockInfo);
        }
        return updateInfo;
    }
}

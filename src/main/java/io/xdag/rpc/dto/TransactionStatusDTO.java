package io.xdag.rpc.dto;

import lombok.Data;

@Data
public class TransactionStatusDTO {
    // 交易状态
    private String status;

    public TransactionStatusDTO(String status) {
        this.status = status;
    }
}

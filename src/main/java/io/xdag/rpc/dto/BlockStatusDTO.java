package io.xdag.rpc.dto;


import lombok.Data;

@Data
public class BlockStatusDTO {
    private String hash;
    private String status;

    public BlockStatusDTO(String hash,String status) {
        this.hash = hash;
        this.status = status;
    }
}

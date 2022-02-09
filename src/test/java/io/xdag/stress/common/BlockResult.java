package io.xdag.stress.common;

import io.xdag.core.ImportResult;
import lombok.Data;

@Data
public class BlockResult {

    private String jsonrpc;
    private int id;
    private Result result;


    public class Result {

        private String hash;
        private String status;

        public String getStatus() {
            return this.status;
        }
    }

}

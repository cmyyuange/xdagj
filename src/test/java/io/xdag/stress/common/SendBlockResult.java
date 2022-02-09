package io.xdag.stress.common;

import io.xdag.core.ImportResult;
import lombok.Data;

@Data
public class SendBlockResult {

    private String jsonrpc;
    private int id;
    private Result result;

    public SendBlockResult(){

    }

    public class Result {
        private ImportResult status;

        public Result() {

        }

        public ImportResult getStatus() {
            return this.status;
        }
    }
}

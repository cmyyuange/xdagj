package io.xdag.stress;

import static io.xdag.core.ImportResult.EXIST;
import static io.xdag.core.ImportResult.IMPORTED_BEST;
import static io.xdag.core.ImportResult.IMPORTED_NOT_BEST;

import cn.hutool.json.JSONObject;
import com.google.gson.Gson;
import io.vertx.core.json.JsonObject;
import io.xdag.config.Config;
import io.xdag.config.DevnetConfig;
import io.xdag.core.Block;
import io.xdag.core.ImportResult;
import io.xdag.stress.common.BlockResult;
import io.xdag.stress.common.JsonCall;
import io.xdag.stress.common.SendBlockResult;
import io.xdag.utils.XdagTime;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.MediaType;
import org.apache.http.entity.StringEntity;
import org.apache.tuweni.crypto.SECP256K1;
import org.jupnp.model.message.header.ServiceUSNHeader;

public class Main {

    private static final String url = "http://127.0.0.1:4444";
    private static BigInteger private_1 = new BigInteger("c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4", 16);
    private static SECP256K1.SecretKey secretkey_1 = SECP256K1.SecretKey.fromInteger(private_1);
    private static final Config config = new DevnetConfig();


    public static void main(String[] args) {

        int num = 100;

        int sendSuccess = 0;
        int sendSuccessBecomeMain = 0;
        int onChainAccepted = 0;
        int onChainRejected = 0;
        int onChainMain = 0;

        List<Block> blocks = new ArrayList<>();


        for (int i = 0; i < num;i++) {
            // 创建区块
            SECP256K1.KeyPair addrKey = SECP256K1.KeyPair.fromSecretKey(secretkey_1);
            long xdagTime = System.currentTimeMillis()+i*10;
            Block b = new Block(config, xdagTime, null, null, false, null, null, -1);
            b.signOut(addrKey);
            blocks.add(b);
        }


        for (int i = 0; i< num;i++) {
            // 发起请求
            JsonCall jsonCall = new JsonCall();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "xdag_sendRawTransaction");
            jsonObject.put("jsonrpc", "2.0");
            jsonObject.put("id", 1);
            List<String> list = new ArrayList<>();
            list.add(blocks.get(i).getXdagBlock().getData().toUnprefixedHexString());
            jsonObject.put("params", list);
            String res = jsonCall.post(url, jsonObject.toString());

            Gson gson=new Gson();
            SendBlockResult result = gson.fromJson(res, SendBlockResult.class);
            if(result.getResult().getStatus().equals(IMPORTED_NOT_BEST)) {
                sendSuccess++;
            } else if (result.getResult().getStatus().equals(IMPORTED_BEST)) {
                sendSuccessBecomeMain++;
            }

        }

        System.out.printf("全部发送完成，%d个发送成功，开始检查是否上链...\n",sendSuccess+sendSuccessBecomeMain);
        // 定时查询区块是否加入
        long ms = 64*1000;
        long startTime = System.currentTimeMillis();
        Date date = new Date(startTime);
        System.out.println("开始检查区块状态:"+date);
        long currentTime = System.currentTimeMillis();
        while (true) {
            currentTime = System.currentTimeMillis();
            if (currentTime % ms == 0) {
                for (int i = 0; i < num; i++) {
                    // 发起请求
                    JsonCall jsonCall = new JsonCall();
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("method", "xdag_getBlockStatus");
                    jsonObject.put("jsonrpc", "2.0");
                    jsonObject.put("id", 1);
                    List<String> list = new ArrayList<>();
                    list.add(blocks.get(i).getHashLow().toUnprefixedHexString());
                    jsonObject.put("params", list);
                    String res = jsonCall.post(url, jsonObject.toString());
                    Gson gson = new Gson();
                    BlockResult result = gson.fromJson(res, BlockResult.class);
                    String status = result.getResult().getStatus();
                    switch (status) {
                        case "Accepted" -> onChainAccepted++;
                        case "Rejected" -> onChainRejected++;
                        case "Main" -> onChainMain++;
                    }
                }
            }
            if (onChainAccepted+onChainRejected+onChainMain == num) {
                break;
            }
        }
        System.out.println("结束:"+ new Date(System.currentTimeMillis()));
        System.out.printf("%d个区块成功上链,%d个区块被拒绝,%d个区块成为主块\n",onChainAccepted,onChainRejected,onChainMain);
    }
}

package io.xdag.stress;
import static io.xdag.core.ImportResult.EXIST;
import static io.xdag.core.ImportResult.IMPORTED_BEST;
import static io.xdag.core.ImportResult.IMPORTED_NOT_BEST;

import cn.hutool.json.JSONObject;
import com.google.gson.Gson;
import io.xdag.config.Config;
import io.xdag.config.DevnetConfig;
import io.xdag.core.Block;
import io.xdag.stress.common.BlockResult;
import io.xdag.stress.common.JsonCall;
import io.xdag.stress.common.ProgressBar;
import io.xdag.stress.common.SendBlockResult;
import java.math.BigInteger;
import java.security.Security;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import jnr.a64asm.SYSREG_CODE;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.crypto.SECP256K1;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class Main4 {
    static { Security.addProvider(new BouncyCastleProvider());  }
    private static final String url = "http://127.0.0.1:4444";
    private static BigInteger private_1 = new BigInteger("c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4", 16);
    private static SECP256K1.SecretKey secretkey_1 = SECP256K1.SecretKey.fromInteger(private_1);
    private static final Config config = new DevnetConfig();


    public static void main(String[] args) {
        int num = 400000;

        ProgressBar generateBar = new ProgressBar(num,30);
        ProgressBar searchBar = new ProgressBar(num,30);

        int sendSuccess = 0;
        int sendSuccessBecomeMain = 0;
        int sendSuccessExist = 0;
        int onChainAccepted = 0;
        int onChainRejected = 0;
        int onChainMain = 0;

        List<Bytes32> blocks = new ArrayList<>();
        Map<Bytes32,Long> map = new HashMap<>();


        // 1. 创建区块并发送
        System.out.print(new Date(System.currentTimeMillis()));
        System.out.printf(" 创建并发送%d个区块...\n",num);
        for (int i = 0; i < num;i++) {

            // 1.1 创建区块
            SECP256K1.KeyPair addrKey = SECP256K1.KeyPair.fromSecretKey(secretkey_1);
            long xdagTime = System.currentTimeMillis()+i;
            Block b = new Block(config, xdagTime, null, null, false, null, null, -1);
            b.signOut(addrKey);
            blocks.add(b.getHashLow());
            // 1.2 发送区块
            JsonCall jsonCall = new JsonCall();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "xdag_sendRawTransaction");
            jsonObject.put("jsonrpc", "2.0");
            jsonObject.put("id", 1);
            List<String> list = new ArrayList<>();
            list.add(b.getXdagBlock().getData().toUnprefixedHexString());
            jsonObject.put("params", list);
            // 1.3 rpc发送
            jsonCall.postASync(url, jsonObject.toString());
            generateBar.showBarByPoint("创建发送中:",i+1);
        }
        System.out.println();
        System.out.print(new Date(System.currentTimeMillis()));
        System.out.println(" 全部发送完成，开始检查是否上链...");
        // 3. 定时查询区块是否加入
        long ms = 64*1000;
        long currentTime = System.currentTimeMillis();
        do {
            currentTime = System.currentTimeMillis();
            if (currentTime % ms == 0) {
                onChainAccepted = 0;
                onChainRejected = 0;
                onChainMain = 0;
                for (int i = 0; i < num; i++) {
                    searchBar.showBarByPoint("查询中:",i+1);
                    // 发起请求
                    JsonCall jsonCall = new JsonCall();
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("method", "xdag_getBlockStatus");
                    jsonObject.put("jsonrpc", "2.0");
                    jsonObject.put("id", 1);
                    List<String> list = new ArrayList<>();
                    list.add(blocks.get(i).toUnprefixedHexString());
                    jsonObject.put("params", list);
                    String res = jsonCall.post(url, jsonObject.toString());
                    if( res != null) {
//                    System.out.println(res);
                        Gson gson = new Gson();
                        BlockResult result = gson.fromJson(res, BlockResult.class);
                        String status = result.getResult().getStatus();
                        switch (status) {
                            case "Accepted" -> onChainAccepted++;
                            case "Rejected" -> onChainRejected++;
                            case "Main" -> onChainMain++;
                        }

//                        if (status.equals("Accepted") || status.equals("Rejected") || status.equals("Main")) {
//                            map.put(blocks.get(i),System.currentTimeMillis()-map.get(blocks.get(i)));
//                        }
                    }
                }
                System.out.printf("\n剩余 %d个 pending状态\n",num - (onChainAccepted + onChainRejected + onChainMain));
            }
        } while (onChainAccepted + onChainRejected + onChainMain != num);
        System.out.print(""+ new Date(System.currentTimeMillis()));
        System.out.printf(" %d个区块成功上链,%d个区块被拒绝,%d个区块成为主块\n",onChainAccepted,onChainRejected,onChainMain);
//        while (map.entrySet().iterator().hasNext()) {
//            Entry<Bytes32,Long> entry = map.entrySet().iterator().next();
//            System.out.println("耗时 ："+entry.getValue()/1000+"s");
//        }
    }
}


package io.xdag.stress;

import cn.hutool.json.JSONObject;
import com.google.gson.Gson;
import io.xdag.config.Config;
import io.xdag.config.DevnetConfig;
import io.xdag.core.Block;
import io.xdag.core.XdagBlock;
import io.xdag.stress.common.BlockResult;
import io.xdag.stress.common.JsonCall;
import io.xdag.stress.common.ProgressBar;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.security.Security;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.tuweni.crypto.SECP256K1;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Test;

public class StressTestByLoadBlocks {
    static { Security.addProvider(new BouncyCastleProvider());  }
    private static final String url = "http://127.0.0.1:4444";
    private static BigInteger private_1 = new BigInteger("c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4", 16);
    private static SECP256K1.SecretKey secretkey_1 = SECP256K1.SecretKey.fromInteger(private_1);
    private static final Config config = new DevnetConfig();

    private static final String fileName = "block.data";


//    @Test
    public void stressTest() throws IOException, ClassNotFoundException {
        System.out.print(new Date(System.currentTimeMillis()));
        System.out.println(" 开始加载区块");

        InputStream freader = StressTestByLoadBlocks.class.getClassLoader().getResourceAsStream(fileName);

        ObjectInputStream objectInputStream = new ObjectInputStream(freader);

        List<byte[]> blocks1 = new ArrayList<>();
        blocks1 = (List<byte[]>) objectInputStream.readObject();

        objectInputStream.close();

        List<Block> blocks = new ArrayList<>();
        for (byte[] data:blocks1) {
            Block block = new Block(new XdagBlock(data));
            blocks.add(block);
        }
        int num = blocks1.size();

        Block lastBlock = blocks.get(num-1);

        // 1. 创建区块
        System.out.print(new Date(System.currentTimeMillis()));
        System.out.printf(" 加载完成%d个区块\n",num);


        // 2. 发起请求。
        for (int i = 0; i< num;i++) {
            JsonCall jsonCall = new JsonCall();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "xdag_sendRawTransaction");
            jsonObject.put("jsonrpc", "2.0");
            jsonObject.put("id", 1);
            List<String> list = new ArrayList<>();
            list.add(blocks.get(i).getXdagBlock().getData().toUnprefixedHexString());
            jsonObject.put("params", list);
//            String res = jsonCall.post(url, jsonObject.toString());
//
//            Gson gson=new Gson();
//            SendBlockResult result = gson.fromJson(res, SendBlockResult.class);
//            if(result.getResult().getStatus().equals(IMPORTED_NOT_BEST)) {
//                sendSuccess++;
//            } else if (result.getResult().getStatus().equals(IMPORTED_BEST)) {
//                sendSuccessBecomeMain++;
//            }else if (result.getResult().getStatus().equals(EXIST)) {
//                sendSuccessExist ++;
//            }
////            map.put(blocks.get(i).getHashLow(),System.currentTimeMillis());
//            sendBar.showBarByPoint("发送区块中:",i+1);
            jsonCall.postASync(url, jsonObject.toString());
        }

        System.out.print(new Date(System.currentTimeMillis()));
        System.out.println(" 全部发送完成，开始检查是否上链...");
        // 3. 查询区块是否加入
        long ms = 60*1000*2;
        boolean returnRes = false;
        do {
            // 每隔2分钟
            if (System.currentTimeMillis() % ms == 0) {
                // 发起请求
                JsonCall jsonCall = new JsonCall();
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("method", "xdag_getBlockStatus");
                jsonObject.put("jsonrpc", "2.0");
                jsonObject.put("id", 1);
                List<String> list = new ArrayList<>();
                list.add(lastBlock.getHashLow().toUnprefixedHexString());
                jsonObject.put("params", list);
                String res = jsonCall.post(url, jsonObject.toString());
                if( res != null) {
                    Gson gson = new Gson();
                    BlockResult result = gson.fromJson(res, BlockResult.class);
                    if (result!=null) {
                        if (result.getResult()!=null) {
                            String status = result.getResult().getStatus();
                            if (status.equals("Accepted") || status.equals("Rejected") || status.equals("Main")) {
                                returnRes = true;
                            }
                        }
                    }
                }
            }
        } while (!returnRes);

        System.out.println("最后一个区块打包完成 "+new Date(System.currentTimeMillis()));
    }
}
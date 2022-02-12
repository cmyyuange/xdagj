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
import java.io.IOException;
import java.math.BigInteger;
import java.security.Security;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import okhttp3.Response;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.crypto.SECP256K1;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class Main5 {
    static { Security.addProvider(new BouncyCastleProvider());  }
    private static final String url = "http://127.0.0.1:4444";
    private static BigInteger private_1 = new BigInteger("c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4", 16);
    private static SECP256K1.SecretKey secretkey_1 = SECP256K1.SecretKey.fromInteger(private_1);
    private static final Config config = new DevnetConfig();
    private static Gson gson = new Gson();

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        int num = 200000;
        ProgressBar generateBar = new ProgressBar(num,30);
        ProgressBar sendBar = new ProgressBar(num,30);
        ProgressBar searchBar = new ProgressBar(num,30);

        int sendSuccess = 0;
        int sendSuccessBecomeMain = 0;
        int sendSuccessExist = 0;
        int onChainAccepted = 0;
        int onChainRejected = 0;
        int onChainMain = 0;

        List<Block> blocks = new ArrayList<>();
        Map<Bytes32,Long> map = new HashMap<>();


        // 1. 创建区块
        System.out.print(new Date(System.currentTimeMillis()));
        System.out.printf(" 创建%d个区块...\n",num);
        for (int i = 0; i < num;i++) {
            SECP256K1.KeyPair addrKey = SECP256K1.KeyPair.fromSecretKey(secretkey_1);
            long xdagTime = System.currentTimeMillis()+i*10;
            Block b = new Block(config, xdagTime, null, null, false, null, null, -1);
            b.signOut(addrKey);
            // 预加载
            b.getXdagBlock();
            blocks.add(b);
            generateBar.showBarByPoint("创建区块中:",i+1);
        }
        System.out.println();
        System.out.print(new Date(System.currentTimeMillis()));
        System.out.printf(" 创建完成%d个区块\n",num);


        // 2. 发起请求。
        JsonCall jsonCall = new JsonCall();
        for (int i = 0; i< num;i++) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "xdag_sendRawTransaction");
            jsonObject.put("jsonrpc", "2.0");
            jsonObject.put("id", 1);
            List<String> list = new ArrayList<>();
            list.add(blocks.get(i).getXdagBlock().getData().toUnprefixedHexString());
            jsonObject.put("params", list);
            jsonCall.postASync(url, jsonObject.toString());
            sendBar.showBarByPoint("发送区块中:",i+1);

        }
        System.out.println();
        System.out.print(new Date(System.currentTimeMillis()));
        System.out.println(" 全部发送完成，开始检查是否上链...");
        // 3. 定时查询区块是否加入
        long ms = 64*1000;
        long currentTime;
        List<CompletableFuture<Response>> futures = new ArrayList<>();
        CompletableFuture<Integer> onChainAcceptedFuture = new CompletableFuture<>();
        do {
            currentTime = System.currentTimeMillis();
            if (currentTime % ms == 0) {
                futures.clear();
                for (int i = 0; i < num; i++) {
                    searchBar.showBarByPoint("查询中:",i+1);
                    // 发起请求
                    jsonCall = new JsonCall();
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("method", "xdag_getBlockStatus");
                    jsonObject.put("jsonrpc", "2.0");
                    jsonObject.put("id", 1);
                    List<String> list = new ArrayList<>();
                    list.add(blocks.get(i).getHashLow().toUnprefixedHexString());
                    jsonObject.put("params", list);
                    CompletableFuture<Response> res = jsonCall.postASyncAndGetFuture(url, jsonObject.toString());
                    futures.add(res);
                }
                CompletableFuture<Void> allofFutures = CompletableFuture.allOf(
                        futures.toArray(new CompletableFuture[futures.size()])
                );

                CompletableFuture<List<String>> allResponse = allofFutures.thenApply(v->{
                    return futures.stream().map(future -> {
                        try {
                            return future.get().body().string();
                        } catch (IOException | InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }).collect(Collectors.toList());
                });

                onChainAcceptedFuture = allResponse.thenApply(
                  response -> {
                      int i = 0;
                      for (String res : response) {
                          BlockResult result = gson.fromJson(res, BlockResult.class);
                          String status = result.getResult().getStatus();
                          if (status.equals("Accepted") || status.equals("Rejected") || status.equals("Main")) {
                              i++;
                          }
                      }
                      return i;
                  }
                );
                try {
                    onChainAccepted = onChainAcceptedFuture.get();
                    System.out.println("\n"+new Date(System.currentTimeMillis()));
                    System.out.printf("已处理%d个区块\n",onChainAccepted);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        } while (onChainAccepted != num);
        System.out.print(new Date(System.currentTimeMillis()));
        System.out.printf(" %d个区块成功上链\n",onChainAccepted);
    }
}

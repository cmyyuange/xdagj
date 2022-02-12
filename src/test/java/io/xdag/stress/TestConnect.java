package io.xdag.stress;

import cn.hutool.json.JSONObject;
import io.xdag.stress.common.JsonCallNS;
import io.xdag.stress.common.ProgressBar;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Test;

public class TestConnect {

    @Test
    public void test() {
        final String DESTINATION_ADDRESS = "http://127.0.0.1:4444";

        // 1.2 发送区块
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("method", "xdag_getTotalBalance");
        jsonObject.put("jsonrpc", "2.0");
        jsonObject.put("id", 1);
        List<String> list = new ArrayList<>();
        jsonObject.put("params", list);

        Request request = new Request.Builder()
                .url(DESTINATION_ADDRESS)
                .build();
        JsonCallNS jsonCallNS = new JsonCallNS();
        CompletableFuture<String> future = jsonCallNS.testPost(DESTINATION_ADDRESS,jsonObject.toString());
        future.thenApply((result) -> {
            System.out.println(result);
            return "success";
        });
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        int num = 4000000;
        long current = System.currentTimeMillis();
        ProgressBar sendBar = new ProgressBar(num,30);
        for (int i = 0;i<num;i++) {
//            sendBar.showBarByPoint("发送区块中:",i+1);
        }

        System.out.println();
        System.out.println(System.currentTimeMillis()-current);
    }

}

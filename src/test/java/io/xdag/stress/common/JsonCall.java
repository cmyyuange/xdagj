package io.xdag.stress.common;

import io.netty.util.concurrent.CompleteFuture;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonCall {
    private static final Logger logger = LoggerFactory.getLogger(JsonCall.class);

    private static OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(100L, TimeUnit.SECONDS)
            .readTimeout(100L, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    static {
        client.dispatcher().setMaxRequests(200);
        client.dispatcher().setMaxRequestsPerHost(200);
    }

    public String post(String url, String json) {

        MediaType JSON
                = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Connection", "close")
                .build();
        try {
            Response response = client.newCall(request).execute();
            return response.body().string();
        } catch (IOException e) {
            logger.error(e.getMessage());
            return null;
        }
    }


    public void postASync(String url, String json) {
        MediaType JSON
                = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Connection", "close")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.error("failure");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                response.body().string();
            }

        });
    }

    public CompletableFuture<Response> postASyncAndGetFuture(String url, String json) {
        MediaType JSON
                = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Connection", "close")
                .build();
        CompletableFuture<Response> future = new CompletableFuture<>();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.error("failure");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                future.complete(response);
            }

        });
        return future;
    }
}
package io.xdag.stress.common;

import com.google.common.base.Throwables;
import com.sun.istack.NotNull;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.Data;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Dispatcher;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class JsonCallNS {
    public static OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(50L, TimeUnit.SECONDS)
            .readTimeout(60L, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    public CompletableFuture<Response> post(String url, String json) {
        MediaType JSON
                = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Connection", "close")
                .build();

        CompletableFuture<Response> f = new CompletableFuture<>();

        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                f.completeExceptionally(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                f.complete(response);
            }
        });
        return f;
    }

    public CompletableFuture<String> testPost(String url,String json) {
        MediaType JSON
                = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Connection", "close")
                .build();
        CompletableFuture<String> f = new CompletableFuture<>();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                f.complete(response.body().string());
            }

        });
        return f;
    }

//    public void postAsync(String url, String json) {
//
//        MediaType JSON
//                = MediaType.get("application/json; charset=utf-8");
//        RequestBody body = RequestBody.create(JSON, json);
//        Request request = new Request.Builder()
//                .url(url)
//                .post(body)
//                .header("Connection", "close")
//                .build();
//
//        OkHttpResponseFuture callback = new OkHttpResponseFuture();
//        client.newCall(request).enqueue(callback);
//    }

    public String postSync(String url, String json) {

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
            e.printStackTrace();
            return null;
        }
    }

    public  class OkHttpResponseFuture implements Callback {
        public final CompletableFuture<Response> future = new CompletableFuture<>();

        public OkHttpResponseFuture() {
        }

        @Override public void onFailure(Call call, IOException e) {
//            future.completeExceptionally(e);
            System.out.println("error");
        }

        @Override public void onResponse(Call call, Response response) throws IOException {
//            future.complete(response);
            System.out.println(response.body().string());
        }
    }
}

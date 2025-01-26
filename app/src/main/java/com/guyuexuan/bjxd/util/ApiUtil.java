package com.guyuexuan.bjxd.util;

import com.guyuexuan.bjxd.model.TaskStatus;
import com.guyuexuan.bjxd.model.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiUtil {
    private static final String BASE_URL = "https://bm2-api.bluemembers.com.cn";
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS) // 2 秒无法建立 TCP 连接就放弃
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true) // dns 解析多个 ip 时，按 ipv6...ipv4... 顺序尝试，直到某个 ip 成功或全部失败
            .connectionPool(new ConnectionPool(1, 5, TimeUnit.MINUTES))
            .build();

    // API 端点
    private static final String API_USER_INFO = "/v1/app/account/users/info";
    private static final String API_MY_SCORE = "/v1/app/user/my_score";
    private static final String API_TASK_LIST = "/v1/app/user/task/list";
    private static final String API_SIGN_LIST = "/v1/app/user/reward_list";
    private static final String API_SIGN_SUBMIT = "/v1/app/user/reward_report";
    private static final String API_ARTICLE_LIST = "/v1/app/white/article/list2";
    private static final String API_ARTICLE_DETAIL = "/v1/app/white/article/detail_app/%s";
    private static final String API_TASK_SCORE = "/v1/app/score";
    private static final String API_QUESTION_INFO = "/v1/app/special/daily/ask_info";
    private static final String API_QUESTION_SUBMIT = "/v1/app/special/daily/ask_answer";

    // 添加默认请求头
    private static Request.Builder getRequestBuilder(String token) {
        return new Request.Builder()
                .addHeader("token", token)
                .addHeader("device", "mp"); // 添加device=mp头
    }

    public static User getUserInfo(String token) throws IOException, JSONException {
        Request request = getRequestBuilder(token)
                .url(BASE_URL + API_USER_INFO)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                System.out.println("getUserInfo API Response: " + json);

                JSONObject jsonObject = new JSONObject(json);
                if (jsonObject.getInt("code") == 0) {
                    JSONObject data = jsonObject.getJSONObject("data");
                    return new User(
                            token,
                            data.getString("nickname"),
                            data.getString("phone"),
                            data.getString("hid"),
                            0);
                } else {
                    throw new IOException(jsonObject.getString("msg"));
                }
            } else {
                if (response.code() == 403) {
                    throw new IOException("Code 403: Token 已过期，请重新添加账号");
                } else {
                    throw new IOException("请求失败: " + response.code());
                }
            }
        }
    }

    public static JSONObject getScore(String token) throws IOException, JSONException {
        Request request = getRequestBuilder(token)
                .url(BASE_URL + API_MY_SCORE + "?page_no=1&page_size=5")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                System.out.println("getScore API Response: " + json);

                JSONObject jsonObject = new JSONObject(json);
                if (jsonObject.getInt("code") == 0) {
                    return jsonObject.getJSONObject("data");
                } else {
                    throw new IOException(jsonObject.getString("msg"));
                }
            } else {
                if (response.code() == 403) {
                    throw new IOException("Code 403: Token 已过期，请重新添加账号");
                } else {
                    throw new IOException("请求失败: " + response.code());
                }
            }
        }
    }

    public static TaskStatus getTaskStatus(String token) throws IOException, JSONException {
        Request request = getRequestBuilder(token)
                .url(BASE_URL + API_TASK_LIST)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                System.out.println("getTaskStatus API Response: " + json);

                JSONObject jsonObject = new JSONObject(json);
                if (jsonObject.getInt("code") == 0) {
                    JSONObject data = jsonObject.getJSONObject("data");
                    TaskStatus status = new TaskStatus();

                    // 检查签到任
                    if (data.has("action4")) {
                        status.setSignCompleted(data.getJSONObject("action4").getInt("status") == 1);
                    }

                    // 检查浏览文章任务
                    if (data.has("action12")) {
                        status.setViewCompleted(data.getJSONObject("action12").getInt("status") == 1);
                    }

                    // 检查答题任务
                    if (data.has("action39")) {
                        status.setQuestionCompleted(data.getJSONObject("action39").getInt("status") == 1);
                    }

                    return status;
                } else {
                    throw new IOException(jsonObject.getString("msg"));
                }
            } else {
                throw new IOException("请求失败: " + response.code());
            }
        }
    }

    public static JSONObject getSignInfo(String token) throws IOException, JSONException {
        Request request = getRequestBuilder(token)
                .url(BASE_URL + API_SIGN_LIST)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                System.out.println("getSignInfo API Response: " + json);

                JSONObject jsonObject = new JSONObject(json);
                if (jsonObject.getInt("code") == 0) {
                    return jsonObject.getJSONObject("data");
                } else {
                    throw new IOException(jsonObject.getString("msg"));
                }
            } else {
                throw new IOException("请求失败: " + response.code());
            }
        }
    }

    public static void submitSign(String token, String hid, String rewardHash) throws IOException, JSONException {
        JSONObject jsonBody = new JSONObject()
                .put("hid", hid)
                .put("hash", rewardHash)
                .put("sm_deviceId", "")
                .put("ctu_token", JSONObject.NULL);

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json; charset=utf-8"));

        Request request = getRequestBuilder(token)
                .url(BASE_URL + API_SIGN_SUBMIT)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                System.out.println("submitSign API Response: " + json);

                JSONObject jsonObject = new JSONObject(json);
                if (jsonObject.getInt("code") != 0) {
                    throw new IOException(jsonObject.getString("msg"));
                }
            } else {
                throw new IOException("请求失败: " + response.code());
            }
        }
    }

    public static JSONObject getArticleList(String token) throws IOException, JSONException {
        Request request = getRequestBuilder(token)
                .url(BASE_URL + API_ARTICLE_LIST + "?page_no=1&page_size=20&type_hid=")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                System.out.println("getArticleList API Response: " + json);

                JSONObject jsonObject = new JSONObject(json);
                if (jsonObject.getInt("code") == 0) {
                    return jsonObject.getJSONObject("data");
                } else {
                    throw new IOException(jsonObject.getString("msg"));
                }
            } else {
                throw new IOException("请求失败: " + response.code());
            }
        }
    }

    public static void viewArticle(String token, String articleId) throws IOException, JSONException {
        Request request = getRequestBuilder(token)
                .url(BASE_URL + String.format(API_ARTICLE_DETAIL, articleId))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                System.out.println("viewArticle API Response: " + json);

                JSONObject jsonObject = new JSONObject(json);
                if (jsonObject.getInt("code") != 0) {
                    throw new IOException(jsonObject.getString("msg"));
                }
            } else {
                throw new IOException("请求失败: " + response.code());
            }
        }
    }

    public static JSONObject submitArticleScore(String token) throws IOException, JSONException {
        JSONObject jsonBody = new JSONObject()
                .put("ctu_token", "")
                .put("action", 12);

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json; charset=utf-8"));

        Request request = getRequestBuilder(token)
                .url(BASE_URL + API_TASK_SCORE)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                System.out.println("submitArticleScore API Response: " + json);

                JSONObject jsonObject = new JSONObject(json);
                if (jsonObject.getInt("code") == 0) {
                    return jsonObject.getJSONObject("data");
                } else {
                    throw new IOException(jsonObject.getString("msg"));
                }
            } else {
                throw new IOException("请求失败: " + response.code());
            }
        }
    }

    public static JSONObject getQuestionInfo(String token) throws IOException, JSONException {
        String date = new SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                .format(new Date());
        Request request = getRequestBuilder(token)
                .url(BASE_URL + API_QUESTION_INFO + "?date=" + date)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                System.out.println("getQuestionInfo API Response: " + json);

                JSONObject jsonObject = new JSONObject(json);
                if (jsonObject.getInt("code") == 0) {
                    return jsonObject.getJSONObject("data");
                } else {
                    throw new IOException(jsonObject.getString("msg"));
                }
            } else {
                throw new IOException("请求失败: " + response.code());
            }
        }
    }

    public static JSONObject submitQuestionAnswer(String token, String questionId, String answer, String shareUserHid)
            throws IOException, JSONException {
        JSONObject jsonBody = new JSONObject()
                .put("answer", answer)
                .put("questions_hid", questionId)
                .put("ctu_token", "");

        // 如果有 shareUserHid，添加日期和分享用户 hid
        if (shareUserHid != null && !shareUserHid.isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            jsonBody.put("date", sdf.format(new Date()));
            jsonBody.put("share_user_hid", shareUserHid);
        }

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json; charset=utf-8"));

        Request request = getRequestBuilder(token)
                .url(BASE_URL + API_QUESTION_SUBMIT)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                System.out.println("submitQuestionAnswer API Response: " + json);

                JSONObject jsonObject = new JSONObject(json);
                if (jsonObject.getInt("code") == 0) {
                    return jsonObject.getJSONObject("data");
                } else {
                    throw new IOException(jsonObject.getString("msg"));
                }
            } else {
                throw new IOException("请求失败: " + response.code());
            }
        }
    }

    public static String askAI(String apiKey, String question) throws IOException, JSONException {
        JSONObject jsonBody = new JSONObject()
                .put("model", "hunyuan-turbo")
                .put("messages", new JSONArray()
                        .put(new JSONObject()
                                .put("role", "user")
                                .put("content", question)))
                .put("enable_enhancement", true)
                .put("force_search_enhancement", true)
                .put("enable_instruction_search", true);

        Request request = new Request.Builder()
                .url("https://api.hunyuan.cloud.tencent.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(jsonBody.toString(),
                        MediaType.parse("application/json; charset=utf-8")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                System.out.println("askAI API Response: " + json);

                JSONObject jsonObject = new JSONObject(json);
                return jsonObject.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");
            } else {
                throw new IOException("请求失败: " + response.code());
            }
        }
    }

}
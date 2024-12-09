package com.guyuexuan.bjxd.util;

import androidx.annotation.NonNull;

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

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiUtil {
    private static final String BASE_URL = "https://bm2-api.bluemembers.com.cn";
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
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

    public static void getUserInfo(String token, ApiCallback<User> callback) {
        Request request = getRequestBuilder(token)
                .url(BASE_URL + API_USER_INFO)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    System.out.println("getUserInfo API Response: " + json);
                    try {
                        JSONObject jsonObject = new JSONObject(json);
                        if (jsonObject.getInt("code") == 0) {
                            JSONObject data = jsonObject.getJSONObject("data");
                            User user = new User(
                                    token,
                                    data.getString("nickname"),
                                    data.getString("phone"),
                                    data.getString("hid"),
                                    0);
                            callback.onSuccess(user);
                        } else {
                            callback.onError(jsonObject.getString("msg"));
                        }
                    } catch (JSONException e) {
                        callback.onError(e.getMessage());
                    }
                } else {
                    callback.onError("请求失败: " + response.code());
                }
            }
        });
    }

    public static void getScore(String token, ApiCallback<JSONObject> callback) {
        Request request = getRequestBuilder(token)
                .url(BASE_URL + API_MY_SCORE + "?page_no=1&page_size=5")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    System.out.println("getScore API Response: " + json);
                    try {
                        JSONObject jsonObject = new JSONObject(json);
                        if (jsonObject.getInt("code") == 0) {
                            callback.onSuccess(jsonObject.getJSONObject("data"));
                        } else {
                            callback.onError(jsonObject.getString("msg"));
                        }
                    } catch (JSONException e) {
                        callback.onError(e.getMessage());
                    }
                } else {
                    callback.onError("请求失败: " + response.code());
                }
            }
        });
    }

    public static void getTaskStatus(String token, ApiCallback<TaskStatus> callback) {
        Request request = getRequestBuilder(token)
                .url(BASE_URL + API_TASK_LIST)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    System.out.println("getTaskStatus API Response: " + json);
                    try {
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

                            callback.onSuccess(status);
                        } else {
                            callback.onError(jsonObject.getString("msg"));
                        }
                    } catch (JSONException e) {
                        callback.onError(e.getMessage());
                    }
                } else {
                    callback.onError("请求失败: " + response.code());
                }
            }
        });
    }

    public static void getSignInfo(String token, ApiCallback<JSONObject> callback) {
        Request request = getRequestBuilder(token)
                .url(BASE_URL + API_SIGN_LIST)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    System.out.println("getSignInfo API Response: " + json);
                    try {
                        JSONObject jsonObject = new JSONObject(json);
                        if (jsonObject.getInt("code") == 0) {
                            callback.onSuccess(jsonObject.getJSONObject("data"));
                        } else {
                            callback.onError(jsonObject.getString("msg"));
                        }
                    } catch (JSONException e) {
                        callback.onError(e.getMessage());
                    }
                } else {
                    callback.onError("请求失败: " + response.code());
                }
            }
        });
    }

    public static void submitSign(String token, String hid, String rewardHash, ApiCallback<Void> callback) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("hid", hid);
            jsonBody.put("hash", rewardHash);
            jsonBody.put("sm_deviceId", "");
            jsonBody.put("ctu_token", JSONObject.NULL);
        } catch (JSONException e) {
            callback.onError(e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json; charset=utf-8"));

        Request request = getRequestBuilder(token)
                .url(BASE_URL + API_SIGN_SUBMIT)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    System.out.println("submitSign API Response: " + json);
                    try {
                        JSONObject jsonObject = new JSONObject(json);
                        if (jsonObject.getInt("code") == 0) {
                            callback.onSuccess(null);
                        } else {
                            callback.onError(jsonObject.getString("msg"));
                        }
                    } catch (JSONException e) {
                        callback.onError(e.getMessage());
                    }
                } else {
                    callback.onError("请求失败: " + response.code());
                }
            }
        });
    }

    public static void getArticleList(String token, ApiCallback<JSONObject> callback) {
        Request request = getRequestBuilder(token)
                .url(BASE_URL + API_ARTICLE_LIST + "?page_no=1&page_size=20&type_hid=")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    System.out.println("getArticleList API Response: " + json);
                    try {
                        JSONObject jsonObject = new JSONObject(json);
                        if (jsonObject.getInt("code") == 0) {
                            callback.onSuccess(jsonObject.getJSONObject("data"));
                        } else {
                            callback.onError(jsonObject.getString("msg"));
                        }
                    } catch (JSONException e) {
                        callback.onError(e.getMessage());
                    }
                } else {
                    callback.onError("请求失败: " + response.code());
                }
            }
        });
    }

    public static void viewArticle(String token, String articleId, ApiCallback<Void> callback) {
        Request request = getRequestBuilder(token)
                .url(BASE_URL + String.format(API_ARTICLE_DETAIL, articleId))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    System.out.println("viewArticle API Response: " + json);
                    try {
                        JSONObject jsonObject = new JSONObject(json);
                        if (jsonObject.getInt("code") == 0) {
                            callback.onSuccess(null);
                        } else {
                            callback.onError(jsonObject.getString("msg"));
                        }
                    } catch (JSONException e) {
                        callback.onError(e.getMessage());
                    }
                } else {
                    callback.onError("请求失败: " + response.code());
                }
            }
        });
    }

    public static void submitArticleScore(String token, ApiCallback<JSONObject> callback) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("ctu_token", "");
            jsonBody.put("action", 12);
        } catch (JSONException e) {
            callback.onError(e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json; charset=utf-8"));

        Request request = getRequestBuilder(token)
                .url(BASE_URL + API_TASK_SCORE)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    System.out.println("submitArticleScore API Response: " + json);
                    try {
                        JSONObject jsonObject = new JSONObject(json);
                        if (jsonObject.getInt("code") == 0) {
                            callback.onSuccess(jsonObject.getJSONObject("data"));
                        } else {
                            callback.onError(jsonObject.getString("msg"));
                        }
                    } catch (JSONException e) {
                        callback.onError(e.getMessage());
                    }
                } else {
                    callback.onError("请求失败: " + response.code());
                }
            }
        });
    }

    public static void getQuestionInfo(String token, ApiCallback<JSONObject> callback) {
        String date = new SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                .format(new Date());
        Request request = getRequestBuilder(token)
                .url(BASE_URL + API_QUESTION_INFO + "?date=" + date)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    System.out.println("getQuestionInfo API Response: " + json);
                    try {
                        JSONObject jsonObject = new JSONObject(json);
                        if (jsonObject.getInt("code") == 0) {
                            callback.onSuccess(jsonObject.getJSONObject("data"));
                        } else {
                            callback.onError(jsonObject.getString("msg"));
                        }
                    } catch (JSONException e) {
                        callback.onError(e.getMessage());
                    }
                } else {
                    callback.onError("请求失败: " + response.code());
                }
            }
        });
    }

    public static void submitQuestionAnswer(String token, String questionId, String answer, String shareUserHid,
                                            ApiCallback<JSONObject> callback) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("answer", answer);
            jsonBody.put("questions_hid", questionId);
            jsonBody.put("ctu_token", "");

            // 如果有 shareUserHid，添加日期和分享用户 hid
            if (shareUserHid != null && !shareUserHid.isEmpty()) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
                jsonBody.put("date", sdf.format(new Date()));
                jsonBody.put("share_user_hid", shareUserHid);
            }
        } catch (JSONException e) {
            callback.onError(e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json; charset=utf-8"));

        Request request = getRequestBuilder(token)
                .url(BASE_URL + API_QUESTION_SUBMIT)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    System.out.println("submitQuestionAnswer API Response: " + json);
                    try {
                        JSONObject jsonObject = new JSONObject(json);
                        if (jsonObject.getInt("code") == 0) {
                            callback.onSuccess(jsonObject.getJSONObject("data"));
                        } else {
                            callback.onError(jsonObject.getString("msg"));
                        }
                    } catch (JSONException e) {
                        callback.onError(e.getMessage());
                    }
                } else {
                    callback.onError("请求失败: " + response.code());
                }
            }
        });
    }

    public static void askAI(String apiKey, String question, ApiCallback<String> callback) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", "hunyuan-turbo");
            jsonBody.put("messages", new JSONArray()
                    .put(new JSONObject()
                            .put("role", "user")
                            .put("content", question)));
            jsonBody.put("enable_enhancement", true);
            jsonBody.put("force_search_enhancement", true);
            jsonBody.put("enable_instruction_search", true);
        } catch (JSONException e) {
            callback.onError(e.getMessage());
            return;
        }

        Request request = new Request.Builder()
                .url("https://api.hunyuan.cloud.tencent.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(jsonBody.toString(),
                        MediaType.parse("application/json; charset=utf-8")))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    System.out.println("askAI API Response: " + json);
                    try {
                        JSONObject jsonObject = new JSONObject(json);
                        String answer = jsonObject.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");
                        callback.onSuccess(answer);
                    } catch (JSONException e) {
                        callback.onError(e.getMessage());
                    }
                } else {
                    callback.onError("请求失败: " + response.code());
                }
            }
        });
    }

}
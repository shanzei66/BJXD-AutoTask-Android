package com.guyuexuan.bjxd.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.guyuexuan.bjxd.model.User;

import java.lang.reflect.Type;
import java.util.List;

public class StorageUtil {
    private static final String PREF_NAME = "app_config";
    private static final String KEY_USERS = "users";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_MANUAL_ANSWER = "manual_answer";
    private final SharedPreferences prefs;
    private final Gson gson;

    public StorageUtil(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void saveUsers(List<User> users) {
        String json = gson.toJson(users);
        prefs.edit().putString(KEY_USERS, json).apply();
    }

    public List<User> getUsers() {
        String json = prefs.getString(KEY_USERS, "[]");
        Type type = new TypeToken<List<User>>() {
        }.getType();
        return gson.fromJson(json, type);
    }

    public void saveApiKey(String apiKey) {
        prefs.edit().putString(KEY_API_KEY, apiKey).apply();
    }

    public String getApiKey() {
        return prefs.getString(KEY_API_KEY, "");
    }

    public boolean isManualAnswer() {
        return prefs.getBoolean(KEY_MANUAL_ANSWER, true);
    }

    public void setManualAnswer(boolean enabled) {
        prefs.edit().putBoolean(KEY_MANUAL_ANSWER, enabled).apply();
    }

    public void addUser(User user) {
        List<User> users = getUsers();
        // 检查是否已存在
        boolean isUpdated = false;
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getPhone().equals(user.getPhone())) {
                // 更新已存在的用户信息，保持原有的 order
                user.setOrder(users.get(i).getOrder());
                users.set(i, user);
                isUpdated = true;
                break;
            }
        }

        if (!isUpdated) {
            // 添加新用户，order 设为列表长度
            user.setOrder(users.size());
            users.add(user);
        }

        saveUsers(users);
    }
}
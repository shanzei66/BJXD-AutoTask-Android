package com.guyuexuan.bjxd.model;

import org.json.JSONException;
import org.json.JSONObject;

public class User {
    private final String token;
    private final String nickname;
    private final String phone;
    private final String hid;
    private String shareUserHid = "";
    private int order;
    private String addedTime;

    public User(String token, String nickname, String phone, String hid, int order) {
        this.token = token;
        this.nickname = nickname;
        this.phone = phone;
        this.hid = hid;
        this.order = order;
        this.addedTime = "";
    }

    public User(String token, String nickname, String phone, String hid, int order, String addedTime) {
        this.token = token;
        this.nickname = nickname;
        this.phone = phone;
        this.hid = hid;
        this.order = order;
        this.addedTime = addedTime;
    }

    public static User fromString(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        return new User(
                obj.getString("token"),
                obj.getString("nickname"),
                obj.getString("phone"),
                obj.getString("hid"),
                obj.getInt("order"),
                obj.optString("addedTime", ""));
    }

    public String getToken() {
        return token;
    }

    public String getNickname() {
        return nickname;
    }

    public String getPhone() {
        return phone;
    }

    /**
     * 获取隐藏中间6位数字的手机号
     * 例如：138******34
     */
    public String getMaskedPhone() {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "******" + phone.substring(9);
    }

    public String getHid() {
        return hid;
    }

    public String getShareUserHid() {
        return shareUserHid;
    }

    public void setShareUserHid(String shareUserHid) {
        this.shareUserHid = shareUserHid;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void setAddedTime(String currentTime) {
        this.addedTime = currentTime;
    }

    public String getAddedTime() {
        return addedTime;
    }

    @Override
    public String toString() {
        return String.format(
                "{\"token\":\"%s\",\"nickname\":\"%s\",\"phone\":\"%s\",\"hid\":\"%s\",\"order\":%d,\"addedTime\":\"%s\"}",
                token, nickname, phone, hid, order, addedTime);
    }
}
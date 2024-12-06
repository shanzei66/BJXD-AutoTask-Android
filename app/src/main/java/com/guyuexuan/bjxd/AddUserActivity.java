package com.guyuexuan.bjxd;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.guyuexuan.bjxd.model.User;
import com.guyuexuan.bjxd.util.ApiCallback;
import com.guyuexuan.bjxd.util.ApiUtil;
import com.guyuexuan.bjxd.util.StorageUtil;

public class AddUserActivity extends AppCompatActivity {
    private StorageUtil storageUtil;
    private WebView loginWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_user);

        storageUtil = new StorageUtil(this);
        initViews();
        setupWebView();
    }

    private void initViews() {
        loginWebView = findViewById(R.id.loginWebView);

        // 添加账号按钮点击事件
        findViewById(R.id.addUserButton).setOnClickListener(v -> {
            extractToken();
        });

        // 返回按钮点击事件
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
    }

    private void setupWebView() {
        WebSettings settings = loginWebView.getSettings();

        // 启用JavaScript支持，允许网页执行JS代码
        settings.setJavaScriptEnabled(true);

        // 启用DOM Storage API支持，允许网页使用localStorage
        settings.setDomStorageEnabled(true);

        // 加载登录页面
        loginWebView.loadUrl("https://bm2-wx.bluemembers.com.cn/browser/login");
    }

    /**
     * 从localStorage中提取token
     */
    private void extractToken() {
        loginWebView.evaluateJavascript(
                "localStorage.getItem('token')",
                value -> {
                    if (value != null && !value.equals("null")) {
                        // 移除引号
                        String token = value.replaceAll("\"", "");
                        addUser(token);
                    } else {
                        runOnUiThread(() -> Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show());
                    }
                });
    }

    /**
     * 添加用户
     *
     * @param token 用户token
     */
    private void addUser(String token) {
        ApiUtil.getUserInfo(token, new ApiCallback<User>() {
            @Override
            public void onSuccess(User user) {
                runOnUiThread(() -> {
                    // 保存用户信息
                    StorageUtil storageUtil = new StorageUtil(AddUserActivity.this);
                    storageUtil.addUser(user);

                    // 返回主页
                    setResult(RESULT_OK);
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(AddUserActivity.this, "获取用户信息失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
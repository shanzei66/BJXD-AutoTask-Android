package com.guyuexuan.bjxd;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.guyuexuan.bjxd.model.User;
import com.guyuexuan.bjxd.util.ApiCallback;
import com.guyuexuan.bjxd.util.ApiUtil;
import com.guyuexuan.bjxd.util.AppUtils;
import com.guyuexuan.bjxd.util.StorageUtil;

public class AddUserActivity extends AppCompatActivity {
    private StorageUtil storageUtil;
    private WebView loginWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_user);

        // 设置标题
        setTitle(AppUtils.getAppNameWithVersion(this));

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

        // 手动添加账号按钮点击事件
        findViewById(R.id.manualAddUserButton).setOnClickListener(v -> {
            showTokenInputDialog();
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

    // 显示输入框以手动输入 token
    private void showTokenInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("手动输入 Token");

        // 创建输入框
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // 设置确认按钮
        builder.setPositiveButton("确认", (dialog, which) -> {
            String token = input.getText().toString().trim();
            if (!token.isEmpty()) {
                addUser(token); // 调用 addUser 方法
            } else {
                Toast.makeText(this, "Token 不能为空", Toast.LENGTH_SHORT).show();
            }
        });

        // 设置取消按钮
        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());

        // 显示对话框
        builder.show();
    }
}
package com.guyuexuan.bjxd;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.guyuexuan.bjxd.util.StorageUtil;

import java.util.Objects;

public class ConfigActivity extends AppCompatActivity {
    private TextInputEditText apiKeyInput;
    private SwitchMaterial manualAnswerSwitch;
    private StorageUtil storageUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        storageUtil = new StorageUtil(this);

        apiKeyInput = findViewById(R.id.apiKeyInput);
        apiKeyInput.setText(storageUtil.getApiKey());

        manualAnswerSwitch = findViewById(R.id.manualAnswerSwitch);
        manualAnswerSwitch.setChecked(storageUtil.isManualAnswer());

        findViewById(R.id.saveButton).setOnClickListener(v -> {
            String apiKey = Objects.requireNonNull(apiKeyInput.getText()).toString().trim();
            storageUtil.saveApiKey(apiKey);
            storageUtil.setManualAnswer(manualAnswerSwitch.isChecked());
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
            finish();
        });

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
    }
}
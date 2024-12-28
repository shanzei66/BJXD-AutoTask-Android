package com.guyuexuan.bjxd;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.guyuexuan.bjxd.adapter.UserAdapter;
import com.guyuexuan.bjxd.model.User;
import com.guyuexuan.bjxd.util.AppUtils;
import com.guyuexuan.bjxd.util.StorageUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements UserAdapter.OnUserActionListener {
    private static final int REQUEST_ADD_USER = 1;
    private final List<User> users = new ArrayList<>();
    private RecyclerView userList;
    private UserAdapter adapter;
    private StorageUtil storageUtil;
    private TextView accountCountText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 设置标题
        setTitle(AppUtils.getAppNameWithVersion(this));

        storageUtil = new StorageUtil(this);
        initViews();
        loadUsers();
    }

    private void initViews() {
        accountCountText = findViewById(R.id.accountCountText);

        userList = findViewById(R.id.userList);
        userList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserAdapter(users, this);
        userList.setAdapter(adapter);

        findViewById(R.id.addUserButton).setOnClickListener(v -> {
            startActivityForResult(new Intent(this, AddUserActivity.class), REQUEST_ADD_USER);
        });

        findViewById(R.id.configButton).setOnClickListener(v -> {
            startActivity(new Intent(this, ConfigActivity.class));
        });

        findViewById(R.id.startTaskButton).setOnClickListener(v -> {
            if (users.isEmpty()) {
                Toast.makeText(this, "请先添加账号", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(this, TaskActivity.class));
        });

        // 添加拖拽排序功能
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            private boolean isDragging = false;

            @Override
            public boolean isLongPressDragEnabled() {
                // 禁用长按拖动
                return false;
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                Collections.swap(users, fromPosition, toPosition);
                adapter.notifyItemMoved(fromPosition, toPosition);

                // 更新所有可见项的序号
                for (int i = 0; i < recyclerView.getChildCount(); i++) {
                    RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
                    if (holder instanceof UserAdapter.UserViewHolder) {
                        ((UserAdapter.UserViewHolder) holder).updateOrder(holder.getAdapterPosition() + 1);
                    }
                }

                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                if (isDragging) {
                    isDragging = false;
                    onMoveUser(viewHolder.getAdapterPosition(), viewHolder.getAdapterPosition());
                }
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                isDragging = actionState == ItemTouchHelper.ACTION_STATE_DRAG;
            }
        };

        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(userList);

        // 将拖动手柄与 ItemTouchHelper 关联
        adapter.setOnStartDragListener(holder -> touchHelper.startDrag(holder));
    }

    private void updateAccountCount() {
        accountCountText.setText(String.format("当前共有 %d 个账号", users.size()));
    }

    private void loadUsers() {
        users.clear();
        users.addAll(storageUtil.getUsers());
        adapter.notifyDataSetChanged();

        // 更新账号数量显示
        updateAccountCount();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUsers();
    }

    @Override
    public void onDeleteUser(User user) {
        new AlertDialog.Builder(this)
                .setTitle("确认删除")
                .setMessage("确定要删除该账号吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    users.remove(user);
                    storageUtil.saveUsers(users);
                    adapter.notifyDataSetChanged();
                    // 更新账号数量显示
                    updateAccountCount();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public void onMoveUser(int fromPosition, int toPosition) {
        // 更新排序后保存
        for (int i = 0; i < users.size(); i++) {
            users.get(i).setOrder(i);
        }
        storageUtil.saveUsers(users);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_USER && resultCode == RESULT_OK) {
            // 用户添加成功，刷新列表
            loadUsers();
        }
    }
}
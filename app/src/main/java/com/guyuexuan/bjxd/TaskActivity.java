package com.guyuexuan.bjxd;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.guyuexuan.bjxd.model.TaskStatus;
import com.guyuexuan.bjxd.model.User;
import com.guyuexuan.bjxd.util.ApiUtil;
import com.guyuexuan.bjxd.util.AppUtils;
import com.guyuexuan.bjxd.util.StorageUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Consumer;

public class TaskActivity extends AppCompatActivity {
    private final Object answerLock = new Object();
    private TextView logTextView;
    private Button actionButton;
    private StorageUtil storageUtil;
    private TaskThread taskThread;
    private String selectedAnswer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        // 设置标题
        setTitle(AppUtils.getAppNameWithVersion(this));

        // 保持屏幕常量
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        storageUtil = new StorageUtil(this);
        initViews();
    }

    private void initViews() {
        logTextView = findViewById(R.id.logTextView);
        actionButton = findViewById(R.id.actionButton);

        actionButton.setOnClickListener(v -> {
            // 关闭屏幕常量
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (taskThread != null && taskThread.isRunning()) {
                actionButton.setText("等待线程结束……");
                actionButton.setEnabled(false);
                taskThread.stopTask();
            } else {
                finish();
            }
        });

        List<User> users = storageUtil.getUsers();
        // 显示总任务数
        appendLog(String.format("共有 %d 个用户任务待执行", users.size()));

        taskThread = new TaskThread(users, storageUtil.getApiKey(), storageUtil, this::appendLog, () -> {
            runOnUiThread(() -> {
                // 关闭屏幕常量
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                actionButton.setText("返回");
                actionButton.setEnabled(true);
            });
        }, this);
        taskThread.start();
    }

    private void appendLog(String log) {
        runOnUiThread(() -> {
            logTextView.append(log + "\n");
            // 滚动到底部
            ScrollView scrollView = (ScrollView) logTextView.getParent();
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (taskThread != null) {
            taskThread.stopTask();
        }
    }

    private void showAnswerDialog(String question, String optionsText, List<String> availableOptionLetters) {
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("请选择答案");

            // 创建一个水平的 RadioGroup 来放置选项
            RadioGroup radioGroup = new RadioGroup(this);
            radioGroup.setOrientation(RadioGroup.HORIZONTAL); // 改为水平布局
            radioGroup.setGravity(Gravity.CENTER); // 居中显示

            // 添加题目文本
            TextView questionView = new TextView(this);
            questionView.setText(question + "\n" + optionsText);
            questionView.setTextSize(16);
            questionView.setPadding(30, 20, 30, 20);

            // 创建一个线性布局来包含所有视图
            LinearLayout container = new LinearLayout(this);
            container.setOrientation(LinearLayout.VERTICAL);
            container.addView(questionView);
            container.addView(radioGroup);

            // 为每个选项创建 RadioButton
            for (String optionLetter : availableOptionLetters) {
                RadioButton radioButton = new RadioButton(this);
                radioButton.setTag(optionLetter); // 保存选项字母用于返回
                radioButton.setText(optionLetter); // 只显示选项字母
                radioButton.setTextSize(16);
                // 调整水平布局的间距
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(30, 20, 30, 20);
                radioButton.setLayoutParams(params);
                radioGroup.addView(radioButton);
            }

            builder.setView(container);

            // 添加确定按钮
            builder.setPositiveButton("确定", null);

            AlertDialog dialog = builder.create();
            dialog.setCancelable(false);
            dialog.show();

            // 重新设置确定按钮的点击监听器
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                int selectedId = radioGroup.getCheckedRadioButtonId();
                if (selectedId != -1) {
                    RadioButton selectedRadioButton = dialog.findViewById(selectedId);
                    synchronized (answerLock) {
                        selectedAnswer = (String) selectedRadioButton.getTag(); // 使用保存的选项字母
                        answerLock.notify();
                    }
                    dialog.dismiss();
                } else {
                    Toast.makeText(TaskActivity.this, "请选择一个答案", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private static class TaskThread extends Thread {
        // 备用分享用户ID列表
        private static final String[] BACKUP_HIDS = {
                "a6688ec1a9ee429fa7b68d50e0c92b1f",
                "bb8cd2e44c7b45eeb8cc5f7fa71c3322",
                "5f640c50061b400c91be326c8fe0accd",
                "55a5d82dacd9417483ae369de9d9b82d"
        };

        private final List<User> users;
        private final String aiApiKey;
        private final StorageUtil storageUtil;
        private final Consumer<String> logger;
        private final Runnable onComplete;
        private final WeakReference<TaskActivity> activityRef;
        private volatile boolean running = true;
        private int currentUserIndex = 0;
        // 新增成员变量
        private String historicalCorrectAnswer = null; // 历史正确答案
        private List<String> wrongAnswers = new ArrayList<>(); // 错误答案列表

        public TaskThread(List<User> users, String aiApiKey, StorageUtil storageUtil, Consumer<String> logger,
                          Runnable onComplete, TaskActivity activity) {
            this.users = users;
            this.aiApiKey = aiApiKey;
            this.storageUtil = storageUtil;
            this.logger = logger;
            this.onComplete = onComplete;
            this.activityRef = new WeakReference<>(activity);
        }

        @Override
        public void run() {
            try {
                // 设置分享用户ID
                logger.accept("\nRUN: 设置分享用户ID");
                setupShareUserHids();

                logger.accept(String.format("\nRUN: 执行任务, 共 %d 个账号", users.size()));
                for (User user : users) {
                    try {
                        checkShouldStop();
                        currentUserIndex++;
                        if (currentUserIndex > 1) {
                            // 延时 5 - 10 秒
                            logger.accept("进行下一个账号, 等待 5-10 秒...");
                            Thread.sleep(5000 + new Random().nextInt(5000));
                        }

                        checkShouldStop();
                        logger.accept(String.format("\n======> 第 %d 个账号", currentUserIndex));
                        executeUserTask(user);
                    } catch (InterruptedException e) {
                        throw e;
                    } catch (Exception e) {
                        logger.accept("执行任务出错: " + e.getMessage());
                    }
                }

                logger.accept(String.format("\nRUN: 积分详情, 共 %d 个账号", users.size()));
                logger.accept(String.format("\n============ 积分详情 ============"));
                for (int i = 0; i < users.size(); i++) {
                    User user = users.get(i);
                    if (i > 0) {
                        logger.accept("\n进行下一个账号, 等待 5-10 秒...");
                        Thread.sleep(5000 + new Random().nextInt(5000));
                    }

                    logger.accept(String.format("\n======== ▷ 第 %d 个账号 ◁ ========", i + 1));
                    logger.accept(String.format("👻 用户名: %s | 手机号: %s", user.getNickname(), user.getMaskedPhone()));

                    // 获取积分详情
                    getScoreDetails(user);
                }

                onComplete.run();
            } catch (InterruptedException e) {
                logger.accept("🚨 任务被中断: " + e.getMessage());
            } finally {
                logger.accept("🚨 任务已停止");
                stopTask();
                onComplete.run();
            }
        }

        /**
         * 检查任务是否需要停止
         *
         * @throws InterruptedException 如果任务需要停止则抛出此异常
         */
        private void checkShouldStop() throws InterruptedException {
            if (!running || isInterrupted()) {
                logger.accept("🚨 检测到停止命令，停止执行任务");
                throw new InterruptedException("Task stopped");
            }
        }

        /**
         * 设置分享用户ID
         */
        private void setupShareUserHids() {
            // 如果只有一个用户或没有用户，使用备用 hid
            if (users.size() <= 1) {
                for (User user : users) {
                    user.setShareUserHid(getBackupShareHid(user.getHid()));
                }
                return;
            }

            // 多个用户时，使用上一个用户的 hid
            for (int i = 0; i < users.size(); i++) {
                User currentUser = users.get(i);
                // 获取上一个用户的索引（第一个用户的上一个是最后一个）
                int prevIndex = (i - 1 >= 0) ? i - 1 : users.size() - 1;
                User prevUser = users.get(prevIndex);

                // 如果上一个用户不是自己，使用上一个用户的 hid
                if (!prevUser.getHid().equals(currentUser.getHid())) {
                    currentUser.setShareUserHid(prevUser.getHid());
                } else {
                    // 否则使用备用 hid
                    currentUser.setShareUserHid(getBackupShareHid(currentUser.getHid()));
                }
            }
        }

        /**
         * 获取备用分享用户ID
         */
        private String getBackupShareHid(String currentHid) {
            // 创建一个不包含当前 hid 的列表
            List<String> availableHids = new ArrayList<>();
            for (String hid : BACKUP_HIDS) {
                if (!hid.equals(currentHid)) {
                    availableHids.add(hid);
                }
            }

            // 如果没有可的 hid（极少情况），第一个备用 hid
            if (availableHids.isEmpty()) {
                return BACKUP_HIDS[0];
            }

            // 随机返回一个可用的 hid
            return availableHids.get(new Random().nextInt(availableHids.size()));
        }

        /**
         * 执行用户任务
         */
        private void executeUserTask(User user) throws InterruptedException {
            logger.accept(String.format("👻 用户名: %s  📱手机号: %s", user.getNickname(), user.getMaskedPhone()));
            logger.accept(String.format("🆔 用户hid: %s", user.getHid()));
            logger.accept(String.format("🆔 分享hid: %s", user.getShareUserHid()));

            try {
                // 检查任务状态
                TaskStatus status = ApiUtil.getTaskStatus(user.getToken());

                checkShouldStop();

                // 答题任务放第一个是为了让手动答题的人可以尽快答题
                if (!status.isQuestionCompleted()) {
                    executeQuestionTask(user);
                    // 延时 5-10 秒
                    Thread.sleep(5000 + new Random().nextInt(5000));
                } else {
                    logger.accept("✅ 答题任务 已完成，跳过");
                    // 获取已答题答案
                    if (historicalCorrectAnswer == null) {
                        getAnsweredAnswer(user);
                    }
                }

                checkShouldStop();

                // 执行未完成的任务
                if (!status.isSignCompleted()) {
                    executeSignTask(user);
                    // 延时 5-10 秒
                    Thread.sleep(5000 + new Random().nextInt(5000));
                } else {
                    logger.accept("✅ 签到任务 已完成，跳过");
                }

                checkShouldStop();

                if (!status.isViewCompleted()) {
                    executeViewTask(user);
                    // 延时 5-10 秒
                    Thread.sleep(5000 + new Random().nextInt(5000));
                } else {
                    logger.accept("✅ 浏览文章任务 已完成，跳过");
                }
            } catch (Exception e) {
                logger.accept("执行任务出错: " + e.getMessage());
            }
        }

        /**
         * 执行签到任务
         */
        private void executeSignTask(User user) throws InterruptedException {
            logger.accept("🔍 开始执行签到任务");

            // 记录最佳签到选项
            String bestHid = null; // 签到任务 hid
            String bestRewardHash = null; // 签到任务 rewardHash
            int bestScore = 0; // 签到任务 奖励积分
            int maxAttemptCount = 5; // 最大尝试次数

            // 尝试多次获取签到信息
            for (int i = 0; i < maxAttemptCount; i++) {
                checkShouldStop();

                try {
                    JSONObject data = ApiUtil.getSignInfo(user.getToken());
                    String hid = data.getString("hid");
                    String rewardHash = data.getString("rewardHash");
                    int currentBestScore = 0;

                    JSONArray list = data.getJSONArray("list");
                    for (int j = 0; j < list.length(); j++) {
                        JSONObject item = list.getJSONObject(j);
                        if (item.getString("hid").equals(hid)) {
                            currentBestScore = item.getInt("score");
                            break;
                        }
                    }

                    // 更新最佳选项
                    if (currentBestScore > bestScore) {
                        bestScore = currentBestScore;
                        bestHid = hid;
                        bestRewardHash = rewardHash;
                    }
                    // 打印当前尝试的签到信息
                    logger.accept(String.format("第 %d 次尝试: score=%d hid=%s rewardHash=%s",
                            i + 1, currentBestScore, hid, rewardHash));
                    logger.accept(String.format("当前可获得签到积分: %d", bestScore));

                } catch (Exception e) {
                    logger.accept("获取签到信息失败: " + e.getMessage());
                }

                // 延时
                if (i < maxAttemptCount - 1) {
                    // 延时 8-10 秒
                    logger.accept("继续尝试获取更高签到积分, 延时8-10s");
                    Thread.sleep(8000 + new Random().nextInt(2000));
                } else {
                    // 延时 3-4 秒
                    logger.accept("即将提交签到, 延时3-4s");
                    Thread.sleep(3000 + new Random().nextInt(1000));
                }
            }

            // 检查是否需要停止
            checkShouldStop();

            // 如果找到了最佳选项，执行签到
            if (bestHid != null && bestRewardHash != null) {
                try {
                    ApiUtil.submitSign(user.getToken(), bestHid, bestRewardHash);
                    logger.accept(String.format("✅ 签到成功: 积分+%d", bestScore));
                } catch (Exception e) {
                    logger.accept("❌ 签到失败: " + e.getMessage());
                }
            } else {
                logger.accept("未找到可用的签到选项");
            }
        }

        /**
         * 执行浏览文章任务
         */
        private void executeViewTask(User user) throws InterruptedException {
            logger.accept("🔍 开始执行浏览文章任务");

            try {
                JSONObject articles = ApiUtil.getArticleList(user.getToken());

                // 获取文章列表
                JSONArray list = articles.getJSONArray("list");
                if (list.length() > 0) {
                    // 收集所有文章的 hid
                    List<String> articleHids = new ArrayList<>();
                    for (int i = 0; i < list.length(); i++) {
                        articleHids.add(list.getJSONObject(i).getString("hid"));
                    }
                    // 打乱顺序
                    Collections.shuffle(articleHids);

                    // 选择前3篇文章（如果文章数量不足3篇，则全部选择）
                    int articlesToRead = Math.min(3, articleHids.size());
                    logger.accept(String.format("需要浏览 %d 篇文章", articlesToRead));

                    // 循环浏览文章
                    for (int i = 0; i < articlesToRead; i++) {
                        checkShouldStop();

                        String articleId = articleHids.get(i);
                        logger.accept(String.format("\n浏览第 %d/%d 篇文章: hid=%s", i + 1, articlesToRead, articleId));

                        try {
                            ApiUtil.viewArticle(user.getToken(), articleId);
                            // 延时 10-15 秒
                            logger.accept("浏览文章 10-15 秒");
                            Thread.sleep(11000 + new Random().nextInt(4000));
                        } catch (Exception e) {
                            logger.accept(String.format("❌ 浏览文章失败: %s", e.getMessage()));
                        }
                    }

                    checkShouldStop();

                    // 提交文章积分
                    try {
                        JSONObject data = ApiUtil.submitArticleScore(user.getToken());
                        int score = data.getInt("score");
                        logger.accept(String.format("✅ 浏览文章成功: 积分+%d", score));
                    } catch (Exception e) {
                        logger.accept("❌ 提交文章积分失败: " + e.getMessage());
                    }
                } else {
                    logger.accept("❌ 没有可浏览的文章");
                }
            } catch (Exception e) {
                logger.accept("获取文章列表失败: " + e.getMessage());
            }
        }

        private String getQuestionAnswer(String question, String optionsText, List<String> availableOptionLetters)
                throws InterruptedException {
            String answer;

            // 检查是否存在历史正确答案
            if (historicalCorrectAnswer != null) {
                answer = historicalCorrectAnswer;
                logger.accept("使用历史正确答案: " + answer);
                return answer;
            }

            // 检查是否需要手动答题
            if (storageUtil.isManualAnswer()) {
                if (!availableOptionLetters.isEmpty()) {
                    TaskActivity activity = activityRef.get();
                    if (activity != null) {
                        // 显示选择弹窗
                        activity.showAnswerDialog(question, optionsText, availableOptionLetters);

                        // 等待用户选择
                        synchronized (activity.answerLock) {
                            while (activity.selectedAnswer == null) {
                                activity.answerLock.wait();
                            }
                            answer = activity.selectedAnswer;
                            activity.selectedAnswer = null; // 重置选择
                        }

                        logger.accept("用户选择答案: " + answer);
                        return answer;
                    }
                }
            }

            // 检查是否设置了AI API Key
            if (!aiApiKey.isEmpty()) {
                logger.accept("使用 AI 查询答案……");
                String prompt = "你是一个专业的北京现代汽车专家，请直接给出这个单选题的答案，并且不要带'答案'等其他内容。\n" +
                        question + optionsText;

                try {
                    String aiResult = ApiUtil.askAI(aiApiKey, prompt);
                    // 提取答案中的选项字母
                    String extractedAnswer = aiResult.replaceAll("[^A-D]", "");
                    if (extractedAnswer.length() > 0
                            && availableOptionLetters.contains(String.valueOf(extractedAnswer.charAt(0)))) {
                        answer = String.valueOf(extractedAnswer.charAt(0));
                        logger.accept("使用 AI 答案: " + answer);
                        return answer;
                    } else {
                        logger.accept("AI 回答无效或不在可用选项中: " + aiResult);
                    }
                } catch (Exception e) {
                    logger.accept("AI 请求失败: " + e.getMessage());
                }
            }

            // 从可用选项中随机选择答案
            if (!availableOptionLetters.isEmpty()) {
                answer = availableOptionLetters.get(new Random().nextInt(availableOptionLetters.size()));
                logger.accept("从可用选项中随机选择答案: " + answer);
                return answer;
            }

            // 使用完全随机答案
            String[] options = {"A", "B", "C", "D"};
            answer = options[new Random().nextInt(options.length)];
            logger.accept("没有可用选项，使用完全随机答案: " + answer);
            return answer;
        }

        private void executeQuestionTask(User user) throws InterruptedException {
            logger.accept("🔍 开始执行答题任务");

            try {
                JSONObject data = ApiUtil.getQuestionInfo(user.getToken());
                JSONObject questionInfo = data.getJSONObject("question_info");

                // 检查答题状态
                // state: 1=未答题 2=已答题且正确 3=答错且未有人帮忙答题 4=答错但有人帮忙答题
                int state = data.getInt("state");
                if (state == 3) {
                    logger.accept("今日已答题但回答错误，当前无人帮助答题，跳过");
                    return;
                }
                if (state != 1) {
                    // 尝试获取已有答案
                    if (data.has("answer")) {
                        String answerText = data.getString("answer");
                        // 从 "C.6个" 格式中提取 "C"
                        if (answerText.matches("[A-D].*")) {
                            String answer = answerText.substring(0, 1);
                            historicalCorrectAnswer = answer;
                            logger.accept(String.format("今日已答题，跳过，答案：%s", answer));
                            return;
                        }
                    }
                    logger.accept("今日已答题，但未获取到答案，跳过");
                    return;
                }

                String questionId = questionInfo.getString("questions_hid");
                String question = questionInfo.getString("content");
                JSONArray options = questionInfo.getJSONArray("option");

                // 构建选项文本
                StringBuilder optionsText = new StringBuilder("\n"); // 可用选项文本
                List<String> availableOptionLetters = new ArrayList<>(); // 可用选项字母列表
                for (int i = 0; i < options.length(); i++) {
                    JSONObject option = options.getJSONObject(i);
                    String optionLetter = option.getString("option");
                    String optionContent = option.getString("option_content");
                    // 排查错误项
                    if (wrongAnswers.contains(optionLetter)) {
                        logger.accept(String.format("排除错误选项: %s.%s", optionLetter, optionContent));
                    } else {
                        availableOptionLetters.add(optionLetter);
                        optionsText.append(String.format("%s. %s\n", optionLetter, optionContent));
                    }
                }

                logger.accept("题目详情:\n" + question + optionsText);

                String answer;
                // 如果 availableOptionLetters 只剩下一个选项，自动选择
                if (availableOptionLetters.size() == 1) {
                    answer = availableOptionLetters.get(0);
                    logger.accept("只剩下一个选项，自动选择答案: " + answer);
                } else {
                    checkShouldStop();
                    // 获取答案
                    answer = getQuestionAnswer(question, optionsText.toString(), availableOptionLetters);
                }

                checkShouldStop();

                // 提交答案
                JSONObject result = ApiUtil.submitQuestionAnswer(user.getToken(), questionId, answer,
                        user.getShareUserHid());
                int submitAnswerState = result.getInt("state");
                if (submitAnswerState == 3) { // 答错且未有人帮忙答题
                    wrongAnswers.add(answer);
                    if (historicalCorrectAnswer == answer) {
                        historicalCorrectAnswer = null;
                    }
                    logger.accept("❌ 答题错误");
                } else if (submitAnswerState == 2) { // 答题正确
                    historicalCorrectAnswer = answer;
                    int score = result.getInt("answer_score");
                    logger.accept(String.format("✅ 答题正确 | 积分+%d", score));
                }
            } catch (Exception e) {
                logger.accept("答题失败: " + e.getMessage());
            }
        }

        public void stopTask() {
            running = false;
        }

        public boolean isRunning() {
            return running;
        }

        private void getAnsweredAnswer(User user) throws InterruptedException {
            try {
                JSONObject data = ApiUtil.getQuestionInfo(user.getToken());
                if (data.has("answer")) {
                    String answerText = data.getString("answer");
                    // 从 "C.6个" 格式中提取 "C"
                    if (answerText.matches("[A-D].*")) {
                        String answer = answerText.substring(0, 1);
                        historicalCorrectAnswer = answer;
                        logger.accept(String.format("从已答题账号获取到答案：%s", answer));
                        return;
                    }
                }
                logger.accept("从已答题账号获取答案失败");
            } catch (Exception e) {
                logger.accept("从已答题账号获取问题失败: " + e.getMessage());
            }
        }

        private void getScoreDetails(User user) throws InterruptedException {
            try {
                JSONObject data = ApiUtil.getScore(user.getToken());
                int totalScore = data.getInt("score");

                // 获取今日日期
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(new Date());

                // 获取积分记录列表
                JSONArray records = data.getJSONObject("points_record")
                        .getJSONArray("list");

                // 获取今日积分记录
                List<JSONObject> todayRecords = new ArrayList<>();
                for (int i = 0; i < records.length(); i++) {
                    JSONObject record = records.getJSONObject(i);
                    String createdAt = record.getString("created_at");
                    if (createdAt.startsWith(today)) {
                        todayRecords.add(record);
                    }
                }

                // 计算今日积分变动
                int todayScore = 0;
                for (JSONObject record : todayRecords) {
                    String scoreStr = record.getString("score_str");
                    todayScore += Integer.parseInt(scoreStr.replace("+", ""));
                }

                // 显示积分信息
                String todayScoreStr = todayScore > 0 ? "+" + todayScore : String.valueOf(todayScore);
                logger.accept(String.format("🎉 总积分: %d | 今日积分变动: %s", totalScore, todayScoreStr));

                // 显示今日积分记录
                if (!todayRecords.isEmpty()) {
                    logger.accept("今日积分记录：");
                    for (JSONObject record : todayRecords) {
                        logger.accept(String.format("%s %s %s",
                                record.getString("created_at"),
                                record.getString("desc"),
                                record.getString("score_str")));
                    }
                } else {
                    logger.accept("今日暂无积分变动");
                }
            } catch (Exception e) {
                logger.accept("获取积分信息失败: " + e.getMessage());
            }
        }
    }
}
package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class QuizActivty extends AppCompatActivity {

    SwipeRefreshLayout swipe;

    private static final String PREFS_NAME = "Quiz";
    private static final String KEY_QUESTION_INDEX = "Index";
    private static final String KEY_REMAINING_TIME = "Time";
    private static final String KEY_TIMER_RUNNING = "TimerRunning";
    private static final long TOTAL_TIME = 600000;

    private ArrayList<Question> questions;
    private int qindex;
    private long rtime;
    private boolean timerRunning;

    private TextView qtv, timertv;
    private RadioGroup c;
    private Button nxt;

    private CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_activty);

        swipe = findViewById(R.id.main);

        timertv = findViewById(R.id.countdown);
        qtv = findViewById(R.id.question);
        c = findViewById(R.id.choices);
        nxt = findViewById(R.id.next);




        loadQuestions();
        loadState();
        displayQuestion();
        startTimer();

        nxt.setOnClickListener(v -> {
            if (qindex < questions.size() - 1) {
                nextQuestion();
            } else {
                showScoreDialog();
            }
        });

        swipe.setOnRefreshListener(()->{

            resetQuiz();
            swipe.setRefreshing(false);
        });

        // Change the text of the button to "Submit" on the last question
        if (qindex == questions.size() - 1) {
            nxt.setText("Submit");
        }
    }



    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_QUESTION_INDEX, qindex);
        outState.putLong(KEY_REMAINING_TIME, rtime);
        outState.putBoolean(KEY_TIMER_RUNNING, timerRunning);
    }

    private void startTimer() {
        timer = new CountDownTimer(rtime, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                rtime = millisUntilFinished;
                updateTimerText();
            }

            @Override
            public void onFinish() {
                showScoreDialog();
                clearState();
                finish();
            }
        }.start();
    }

    private void updateTimerText() {
        int min = (int) (rtime / 60000);
        int secs = (int) (rtime % 60000 / 1000);
        String time = String.format("%02d:%02d", min, secs);
        timertv.setText(time);
    }

    private void loadQuestions() {
        questions = new ArrayList<>();
        try {
            InputStream is = getAssets().open("questions.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");
            JSONObject job = new JSONObject(json);
            JSONArray jarr = job.getJSONArray("questions");

            for (int i = 0; i < jarr.length(); i++) {
                JSONObject qobj = jarr.getJSONObject(i);
                String question = qobj.getString("question");
                JSONArray carr = qobj.getJSONArray("choices");

                ArrayList<String> choices = new ArrayList<>();
                for (int j = 0; j < carr.length(); j++) {
                    choices.add(carr.getString(j));
                }

                String answer = qobj.getString("answer");
                questions.add(new Question(question, choices, answer));
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        qindex = prefs.getInt(KEY_QUESTION_INDEX, 0);
        rtime = prefs.getLong(KEY_REMAINING_TIME, TOTAL_TIME);
        timerRunning = prefs.getBoolean(KEY_TIMER_RUNNING, false);
    }

    private void saveState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_QUESTION_INDEX, qindex);
        editor.putLong(KEY_REMAINING_TIME, rtime);
        editor.putBoolean(KEY_TIMER_RUNNING, timerRunning);
        editor.apply();
    }

    private void displayQuestion() {
        if (qindex < questions.size()) {
            Question cq = questions.get(qindex);
            qtv.setText(cq.getQuestion());
            c.removeAllViews();
            for (String choice : cq.getChoices()) {
                RadioButton rb = new RadioButton(this);
                rb.setText(choice);
                rb.setOnClickListener(v -> cq.setSelectedAnswer(choice));
                c.addView(rb);
            }
        } else {
            showScoreDialog();
        }
    }

    private void nextQuestion() {
        if (qindex < questions.size() - 1) {
            qindex++;
            displayQuestion();
            // Change the text of the button to "Submit" on the last question
            if (qindex == questions.size() - 1) {
                nxt.setText("Submit");
            }
        }
    }

    private void showScoreDialog() {
        int correctAnswers = 0;
        for (Question question : questions) {
            if (question.isCorrect()) {
                correctAnswers++;
            }
        }
        float score = (correctAnswers / (float) questions.size()) * 10;

        // Reset the timer back to its original duration
        rtime = TOTAL_TIME;
        updateTimerText();

        // Create custom dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.custom_dialog, null);
        builder.setView(dialogView);

        TextView scoreTextView = dialogView.findViewById(R.id.score);
        scoreTextView.setText("Your Score: " + score + " / 10");

        Button okButton = dialogView.findViewById(R.id.ok);
        AlertDialog dialog = builder.create();
        okButton.setOnClickListener(v -> {
            dialog.dismiss();
            resetQuiz(); // Reset quiz state
            navigateToMainActivity();
        });

        dialog.show();
    }

    private void resetQuiz() {
        qindex = 0;
        loadQuestions(); // Reload questions
        rtime = TOTAL_TIME; // Reset timer to original duration
        saveState(); // Save initial state
        if (timer != null) {
            timer.cancel();
        }
        startTimer(); // Start timer again
    }

    private void clearState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor edt = prefs.edit();
        edt.clear();
        edt.apply();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveState();
        if (timer != null) {
            timer.cancel();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startTimer();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity(); // Close the application when back is pressed
    }

}

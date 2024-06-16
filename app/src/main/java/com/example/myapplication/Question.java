package com.example.myapplication;

import java.util.ArrayList;

public class Question {
    private String question;
    private ArrayList<String> choices;
    private String answer;
    private String selectedAnswer;

    public Question(String question, ArrayList<String> choices, String answer) {
        this.question = question;
        this.choices = choices;
        this.answer = answer;
        this.selectedAnswer = null;
    }

    public String getQuestion() {
        return question;
    }

    public ArrayList<String> getChoices() {
        return choices;
    }

    public String getAnswer() {
        return answer;
    }

    public void setSelectedAnswer(String selectedAnswer) {
        this.selectedAnswer = selectedAnswer;
    }

    public boolean isCorrect() {
        return answer.equals(selectedAnswer);
    }
}


package com.example.ragApp.dto;

public class PriorQuestionResponse {

    private String question;
    private String questionKey;
    private String questionType;
    private Integer questionOrder;

    public PriorQuestionResponse(String question, String questionKey, String questionType, Integer questionOrder) {
        this.question = question;
        this.questionKey = questionKey;
        this.questionType = questionType;
        this.questionOrder = questionOrder;
    }

    public String getQuestion() { return question; }
    public String getQuestionKey() { return questionKey; }
    public String getQuestionType() { return questionType; }
    public Integer getQuestionOrder() { return questionOrder; }
}



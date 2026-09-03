package com.learnpath.version1.dto;

public record QuestionDto (Long id, String questionText,
                           String optionA, String optionB,
                           String optionC, String optionD){}

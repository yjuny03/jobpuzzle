package com.example.jobpuzzle.interview.service;

import java.util.Locale;
import java.util.regex.Pattern;

final class AnswerTextValidity {

    private static final int MINIMUM_SUBSTANTIVE_LENGTH = 6;
    private static final Pattern REPEATED_CHARACTER = Pattern.compile("(.)\\1{2,}");
    private static final Pattern ABUSIVE_LANGUAGE = Pattern.compile(
            "씨발|시발|병신|좆|개새끼|닥쳐|꺼져",
            Pattern.CASE_INSENSITIVE
    );

    private AnswerTextValidity() {
    }

    static boolean isUnusable(String answer) {
        String normalized = answer == null ? "" : answer
                .replaceAll("[\\s\\p{Punct}]", "")
                .toLowerCase(Locale.ROOT);
        return normalized.length() < MINIMUM_SUBSTANTIVE_LENGTH
                || REPEATED_CHARACTER.matcher(normalized).find()
                || ABUSIVE_LANGUAGE.matcher(normalized).find();
    }
}

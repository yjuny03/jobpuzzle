package com.example.jobpuzzle.evaluation.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class ScoreAggregationCalculator {

    private ScoreAggregationCalculator() {
    }

    static Result aggregate(List<Map<String, Integer>> evaluationScores) {
        Map<String, List<Integer>> values = new LinkedHashMap<>();
        evaluationScores.forEach(evaluation ->
                evaluation.forEach((dimension, score) -> {
                    if (score != null) {
                        values.computeIfAbsent(dimension, ignored -> new ArrayList<>()).add(score);
                    }
                })
        );

        Map<String, Integer> scores = values.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> roundedAverage(entry.getValue()),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        Map<String, Integer> counts = values.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().size(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        return new Result(scores, counts);
    }

    private static int roundedAverage(List<Integer> values) {
        return (int) Math.round(values.stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    record Result(
            Map<String, Integer> scores,
            Map<String, Integer> counts
    ) {
    }
}

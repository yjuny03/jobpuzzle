package com.example.jobpuzzle.evaluation.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ScoreAggregationCalculatorTest {

    @Test
    void 동일_관점의_질답별_점수_평균과_평가횟수를_함께_계산한다() {
        ScoreAggregationCalculator.Result result =
                ScoreAggregationCalculator.aggregate(List.of(
                        Map.of("intentMatch", 60, "guideAlignment", 70),
                        Map.of("intentMatch", 80, "guideAlignment", 76)
                ));

        assertThat(result.scores())
                .containsEntry("intentMatch", 70)
                .containsEntry("guideAlignment", 73);
        assertThat(result.counts())
                .containsEntry("intentMatch", 2)
                .containsEntry("guideAlignment", 2);
    }

    @Test
    void 해당_질답에서_평가하지_않은_관점은_평균의_분모에서_제외한다() {
        ScoreAggregationCalculator.Result result =
                ScoreAggregationCalculator.aggregate(List.of(
                        Map.of("specificity", 50),
                        Map.of("specificity", 70, "ownRole", 90)
                ));

        assertThat(result.scores())
                .containsEntry("specificity", 60)
                .containsEntry("ownRole", 90);
        assertThat(result.counts())
                .containsEntry("specificity", 2)
                .containsEntry("ownRole", 1);
    }
}

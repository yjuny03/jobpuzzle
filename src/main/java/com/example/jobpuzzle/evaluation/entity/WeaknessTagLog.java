package com.example.jobpuzzle.evaluation.entity;

import com.example.jobpuzzle.global.common.BaseTimeEntity;
import com.example.jobpuzzle.interview.entity.InterviewSession;
import com.example.jobpuzzle.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "weakness_tag_log", uniqueConstraints = {
        @UniqueConstraint(name = "uk_weakness_log_evaluation_tag", columnNames = {"evaluation_id", "tag"})
})
public class WeaknessTagLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_log_id")
    private Long tagLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private AnswerEvaluation evaluation;

    @Column(name = "tag", nullable = false, length = 100)
    private String tag;

    @Enumerated(EnumType.STRING)
    @Column(name = "occurrence_type", nullable = false, length = 20)
    private WeaknessOccurrenceType occurrenceType;

    public static WeaknessTagLog create(
            User user,
            InterviewSession session,
            AnswerEvaluation evaluation,
            String tag,
            WeaknessOccurrenceType occurrenceType
    ) {
        WeaknessTagLog log = new WeaknessTagLog();
        log.user = user;
        log.session = session;
        log.evaluation = evaluation;
        log.tag = tag;
        log.occurrenceType = occurrenceType;
        return log;
    }
}

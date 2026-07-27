package com.example.jobpuzzle.evaluation.entity;

import com.example.jobpuzzle.interview.entity.InterviewSession;
import com.example.jobpuzzle.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "weakness_tag_status", uniqueConstraints = {
        @UniqueConstraint(name = "uk_weakness_status_user_tag", columnNames = {"user_id", "tag"})
})
public class WeaknessTagStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_id")
    private Long weaknessStatusId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "tag", nullable = false, length = 100)
    private String tag;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WeaknessTagResolveStatus status;

    @Column(name = "first_occurred_at", nullable = false)
    private LocalDateTime firstOccurredAt;

    @Column(name = "last_occurred_at", nullable = false)
    private LocalDateTime lastOccurredAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_session_id")
    private InterviewSession resolvedBySession;

    @Column(name = "last_updated_at", nullable = false)
    private LocalDateTime lastUpdatedAt;

    public static WeaknessTagStatus detected(User user, String tag) {
        LocalDateTime now = LocalDateTime.now();
        WeaknessTagStatus value = new WeaknessTagStatus();
        value.user = user;
        value.tag = tag;
        value.status = WeaknessTagResolveStatus.UNRESOLVED;
        value.firstOccurredAt = now;
        value.lastOccurredAt = now;
        value.lastUpdatedAt = now;
        return value;
    }

    public WeaknessOccurrenceType recur() {
        LocalDateTime now = LocalDateTime.now();
        WeaknessOccurrenceType occurrenceType = status == WeaknessTagResolveStatus.RESOLVED
                ? WeaknessOccurrenceType.RECURRED
                : WeaknessOccurrenceType.DETECTED;
        status = WeaknessTagResolveStatus.UNRESOLVED;
        lastOccurredAt = now;
        lastUpdatedAt = now;
        resolvedAt = null;
        resolvedBySession = null;
        return occurrenceType;
    }

    public void resolve(InterviewSession session) {
        status = WeaknessTagResolveStatus.RESOLVED;
        resolvedAt = LocalDateTime.now();
        resolvedBySession = session;
        lastUpdatedAt = resolvedAt;
    }
}

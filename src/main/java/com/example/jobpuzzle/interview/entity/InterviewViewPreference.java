package com.example.jobpuzzle.interview.entity;

import com.example.jobpuzzle.global.common.BaseTimeEntity;
import com.example.jobpuzzle.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "interview_view_preference", uniqueConstraints = {
        @UniqueConstraint(name = "uk_interview_view_preference_user", columnNames = "user_id")
})
public class InterviewViewPreference extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preference_id")
    private Long preferenceId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "section_order", nullable = false, columnDefinition = "json")
    private List<InterviewSectionKey> sectionOrder;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static InterviewViewPreference create(User user) {
        InterviewViewPreference preference = new InterviewViewPreference();
        preference.user = user;
        preference.sectionOrder = InterviewViewPreferenceDefaults.SECTION_ORDER;
        preference.updatedAt = LocalDateTime.now();
        return preference;
    }

    public void updateSectionOrder(List<InterviewSectionKey> sectionOrder) {
        this.sectionOrder = List.copyOf(sectionOrder);
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void touchUpdatedAt() {
        updatedAt = LocalDateTime.now();
    }
}

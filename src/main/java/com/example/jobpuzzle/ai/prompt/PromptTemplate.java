package com.example.jobpuzzle.ai.prompt;

import com.example.jobpuzzle.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(
        name = "prompt_template",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_prompt_template_code_version",
                        columnNames = {"prompt_code", "version"}
                )
        }
)
public class PromptTemplate extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prompt_template_id")
    private Long promptTemplateId;

    @Column(name = "prompt_code", nullable = false, length = 50)
    private String promptCode;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "version", nullable = false, length = 20)
    private String version;

    @Column(name = "target_json", nullable = false, length = 20)
    private String targetJson;

    @Lob
    @Column(name = "template_text", nullable = false, columnDefinition = "LONGTEXT")
    private String templateText;

    @Lob
    @Column(name = "forbidden_rules", columnDefinition = "TEXT")
    private String forbiddenRules;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Builder
    private PromptTemplate(
            String promptCode,
            String name,
            String version,
            String targetJson,
            String templateText,
            String forbiddenRules,
            Boolean isActive
    ) {
        this.promptCode = promptCode;
        this.name = name;
        this.version = version == null ? "v1.0" : version;
        this.targetJson = targetJson;
        this.templateText = templateText;
        this.forbiddenRules = forbiddenRules;
        this.isActive = isActive == null || isActive;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
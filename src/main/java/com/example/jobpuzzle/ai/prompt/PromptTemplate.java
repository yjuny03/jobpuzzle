package com.example.jobpuzzle.ai.prompt;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "prompt_template")
public class PromptTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long promptTemplateId;

    private String promptCode;

    private String name;

    private Integer version;

    private String targetJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String templateText;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String forbiddenRules;

    private Boolean isActive;

    private LocalDateTime createdAt;

}

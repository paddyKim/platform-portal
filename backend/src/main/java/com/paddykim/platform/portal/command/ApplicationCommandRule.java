package com.paddykim.platform.portal.command;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "application_command_rules")
public class ApplicationCommandRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String intent;

    @Column(nullable = false)
    private String view;

    @Column(nullable = false)
    private String message;

    @Lob
    @Column(name = "target_terms", nullable = false)
    private String targetTerms;

    @Lob
    @Column(name = "action_terms", nullable = false)
    private String actionTerms;

    @Column(name = "result_api_method")
    private String resultApiMethod;

    @Column(name = "result_api_path")
    private String resultApiPath;

    @Column(nullable = false)
    private double confidence;

    @Column(nullable = false)
    private int priority;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ApplicationCommandRule() {
    }

    public ApplicationCommandRule(
            String name,
            String intent,
            String view,
            String message,
            String targetTerms,
            String actionTerms,
            String resultApiMethod,
            String resultApiPath,
            double confidence,
            int priority
    ) {
        this.name = name;
        this.intent = intent;
        this.view = view;
        this.message = message;
        this.targetTerms = targetTerms;
        this.actionTerms = actionTerms;
        this.resultApiMethod = resultApiMethod;
        this.resultApiPath = resultApiPath;
        this.confidence = confidence;
        this.priority = priority;
        this.enabled = true;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void updateFrom(ApplicationCommandRule rule) {
        this.intent = rule.intent;
        this.view = rule.view;
        this.message = rule.message;
        this.targetTerms = rule.targetTerms;
        this.actionTerms = rule.actionTerms;
        this.resultApiMethod = rule.resultApiMethod;
        this.resultApiPath = rule.resultApiPath;
        this.confidence = rule.confidence;
        this.priority = rule.priority;
        this.enabled = true;
        this.updatedAt = Instant.now();
    }

    public String getName() {
        return name;
    }

    public String getIntent() {
        return intent;
    }

    public String getView() {
        return view;
    }

    public String getMessage() {
        return message;
    }

    public String getTargetTerms() {
        return targetTerms;
    }

    public String getActionTerms() {
        return actionTerms;
    }

    public String getResultApiMethod() {
        return resultApiMethod;
    }

    public String getResultApiPath() {
        return resultApiPath;
    }

    public double getConfidence() {
        return confidence;
    }

    public int getPriority() {
        return priority;
    }
}

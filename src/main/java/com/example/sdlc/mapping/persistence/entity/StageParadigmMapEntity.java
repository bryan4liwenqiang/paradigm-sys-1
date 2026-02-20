package com.example.sdlc.mapping.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "stage_paradigm_map",
        uniqueConstraints = @UniqueConstraint(columnNames = {"stage_id", "paradigm_id"})
)
public class StageParadigmMapEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stage_id", nullable = false)
    private Long stageId;

    @Column(name = "paradigm_id", nullable = false)
    private Long paradigmId;

    @Column(nullable = false)
    private Integer priority;

    @Column(name = "condition_expr", columnDefinition = "text")
    private String conditionExpr;

    @Column(nullable = false)
    private Boolean enabled;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStageId() {
        return stageId;
    }

    public void setStageId(Long stageId) {
        this.stageId = stageId;
    }

    public Long getParadigmId() {
        return paradigmId;
    }

    public void setParadigmId(Long paradigmId) {
        this.paradigmId = paradigmId;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getConditionExpr() {
        return conditionExpr;
    }

    public void setConditionExpr(String conditionExpr) {
        this.conditionExpr = conditionExpr;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}

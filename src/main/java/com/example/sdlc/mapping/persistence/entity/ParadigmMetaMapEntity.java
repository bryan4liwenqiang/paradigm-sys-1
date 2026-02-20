package com.example.sdlc.mapping.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "paradigm_meta_map",
        uniqueConstraints = @UniqueConstraint(columnNames = {"paradigm_id", "meta_methodology_id"})
)
public class ParadigmMetaMapEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paradigm_id", nullable = false)
    private Long paradigmId;

    @Column(name = "meta_methodology_id", nullable = false)
    private Long metaMethodologyId;

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

    public Long getParadigmId() {
        return paradigmId;
    }

    public void setParadigmId(Long paradigmId) {
        this.paradigmId = paradigmId;
    }

    public Long getMetaMethodologyId() {
        return metaMethodologyId;
    }

    public void setMetaMethodologyId(Long metaMethodologyId) {
        this.metaMethodologyId = metaMethodologyId;
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

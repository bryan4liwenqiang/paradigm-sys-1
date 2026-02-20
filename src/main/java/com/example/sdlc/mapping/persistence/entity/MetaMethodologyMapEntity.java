package com.example.sdlc.mapping.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "meta_methodology_map",
        uniqueConstraints = @UniqueConstraint(columnNames = {"meta_methodology_id", "methodology_id"})
)
public class MetaMethodologyMapEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meta_methodology_id", nullable = false)
    private Long metaMethodologyId;

    @Column(name = "methodology_id", nullable = false)
    private Long methodologyId;

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

    public Long getMetaMethodologyId() {
        return metaMethodologyId;
    }

    public void setMetaMethodologyId(Long metaMethodologyId) {
        this.metaMethodologyId = metaMethodologyId;
    }

    public Long getMethodologyId() {
        return methodologyId;
    }

    public void setMethodologyId(Long methodologyId) {
        this.methodologyId = methodologyId;
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

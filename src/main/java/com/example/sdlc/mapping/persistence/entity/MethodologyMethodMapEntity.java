package com.example.sdlc.mapping.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "methodology_method_map",
        uniqueConstraints = @UniqueConstraint(columnNames = {"methodology_id", "method_id", "seq_no"})
)
public class MethodologyMethodMapEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "methodology_id", nullable = false)
    private Long methodologyId;

    @Column(name = "method_id", nullable = false)
    private Long methodId;

    @Column(name = "seq_no", nullable = false)
    private Integer seqNo;

    @Column(name = "required_flag", nullable = false)
    private Boolean requiredFlag;

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

    public Long getMethodologyId() {
        return methodologyId;
    }

    public void setMethodologyId(Long methodologyId) {
        this.methodologyId = methodologyId;
    }

    public Long getMethodId() {
        return methodId;
    }

    public void setMethodId(Long methodId) {
        this.methodId = methodId;
    }

    public Integer getSeqNo() {
        return seqNo;
    }

    public void setSeqNo(Integer seqNo) {
        this.seqNo = seqNo;
    }

    public Boolean getRequiredFlag() {
        return requiredFlag;
    }

    public void setRequiredFlag(Boolean requiredFlag) {
        this.requiredFlag = requiredFlag;
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

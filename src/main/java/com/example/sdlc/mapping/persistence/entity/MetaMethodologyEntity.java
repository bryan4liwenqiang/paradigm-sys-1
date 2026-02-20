package com.example.sdlc.mapping.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "meta_methodologies")
public class MetaMethodologyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "meta_goal", columnDefinition = "text")
    private String metaGoal;

    @Column(name = "meta_object", columnDefinition = "text")
    private String metaObject;

    @Column(name = "meta_mechanism", columnDefinition = "text")
    private String metaMechanism;

    @Column(name = "meta_constraint", columnDefinition = "text")
    private String metaConstraint;

    @Column(name = "meta_metric", columnDefinition = "text")
    private String metaMetric;

    @Column(name = "meta_gate", columnDefinition = "text")
    private String metaGate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMetaGoal() {
        return metaGoal;
    }

    public void setMetaGoal(String metaGoal) {
        this.metaGoal = metaGoal;
    }

    public String getMetaObject() {
        return metaObject;
    }

    public void setMetaObject(String metaObject) {
        this.metaObject = metaObject;
    }

    public String getMetaMechanism() {
        return metaMechanism;
    }

    public void setMetaMechanism(String metaMechanism) {
        this.metaMechanism = metaMechanism;
    }

    public String getMetaConstraint() {
        return metaConstraint;
    }

    public void setMetaConstraint(String metaConstraint) {
        this.metaConstraint = metaConstraint;
    }

    public String getMetaMetric() {
        return metaMetric;
    }

    public void setMetaMetric(String metaMetric) {
        this.metaMetric = metaMetric;
    }

    public String getMetaGate() {
        return metaGate;
    }

    public void setMetaGate(String metaGate) {
        this.metaGate = metaGate;
    }
}

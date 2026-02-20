package com.example.sdlc.mapping.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "methodologies")
public class MethodologyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 32)
    private String version;

    @Column(name = "template_ref", columnDefinition = "text")
    private String templateRef;

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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTemplateRef() {
        return templateRef;
    }

    public void setTemplateRef(String templateRef) {
        this.templateRef = templateRef;
    }
}

package com.example.sdlc.mapping.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "methods")
public class MethodEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "method_type", length = 64)
    private String methodType;

    @Column(name = "tool_ref", length = 128)
    private String toolRef;

    @Column(name = "input_schema", columnDefinition = "jsonb")
    private String inputSchema;

    @Column(name = "output_schema", columnDefinition = "jsonb")
    private String outputSchema;

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

    public String getMethodType() {
        return methodType;
    }

    public void setMethodType(String methodType) {
        this.methodType = methodType;
    }

    public String getToolRef() {
        return toolRef;
    }

    public void setToolRef(String toolRef) {
        this.toolRef = toolRef;
    }

    public String getInputSchema() {
        return inputSchema;
    }

    public void setInputSchema(String inputSchema) {
        this.inputSchema = inputSchema;
    }

    public String getOutputSchema() {
        return outputSchema;
    }

    public void setOutputSchema(String outputSchema) {
        this.outputSchema = outputSchema;
    }
}

package com.example.sdlc.mapping.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.HashMap;
import java.util.Map;

public class MappingRecommendRequest {

    @NotNull
    private Long projectId;

    @NotBlank
    private String stageCode;

    private Map<String, Object> context = new HashMap<>();

    private Limit limit = new Limit();

    public int limitParadigms() {
        return limit == null ? 3 : limit.getParadigms();
    }

    public int limitMetaMethodologies() {
        return limit == null ? 5 : limit.getMetaMethodologies();
    }

    public int limitMethodologies() {
        return limit == null ? 5 : limit.getMethodologies();
    }

    public int limitMethods() {
        return limit == null ? 20 : limit.getMethods();
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getStageCode() {
        return stageCode;
    }

    public void setStageCode(String stageCode) {
        this.stageCode = stageCode;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    public Limit getLimit() {
        return limit;
    }

    public void setLimit(Limit limit) {
        this.limit = limit;
    }

    public static class Limit {
        private int paradigms = 3;
        private int metaMethodologies = 5;
        private int methodologies = 5;
        private int methods = 20;

        public int getParadigms() {
            return paradigms;
        }

        public void setParadigms(int paradigms) {
            this.paradigms = paradigms;
        }

        public int getMetaMethodologies() {
            return metaMethodologies;
        }

        public void setMetaMethodologies(int metaMethodologies) {
            this.metaMethodologies = metaMethodologies;
        }

        public int getMethodologies() {
            return methodologies;
        }

        public void setMethodologies(int methodologies) {
            this.methodologies = methodologies;
        }

        public int getMethods() {
            return methods;
        }

        public void setMethods(int methods) {
            this.methods = methods;
        }
    }
}

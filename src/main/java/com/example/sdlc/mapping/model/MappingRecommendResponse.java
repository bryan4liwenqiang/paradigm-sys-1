package com.example.sdlc.mapping.model;

import java.util.ArrayList;
import java.util.List;

public class MappingRecommendResponse {
    private String stage;
    private List<NamedRef> recommendedParadigms = new ArrayList<>();
    private List<NamedRef> recommendedMetaMethodologies = new ArrayList<>();
    private List<NamedRef> recommendedMethodologies = new ArrayList<>();
    private List<NamedRef> recommendedMethods = new ArrayList<>();
    private Trace trace = new Trace();

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public List<NamedRef> getRecommendedParadigms() {
        return recommendedParadigms;
    }

    public void setRecommendedParadigms(List<NamedRef> recommendedParadigms) {
        this.recommendedParadigms = recommendedParadigms;
    }

    public List<NamedRef> getRecommendedMetaMethodologies() {
        return recommendedMetaMethodologies;
    }

    public void setRecommendedMetaMethodologies(List<NamedRef> recommendedMetaMethodologies) {
        this.recommendedMetaMethodologies = recommendedMetaMethodologies;
    }

    public List<NamedRef> getRecommendedMethodologies() {
        return recommendedMethodologies;
    }

    public void setRecommendedMethodologies(List<NamedRef> recommendedMethodologies) {
        this.recommendedMethodologies = recommendedMethodologies;
    }

    public List<NamedRef> getRecommendedMethods() {
        return recommendedMethods;
    }

    public void setRecommendedMethods(List<NamedRef> recommendedMethods) {
        this.recommendedMethods = recommendedMethods;
    }

    public Trace getTrace() {
        return trace;
    }

    public void setTrace(Trace trace) {
        this.trace = trace;
    }
}

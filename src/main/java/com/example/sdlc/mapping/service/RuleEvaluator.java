package com.example.sdlc.mapping.service;

import java.util.Map;

public interface RuleEvaluator {
    boolean evaluate(String expression, Map<String, Object> context);
}

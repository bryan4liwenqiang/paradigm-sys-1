package com.example.sdlc.mapping.service;

import com.googlecode.aviator.AviatorEvaluator;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AviatorRuleEvaluator implements RuleEvaluator {

    @Override
    public boolean evaluate(String expression, Map<String, Object> context) {
        if (expression == null || expression.isBlank()) {
            return true;
        }
        try {
            Object result = AviatorEvaluator.execute(expression, context, true);
            return result instanceof Boolean && (Boolean) result;
        } catch (Exception ex) {
            return false;
        }
    }
}

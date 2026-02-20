package com.example.sdlc.mapping.controller;

import com.example.sdlc.mapping.model.MappingRecommendRequest;
import com.example.sdlc.mapping.model.MappingRecommendResponse;
import com.example.sdlc.mapping.service.MappingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mapping")
public class MappingController {

    private final MappingService mappingService;

    public MappingController(MappingService mappingService) {
        this.mappingService = mappingService;
    }

    @PostMapping("/recommend")
    public MappingRecommendResponse recommend(@Valid @RequestBody MappingRecommendRequest request) {
        return mappingService.recommend(request);
    }
}

package org.com.repair.controller;

import jakarta.validation.Valid;
import org.com.repair.DTO.AIDiagnosisRequest;
import org.com.repair.DTO.AIDiagnosisResponse;
import org.com.repair.security.RequestUserContextResolver;
import org.com.repair.service.AIDiagnosisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping({"/api/ai-diagnosis", "/api/diagnosis"})
public class AIDiagnosisController {

    private static final int MAX_IMAGE_COUNT = 3;

    private final AIDiagnosisService aiDiagnosisService;
    private final RequestUserContextResolver requestUserContextResolver;

    public AIDiagnosisController(AIDiagnosisService aiDiagnosisService,
                                 RequestUserContextResolver requestUserContextResolver) {
        this.aiDiagnosisService = aiDiagnosisService;
        this.requestUserContextResolver = requestUserContextResolver;
    }

    @PostMapping({"", "/diagnose"})
    public ResponseEntity<AIDiagnosisResponse> diagnoseFault(@Valid @RequestBody AIDiagnosisRequest request,
                                                             HttpServletRequest servletRequest) {
        String role = requestUserContextResolver.requireRole(servletRequest);
        if (!"customer".equals(role) && !"technician".equals(role) && !"admin".equals(role)) {
            throw new AccessDeniedException("当前角色无权调用AI故障诊断");
        }

        String normalizedProblemDescription = request.getProblemDescription() == null
                ? ""
                : request.getProblemDescription().trim();
        List<String> normalizedImageDataUrls = normalizeImageDataUrls(request.getImageDataUrls());
        if (normalizedProblemDescription.isBlank() && normalizedImageDataUrls.isEmpty()) {
            throw new IllegalArgumentException("请至少输入文字描述或上传故障图片");
        }

        String diagnosisRole = ("technician".equals(role) || "admin".equals(role)) ? "technician" : "customer";

        AIDiagnosisResponse response = aiDiagnosisService.diagnoseFault(
                normalizedProblemDescription,
                diagnosisRole,
                request.getTechnicianId(),
                normalizedImageDataUrls);
        
        if (response.isSuccess()) {
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private List<String> normalizeImageDataUrls(List<String> imageDataUrls) {
        if (imageDataUrls == null || imageDataUrls.isEmpty()) {
            return List.of();
        }

        List<String> normalized = new ArrayList<>();
        for (String item : imageDataUrls) {
            if (item == null) {
                continue;
            }

            String trimmed = item.trim();
            if (!trimmed.isBlank()) {
                normalized.add(trimmed);
            }

            if (normalized.size() >= MAX_IMAGE_COUNT) {
                break;
            }
        }

        return normalized;
    }
}

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

@RestController
@RequestMapping({"/api/ai-diagnosis", "/api/diagnosis"})
public class AIDiagnosisController {

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
        String diagnosisRole = ("technician".equals(role) || "admin".equals(role)) ? "technician" : "customer";

        AIDiagnosisResponse response = aiDiagnosisService.diagnoseFault(
                request.getProblemDescription(),
                diagnosisRole,
                request.getTechnicianId());
        
        if (response.isSuccess()) {
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

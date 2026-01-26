package org.com.repair.controller;

import jakarta.validation.Valid;
import org.com.repair.DTO.AIDiagnosisRequest;
import org.com.repair.DTO.AIDiagnosisResponse;
import org.com.repair.service.AIDiagnosisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai-diagnosis")
public class AIDiagnosisController {

    private final AIDiagnosisService aiDiagnosisService;

    public AIDiagnosisController(AIDiagnosisService aiDiagnosisService) {
        this.aiDiagnosisService = aiDiagnosisService;
    }

    @PostMapping("/diagnose")
    public ResponseEntity<AIDiagnosisResponse> diagnoseFault(@Valid @RequestBody AIDiagnosisRequest request) {
        AIDiagnosisResponse response = aiDiagnosisService.diagnoseFault(request.getProblemDescription());
        
        if (response.isSuccess()) {
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

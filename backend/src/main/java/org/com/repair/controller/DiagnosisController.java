package org.com.repair.controller;

import org.com.repair.DTO.DiagnosisRequest;
import org.com.repair.DTO.DiagnosisResponse;
import org.com.repair.service.IntelligentDiagnosisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 智能诊断控制器
 * 提供故障诊断的API接口
 */
@RestController
@RequestMapping("/api/diagnosis")
public class DiagnosisController {
    
    private final IntelligentDiagnosisService diagnosisService;
    
    public DiagnosisController(IntelligentDiagnosisService diagnosisService) {
        this.diagnosisService = diagnosisService;
    }
    
    /**
     * 执行智能故障诊断
     * @param request 诊断请求，包含故障描述和车辆信息
     * @return 诊断结果
     */
    @PostMapping("/analyze")
    public ResponseEntity<DiagnosisResponse> analyzeFault(@RequestBody DiagnosisRequest request) {
        try {
            if (request.description() == null || request.description().trim().isEmpty()) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            
            DiagnosisResponse response = diagnosisService.diagnose(request);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

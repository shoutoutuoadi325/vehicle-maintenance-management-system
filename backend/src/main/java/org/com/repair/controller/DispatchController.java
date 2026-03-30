package org.com.repair.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.com.repair.DTO.DispatchBoardTechnicianResponse;
import org.com.repair.security.RequestUserContextResolver;
import org.com.repair.service.RepairOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dispatch")
public class DispatchController {

    private final RepairOrderService repairOrderService;
    private final RequestUserContextResolver requestUserContextResolver;

    public DispatchController(RepairOrderService repairOrderService,
                              RequestUserContextResolver requestUserContextResolver) {
        this.repairOrderService = repairOrderService;
        this.requestUserContextResolver = requestUserContextResolver;
    }

    @GetMapping("/board")
    public ResponseEntity<List<DispatchBoardTechnicianResponse>> getDispatchBoard(HttpServletRequest request) {
        requestUserContextResolver.requireAdminRole(request);
        return ResponseEntity.ok(repairOrderService.getDispatchBoard());
    }
}

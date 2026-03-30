package org.com.repair.DTO;

import java.util.List;

public record DispatchBoardTechnicianResponse(
        Long id,
        String name,
        String employeeId,
        String skillType,
        Double hourlyRate,
        Integer activeOrderCount,
        Double totalWorkHours,
        Integer completedOrders,
        List<DispatchBoardOrderResponse> orders
) {
}

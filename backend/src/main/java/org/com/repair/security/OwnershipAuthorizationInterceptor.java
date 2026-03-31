package org.com.repair.security;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.com.repair.entity.Feedback;
import org.com.repair.entity.MaintenanceAlert;
import org.com.repair.entity.RepairOrder;
import org.com.repair.entity.Vehicle;
import org.com.repair.repository.FeedbackRepository;
import org.com.repair.repository.MaintenanceAlertRepository;
import org.com.repair.repository.RepairOrderRepository;
import org.com.repair.repository.VehicleRepository;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OwnershipAuthorizationInterceptor implements HandlerInterceptor {

    private static final Pattern VEHICLE_ID_PATH = Pattern.compile("^/api/vehicles/(\\d+)$");
    private static final Pattern REPAIR_ORDER_ID_PATH = Pattern.compile("^/api/repair-orders/(\\d+)(/.*)?$");
    private static final Pattern FEEDBACK_ID_PATH = Pattern.compile("^/api/feedbacks/(\\d+)$");
    private static final Pattern MAINTENANCE_ALERT_ID_PATH = Pattern.compile("^/api/maintenance-alerts/(\\d+)(/.*)?$");
    private static final Pattern USER_SCOPED_PATH = Pattern.compile("^/api/(vehicles|repair-orders|feedbacks|maintenance-alerts)/user/(\\d+)(/.*)?$");
    private static final Pattern TECHNICIAN_SCOPED_PATH = Pattern.compile("^/api/repair-orders/technician/(\\d+)$");

    private final VehicleRepository vehicleRepository;
    private final RepairOrderRepository repairOrderRepository;
    private final FeedbackRepository feedbackRepository;
    private final MaintenanceAlertRepository maintenanceAlertRepository;

    public OwnershipAuthorizationInterceptor(VehicleRepository vehicleRepository,
                                             RepairOrderRepository repairOrderRepository,
                                             FeedbackRepository feedbackRepository,
                                             MaintenanceAlertRepository maintenanceAlertRepository) {
        this.vehicleRepository = vehicleRepository;
        this.repairOrderRepository = repairOrderRepository;
        this.feedbackRepository = feedbackRepository;
        this.maintenanceAlertRepository = maintenanceAlertRepository;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {
        String method = request.getMethod();
        if (method != null && HttpMethod.OPTIONS.matches(method)) {
            return true;
        }

        String role = String.valueOf(request.getAttribute("authRole")).toLowerCase();
        Long authUserId = (Long) request.getAttribute("authUserId");
        String path = request.getRequestURI();

        if ("admin".equals(role)) {
            return true;
        }

        if (authUserId == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "登录态缺失");
            return false;
        }

        if ("customer".equals(role)) {
            if (!checkCustomerOwnership(path, authUserId, request, response)) {
                return false;
            }
        }

        if ("technician".equals(role)) {
            if (!checkTechnicianOwnership(path, authUserId, response)) {
                return false;
            }
        }

        return true;
    }

    private boolean checkCustomerOwnership(String path,
                                           Long authUserId,
                                           HttpServletRequest request,
                                           HttpServletResponse response) throws Exception {
        Matcher userScoped = USER_SCOPED_PATH.matcher(path);
        if (userScoped.matches()) {
            Long pathUserId = Long.parseLong(userScoped.group(2));
            if (!authUserId.equals(pathUserId)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "禁止访问其他用户的资源");
                return false;
            }
            return true;
        }

        Matcher maintenanceAlertMatcher = MAINTENANCE_ALERT_ID_PATH.matcher(path);
        if (maintenanceAlertMatcher.matches()) {
            Long alertId = Long.parseLong(maintenanceAlertMatcher.group(1));
            Optional<MaintenanceAlert> alert = maintenanceAlertRepository.findById(alertId);
            if (alert.isPresent() && !authUserId.equals(alert.get().getUserId())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "禁止访问其他用户保养提醒");
                return false;
            }
            return true;
        }

        if (path.startsWith("/api/feedbacks/check-feedback")) {
            String userIdParam = request.getParameter("userId");
            if (userIdParam != null && !authUserId.equals(Long.parseLong(userIdParam))) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "禁止校验其他用户反馈");
                return false;
            }
            return true;
        }

        Matcher vehicleMatcher = VEHICLE_ID_PATH.matcher(path);
        if (vehicleMatcher.matches()) {
            Long vehicleId = Long.parseLong(vehicleMatcher.group(1));
            Optional<Vehicle> vehicle = vehicleRepository.findById(vehicleId);
            if (vehicle.isPresent() && !authUserId.equals(vehicle.get().getUserId())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "禁止访问其他用户车辆");
                return false;
            }
            return true;
        }

        Matcher orderMatcher = REPAIR_ORDER_ID_PATH.matcher(path);
        if (orderMatcher.matches()) {
            Long orderId = Long.parseLong(orderMatcher.group(1));
            Optional<RepairOrder> order = repairOrderRepository.findById(orderId);
            if (order.isPresent() && (order.get().getUser() == null || !authUserId.equals(order.get().getUser().getId()))) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "禁止访问其他用户维修工单");
                return false;
            }
            return true;
        }

        Matcher feedbackMatcher = FEEDBACK_ID_PATH.matcher(path);
        if (feedbackMatcher.matches()) {
            Long feedbackId = Long.parseLong(feedbackMatcher.group(1));
            Optional<Feedback> feedback = feedbackRepository.findById(feedbackId);
            if (feedback.isPresent() && (feedback.get().getUser() == null || !authUserId.equals(feedback.get().getUser().getId()))) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "禁止访问其他用户反馈");
                return false;
            }
            return true;
        }

        return true;
    }

    private boolean checkTechnicianOwnership(String path,
                                             Long authUserId,
                                             HttpServletResponse response) throws Exception {
        Matcher technicianScoped = TECHNICIAN_SCOPED_PATH.matcher(path);
        if (technicianScoped.matches()) {
            Long pathTechnicianId = Long.parseLong(technicianScoped.group(1));
            if (!authUserId.equals(pathTechnicianId)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "技师仅可查看自己的任务列表");
                return false;
            }
        }

        if (path.startsWith("/api/technicians/") && path.contains("/earnings")) {
            Long technicianId = extractIdFromTechnicianPath(path);
            if (technicianId != null && !authUserId.equals(technicianId)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "技师仅可查看自己的收入数据");
                return false;
            }
        }

        return true;
    }

    private Long extractIdFromTechnicianPath(String path) {
        String[] parts = path.split("/");
        if (parts.length < 4) {
            return null;
        }
        try {
            return Long.parseLong(parts[3]);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}

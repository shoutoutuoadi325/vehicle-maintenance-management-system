package org.com.repair.entity;

import java.util.Date;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity
public class RepairOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String orderNumber;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RepairStatus status;

    @Column(nullable = false)
    private String description;

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date startedAt;

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date completedAt;

    @Column
    private Double laborCost;

    // 绿色导向：碳排放评估相关字段
    @Column
    private Double estimatedEmission;

    @Column
    private Boolean ecoMaterial; // 是否环保材料

    @Column
    private Integer reworkCount; // 返工次数

    @Column
    private String repairType; // "repair" or "replace"

    @Column
    private Double materialCost;

    @Column
    private Double totalCost;
    
    @Column
    private Double estimatedHours;
    
    @Column
    private Double actualHours;
    
    @Column
    @Enumerated(EnumType.STRING)
    private AssignmentType assignmentType = AssignmentType.AUTO;

    @Column
    @Enumerated(EnumType.STRING)
    private org.com.repair.entity.Technician.SkillType requiredSkillType;

    @Column
    @Enumerated(EnumType.STRING)
    private UrgeStatus urgeStatus = UrgeStatus.NOT_URGED;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference("user-repairOrders")
    private User user;

    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    @JsonBackReference("vehicle-repairOrders")
    private Vehicle vehicle;

    @ManyToMany
    @JoinTable(
        name = "order_technician", 
        joinColumns = @JoinColumn(name = "order_id"),
        inverseJoinColumns = @JoinColumn(name = "technician_id")
    )
    private Set<Technician> technicians;

    @OneToMany(mappedBy = "repairOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("repairOrder-feedbacks")
    private List<Feedback> feedbacks;

    // 枚举类型定义
    public enum RepairStatus {
        PENDING, ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED
    }
    
    public enum AssignmentType {
        AUTO, MANUAL
    }

    public enum UrgeStatus {
        NOT_URGED, URGED
    }

    // 构造函数
    public RepairOrder() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public Double getEstimatedEmission() {
        return estimatedEmission;
    }

    public void setEstimatedEmission(Double estimatedEmission) {
        this.estimatedEmission = estimatedEmission;
    }

    public Boolean isEcoMaterial() {
        return ecoMaterial != null ? ecoMaterial : false;
    }

    public void setEcoMaterial(Boolean ecoMaterial) {
        this.ecoMaterial = ecoMaterial;
    }

    public Integer getReworkCount() {
        return reworkCount != null ? reworkCount : 0;
    }

    public void setReworkCount(Integer reworkCount) {
        this.reworkCount = reworkCount;
    }

    public String getRepairType() {
        return repairType;
    }

    public void setRepairType(String repairType) {
        this.repairType = repairType;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public RepairStatus getStatus() {
        return status;
    }

    public void setStatus(RepairStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Date getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
    }

    public Date getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Date completedAt) {
        this.completedAt = completedAt;
    }

    public Double getLaborCost() {
        return laborCost;
    }

    public void setLaborCost(Double laborCost) {
        this.laborCost = laborCost;
    }

    public Double getMaterialCost() {
        return materialCost;
    }

    public void setMaterialCost(Double materialCost) {
        this.materialCost = materialCost;
    }

    public Double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(Double totalCost) {
        this.totalCost = totalCost;
    }
    
    public Double getEstimatedHours() {
        return estimatedHours;
    }
    
    public void setEstimatedHours(Double estimatedHours) {
        this.estimatedHours = estimatedHours;
    }
    
    public Double getActualHours() {
        return actualHours;
    }
    
    public void setActualHours(Double actualHours) {
        this.actualHours = actualHours;
    }
    
    public AssignmentType getAssignmentType() {
        return assignmentType;
    }
    
    public void setAssignmentType(AssignmentType assignmentType) {
        this.assignmentType = assignmentType;
    }

    public org.com.repair.entity.Technician.SkillType getRequiredSkillType() {
        return requiredSkillType;
    }

    public void setRequiredSkillType(org.com.repair.entity.Technician.SkillType requiredSkillType) {
        this.requiredSkillType = requiredSkillType;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public Set<Technician> getTechnicians() {
        return technicians;
    }

    public void setTechnicians(Set<Technician> technicians) {
        this.technicians = technicians;
    }

    public List<Feedback> getFeedbacks() {
        return feedbacks;
    }

    public void setFeedbacks(List<Feedback> feedbacks) {
        this.feedbacks = feedbacks;
    }

    public UrgeStatus getUrgeStatus() {
        return urgeStatus;
    }

    public void setUrgeStatus(UrgeStatus urgeStatus) {
        this.urgeStatus = urgeStatus;
    }
}
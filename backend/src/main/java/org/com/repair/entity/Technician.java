package org.com.repair.entity;

import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

@Entity
public class Technician {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String employeeId;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String phone;

    @Column
    private String email;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SkillType skillType;

    @Column(nullable = false)
    private Double hourlyRate;
    
    @Column
    private Double totalWorkHours = 0.0;
    
    @Column
    private Integer completedOrders = 0;

    @ManyToMany(mappedBy = "technicians")
    private Set<RepairOrder> repairOrders;

    // 枚举类型定义
    public enum SkillType {
        MECHANIC, ELECTRICIAN, BODY_WORK, PAINT, DIAGNOSTIC
    }

    // 构造函数
    public Technician() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public SkillType getSkillType() {
        return skillType;
    }

    public void setSkillType(SkillType skillType) {
        this.skillType = skillType;
    }

    public Double getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(Double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }
    
    public Double getTotalWorkHours() {
        return totalWorkHours;
    }
    
    public void setTotalWorkHours(Double totalWorkHours) {
        this.totalWorkHours = totalWorkHours;
    }
    
    public Integer getCompletedOrders() {
        return completedOrders;
    }
    
    public void setCompletedOrders(Integer completedOrders) {
        this.completedOrders = completedOrders;
    }

    public Set<RepairOrder> getRepairOrders() {
        return repairOrders;
    }

    public void setRepairOrders(Set<RepairOrder> repairOrders) {
        this.repairOrders = repairOrders;
    }
}
package org.com.repair.config;

import org.com.repair.entity.Admin;
import org.com.repair.entity.Technician;
import org.com.repair.entity.Technician.SkillType;
import org.com.repair.entity.User;
import org.com.repair.repository.AdminRepository;
import org.com.repair.repository.TechnicianRepository;
import org.com.repair.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(100)
@Profile("standalone")
@ConditionalOnProperty(prefix = "standalone.demo-data", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StandaloneDemoDataInitializer implements ApplicationRunner {

    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final TechnicianRepository technicianRepository;

    public StandaloneDemoDataInitializer(AdminRepository adminRepository,
                                         UserRepository userRepository,
                                         TechnicianRepository technicianRepository) {
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.technicianRepository = technicianRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedAdmin();
        seedUser();
        seedTechnician();
        seedBodyTechnician();
    }

    private void seedAdmin() {
        if (adminRepository.existsByUsername("admin")) {
            return;
        }

        Admin admin = new Admin();
        admin.setUsername("admin");
        admin.setPassword("123456");
        admin.setName("管理员");
        admin.setPhone("13820260001");
        admin.setEmail("admin.system@example.com");
        admin.setRole("SUPER_ADMIN");
        adminRepository.save(admin);
    }

    private void seedUser() {
        if (userRepository.existsByUsername("user")) {
            return;
        }

        User user = new User();
        user.setUsername("user");
        user.setPassword("123456");
        user.setName("车主用户");
        user.setPhone("13820260002");
        user.setEmail("user@example.com");
        user.setAddress("默认地址");
        userRepository.save(user);
    }

    private void seedTechnician() {
        if (technicianRepository.existsByUsername("tech")) {
            return;
        }

        Technician technician = new Technician();
        technician.setUsername("tech");
        technician.setEmployeeId("TECH-001");
        technician.setPassword("123456");
        technician.setName("技师");
        technician.setPhone("13920260001");
        technician.setEmail("tech@example.com");
        technician.setSkillType(SkillType.MECHANIC);
        technician.setHourlyRate(128.0);
        technician.setServiceRating(4.8);
        technician.setSkillTags("发动机,底盘,疑难诊断");
        technician.setTotalWorkHours(0.0);
        technician.setCompletedOrders(0);
        technicianRepository.save(technician);
    }

    private void seedBodyTechnician() {
        if (technicianRepository.existsByUsername("body")) {
            return;
        }

        Technician technician = new Technician();
        technician.setUsername("body");
        technician.setEmployeeId("TECH-002");
        technician.setPassword("123456");
        technician.setName("钣喷技师");
        technician.setPhone("13920260002");
        technician.setEmail("body@example.com");
        technician.setSkillType(SkillType.BODY_WORK);
        technician.setHourlyRate(138.0);
        technician.setServiceRating(4.6);
        technician.setSkillTags("钣金,喷漆,外观修复");
        technician.setTotalWorkHours(0.0);
        technician.setCompletedOrders(0);
        technicianRepository.save(technician);
    }
}

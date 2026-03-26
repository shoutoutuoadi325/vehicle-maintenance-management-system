package org.com.repair.repository;

import java.util.Optional;

import org.com.repair.entity.BrandPartner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BrandPartnerRepository extends JpaRepository<BrandPartner, Long> {

    Optional<BrandPartner> findByBrandCode(String brandCode);
}

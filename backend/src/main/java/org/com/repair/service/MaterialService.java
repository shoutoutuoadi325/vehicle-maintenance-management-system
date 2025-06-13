package org.com.repair.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.com.repair.DTO.MaterialResponse;
import org.com.repair.DTO.NewMaterialRequest;
import org.com.repair.entity.Material;
import org.com.repair.repository.MaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaterialService {
    
    private final MaterialRepository materialRepository;
    
    public MaterialService(MaterialRepository materialRepository) {
        this.materialRepository = materialRepository;
    }
    
    @Transactional
    public MaterialResponse addMaterial(NewMaterialRequest request) {
        Material material = new Material();
        material.setName(request.name());
        material.setUnitPrice(request.unitPrice());
        Material saved = materialRepository.save(material);
        return new MaterialResponse(saved);
    }
    
    public Optional<MaterialResponse> getMaterialById(Long id) {
        return materialRepository.findById(id)
                .map(MaterialResponse::new);
    }
    
    @Transactional
    public MaterialResponse updateMaterial(Long id, NewMaterialRequest request) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("材料不存在"));
        
        material.setName(request.name());
        material.setUnitPrice(request.unitPrice());
        Material updated = materialRepository.save(material);
        return new MaterialResponse(updated);
    }
    
    @Transactional
    public boolean deleteMaterial(Long id) {
        if (materialRepository.existsById(id)) {
            materialRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    public List<MaterialResponse> getAllMaterials() {
        return materialRepository.findAll().stream()
                .map(MaterialResponse::new)
                .collect(Collectors.toList());
    }
    
    public List<MaterialResponse> getMaterialsByName(String name) {
        return materialRepository.findByNameContaining(name).stream()
                .map(MaterialResponse::new)
                .collect(Collectors.toList());
    }
    
    public List<MaterialResponse> getMaterialsByPriceRange(Double minPrice, Double maxPrice) {
        return materialRepository.findByUnitPriceBetween(minPrice, maxPrice).stream()
                .map(MaterialResponse::new)
                .collect(Collectors.toList());
    }
} 
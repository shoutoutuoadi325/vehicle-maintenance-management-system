package org.com.repair.controller;

import java.util.List;

import org.com.repair.DTO.MaterialResponse;
import org.com.repair.DTO.NewMaterialRequest;
import org.com.repair.service.MaterialService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/materials")
public class MaterialController {
    
    private final MaterialService materialService;
    
    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }
    
    @PostMapping
    public ResponseEntity<MaterialResponse> addMaterial(@RequestBody NewMaterialRequest request) {
        MaterialResponse response = materialService.addMaterial(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<MaterialResponse> getMaterialById(@PathVariable Long id) {
        return materialService.getMaterialById(id)
                .map(material -> new ResponseEntity<>(material, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @GetMapping
    public ResponseEntity<List<MaterialResponse>> getAllMaterials() {
        List<MaterialResponse> materials = materialService.getAllMaterials();
        return new ResponseEntity<>(materials, HttpStatus.OK);
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<MaterialResponse>> getMaterialsByName(@RequestParam String name) {
        List<MaterialResponse> materials = materialService.getMaterialsByName(name);
        return new ResponseEntity<>(materials, HttpStatus.OK);
    }
    
    @GetMapping("/price-range")
    public ResponseEntity<List<MaterialResponse>> getMaterialsByPriceRange(
            @RequestParam Double minPrice, 
            @RequestParam Double maxPrice) {
        List<MaterialResponse> materials = materialService.getMaterialsByPriceRange(minPrice, maxPrice);
        return new ResponseEntity<>(materials, HttpStatus.OK);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<MaterialResponse> updateMaterial(
            @PathVariable Long id, 
            @RequestBody NewMaterialRequest request) {
        try {
            MaterialResponse response = materialService.updateMaterial(id, request);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMaterial(@PathVariable Long id) {
        boolean deleted = materialService.deleteMaterial(id);
        return deleted ? new ResponseEntity<>(HttpStatus.NO_CONTENT) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
} 
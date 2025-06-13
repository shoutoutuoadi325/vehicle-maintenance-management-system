package org.com.repair.controller;

import java.util.List;

import org.com.repair.DTO.FeedbackResponse;
import org.com.repair.DTO.NewFeedbackRequest;
import org.com.repair.service.FeedbackService;
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
@RequestMapping("/api/feedbacks")
public class FeedbackController {
    
    private final FeedbackService feedbackService;
    
    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }
    
    @PostMapping
    public ResponseEntity<FeedbackResponse> addFeedback(@RequestBody NewFeedbackRequest request) {
        FeedbackResponse response = feedbackService.addFeedback(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<FeedbackResponse> getFeedbackById(@PathVariable Long id) {
        return feedbackService.getFeedbackById(id)
                .map(feedback -> new ResponseEntity<>(feedback, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @GetMapping("/repair-order/{repairOrderId}")
    public ResponseEntity<List<FeedbackResponse>> getFeedbacksByRepairOrderId(@PathVariable Long repairOrderId) {
        List<FeedbackResponse> feedbacks = feedbackService.getFeedbacksByRepairOrderId(repairOrderId);
        return new ResponseEntity<>(feedbacks, HttpStatus.OK);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<FeedbackResponse>> getFeedbacksByUserId(@PathVariable Long userId) {
        List<FeedbackResponse> feedbacks = feedbackService.getFeedbacksByUserId(userId);
        return new ResponseEntity<>(feedbacks, HttpStatus.OK);
    }
    
    @GetMapping
    public ResponseEntity<List<FeedbackResponse>> getAllFeedbacks() {
        List<FeedbackResponse> feedbacks = feedbackService.getAllFeedbacks();
        return new ResponseEntity<>(feedbacks, HttpStatus.OK);
    }
    

    
    @GetMapping("/search")
    public ResponseEntity<List<FeedbackResponse>> getFeedbacksByKeyword(@RequestParam String keyword) {
        List<FeedbackResponse> feedbacks = feedbackService.getFeedbacksByKeyword(keyword);
        return new ResponseEntity<>(feedbacks, HttpStatus.OK);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<FeedbackResponse> updateFeedback(
            @PathVariable Long id, 
            @RequestBody NewFeedbackRequest request) {
        try {
            FeedbackResponse response = feedbackService.updateFeedback(id, request);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeedback(
            @PathVariable Long id, 
            @RequestParam Long userId) {
        try {
            boolean deleted = feedbackService.deleteFeedback(id, userId);
            return deleted ? new ResponseEntity<>(HttpStatus.NO_CONTENT) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }
    
    @GetMapping("/repair-order/{repairOrderId}/average-rating")
    public ResponseEntity<Double> getAverageRatingByRepairOrderId(@PathVariable Long repairOrderId) {
        Double averageRating = feedbackService.getAverageRatingByRepairOrderId(repairOrderId);
        return new ResponseEntity<>(averageRating, HttpStatus.OK);
    }
    
    @GetMapping("/technician/{technicianId}/average-rating")
    public ResponseEntity<Double> getAverageRatingByTechnicianId(@PathVariable Long technicianId) {
        Double averageRating = feedbackService.getAverageRatingByTechnicianId(technicianId);
        return new ResponseEntity<>(averageRating, HttpStatus.OK);
    }
    
    @GetMapping("/overall-average-rating")
    public ResponseEntity<Double> getOverallAverageRating() {
        Double averageRating = feedbackService.getOverallAverageRating();
        return new ResponseEntity<>(averageRating, HttpStatus.OK);
    }
    
    @GetMapping("/rating-distribution")
    public ResponseEntity<java.util.Map<Integer, Long>> getRatingDistribution() {
        java.util.Map<Integer, Long> distribution = feedbackService.getRatingDistribution();
        return new ResponseEntity<>(distribution, HttpStatus.OK);
    }
    
    @GetMapping("/check-feedback")
    public ResponseEntity<Boolean> checkUserFeedback(
            @RequestParam Long repairOrderId, 
            @RequestParam Long userId) {
        boolean hasFeedback = feedbackService.hasUserFeedbackForOrder(repairOrderId, userId);
        return new ResponseEntity<>(hasFeedback, HttpStatus.OK);
    }
} 
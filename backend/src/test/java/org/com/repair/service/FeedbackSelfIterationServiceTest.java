package org.com.repair.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.Optional;

import org.com.repair.entity.AgentPromptTemplateConfig;
import org.com.repair.entity.DispatchWeightConfig;
import org.com.repair.repository.AgentPromptTemplateConfigRepository;
import org.com.repair.repository.DispatchWeightConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class FeedbackSelfIterationServiceTest {

    private JdbcTemplate jdbcTemplate;
    private AgentPromptTemplateConfigRepository promptTemplateRepository;
    private DispatchWeightConfigRepository dispatchWeightConfigRepository;
    private FeedbackSelfIterationService service;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate = mock(JdbcTemplate.class);
        promptTemplateRepository = mock(AgentPromptTemplateConfigRepository.class);
        dispatchWeightConfigRepository = mock(DispatchWeightConfigRepository.class);
        service = new FeedbackSelfIterationService(
                jdbcTemplate,
                promptTemplateRepository,
                dispatchWeightConfigRepository);

        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getLong("total_feedback")).thenReturn(10L);
        when(resultSet.getDouble("avg_rating")).thenReturn(3.4);
        when(resultSet.getLong("low_rating_count")).thenReturn(3L);
        when(resultSet.getLong("touched_technicians")).thenReturn(2L);
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any(LocalDateTime.class)))
                .thenAnswer((Answer<Object>) invocation -> {
                    @SuppressWarnings("unchecked")
                    RowMapper<FeedbackSelfIterationService.FeedbackAggregate> mapper =
                            invocation.getArgument(1, RowMapper.class);
                    return mapper.mapRow(resultSet, 0);
                });
    }

    @Test
    void shouldBuildAndStoreLatestIterationDraft() {
        FeedbackSelfIterationService.SelfIterationDraft draft = service.buildIterationDraft();

        assertEquals(10, draft.aggregate().totalFeedback());
        assertEquals(0.3, draft.aggregate().lowRatingRatio());
        assertEquals(new BigDecimal("0.5500"), draft.dispatchWeightDraft().ratingWeight());
        assertFalse(draft.configUpsertPreviewSql().isEmpty());
        assertTrue(service.getLatestDraft().isPresent());
    }

    @Test
    void shouldApproveLatestDraftAndPersistPromptAndDispatchConfig() {
        AgentPromptTemplateConfig promptConfig = AgentPromptTemplateConfig.builder()
                .id(7L)
                .agentRole("ARBITRATOR")
                .templateKey("default")
                .promptTemplate("old prompt")
                .enabled(true)
                .build();
        DispatchWeightConfig dispatchConfig = DispatchWeightConfig.builder()
                .id(8L)
                .configKey("default")
                .ratingWeight(new BigDecimal("0.5000"))
                .workloadWeight(new BigDecimal("0.3000"))
                .experienceWeight(new BigDecimal("0.2000"))
                .fatiguePenaltyWeight(BigDecimal.ZERO)
                .enabled(true)
                .build();
        when(promptTemplateRepository.findByAgentRoleAndTemplateKey("ARBITRATOR", "default"))
                .thenReturn(Optional.of(promptConfig));
        when(dispatchWeightConfigRepository.findByConfigKey("default"))
                .thenReturn(Optional.of(dispatchConfig));
        when(promptTemplateRepository.save(any(AgentPromptTemplateConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(dispatchWeightConfigRepository.save(any(DispatchWeightConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.buildIterationDraft();
        FeedbackSelfIterationService.SelfIterationApprovalResult result =
                service.approveLatestDraft("ops-admin", "确认本周低分反馈偏高");

        assertTrue(result.approved());
        assertEquals(new BigDecimal("0.5500"), dispatchConfig.getRatingWeight());
        assertEquals(new BigDecimal("0.1000"), dispatchConfig.getFatiguePenaltyWeight());
        assertTrue(promptConfig.getPromptTemplate().contains("反馈自演进补丁"));
        assertTrue(promptConfig.getUpdateReason().contains("ops-admin"));
        assertTrue(service.getLatestDraft().isEmpty());
        verify(promptTemplateRepository).save(promptConfig);
        verify(dispatchWeightConfigRepository).save(dispatchConfig);
    }
}

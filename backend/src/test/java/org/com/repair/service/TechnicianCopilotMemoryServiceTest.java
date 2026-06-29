package org.com.repair.service;

import org.com.repair.DTO.AIDiagnosisResponse;
import org.com.repair.entity.TechnicianCopilotMemory;
import org.com.repair.repository.TechnicianCopilotMemoryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TechnicianCopilotMemoryServiceTest {

    @Test
    void shouldPersistMaskedTechnicianCopilotMemoryAndBuildContext() {
        TechnicianCopilotMemoryRepository repository = mock(TechnicianCopilotMemoryRepository.class);
        TechnicianCopilotMemoryService service = new TechnicianCopilotMemoryService(repository, new PrivacyMaskingService());
        AIDiagnosisResponse response = new AIDiagnosisResponse("engine misfire", "inspect spark plugs");
        response.setConfidence(0.86);
        response.setWorkflowStatus("VALIDATED");
        HistoryCaseAgent.HistoryCaseEvidence historyCaseEvidence = new HistoryCaseAgent.HistoryCaseEvidence(
                "History Case Agent: RAG vector retrieval similar cases=RO-1(desc=沪B67890 VIN LSVFA49J232123456 更换火花塞). tags=HISTORY_RELATED_CASE,HISTORY_RAG_VECTOR_MATCH",
                List.of("HISTORY_RELATED_CASE", "HISTORY_RAG_VECTOR_MATCH"));

        when(repository.findByTechnicianId(7L)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.recordInteraction(
                7L,
                "沪B67890 怠速抖动，VIN LSVFA49J232123456",
                response,
                historyCaseEvidence);

        ArgumentCaptor<TechnicianCopilotMemory> captor = ArgumentCaptor.forClass(TechnicianCopilotMemory.class);
        verify(repository).save(captor.capture());
        TechnicianCopilotMemory saved = captor.getValue();
        assertFalse(saved.getLastProblemText().contains("沪B67890"));
        assertFalse(saved.getLastProblemText().contains("LSVFA49J232123456"));
        assertFalse(saved.getLastHistoryCaseSummary().contains("沪B67890"));
        assertFalse(saved.getLastHistoryCaseSummary().contains("LSVFA49J232123456"));
        assertTrue(saved.getLastProblemText().contains("[MASKED_PLATE_1]"));
        assertTrue(saved.getLastHistoryCaseSummary().contains("HISTORY_RAG_VECTOR_MATCH"));

        when(repository.findByTechnicianId(7L)).thenReturn(Optional.of(saved));
        TechnicianCopilotMemoryService.CopilotMemoryContext context = service.resolveContext(7L);

        assertTrue(context.found());
        assertTrue(context.promptContext().contains("priorSessions=1"));
        assertTrue(context.promptContext().contains("HISTORY_RAG_VECTOR_MATCH"));
        assertFalse(context.promptContext().contains("沪B67890"));
        assertFalse(context.promptContext().contains("LSVFA49J232123456"));
    }
}

package com.example.filmprop.service;

import com.example.filmprop.dto.request.BindingUpdateRequest;
import com.example.filmprop.dto.response.BindingDetailResponse;
import com.example.filmprop.entity.Crew;
import com.example.filmprop.entity.Prop;
import com.example.filmprop.entity.PropScheduleBinding;
import com.example.filmprop.repository.PropScheduleBindingRepository;
import com.example.filmprop.repository.ScheduleChangeLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleBindingConcurrencyTest {

    @Mock
    private PropScheduleBindingRepository bindingRepository;
    @Mock
    private ScheduleChangeLogRepository changeLogRepository;
    @Mock
    private PropService propService;
    @Mock
    private CrewService crewService;

    private ScheduleBindingService bindingService;

    @BeforeEach
    void setUp() {
        bindingService = new ScheduleBindingService(bindingRepository, changeLogRepository,
                propService, crewService);
    }

    private Prop prop() {
        Prop prop = new Prop();
        prop.setId(1L);
        prop.setPropCode("PROP-001");
        prop.setPropName("青铜剑");
        prop.setSceneType("古装");
        return prop;
    }

    private Crew crew() {
        Crew crew = new Crew();
        crew.setId(2L);
        crew.setCrewName("长风剧组");
        crew.setProjectName("长风");
        crew.setGenre("古装");
        return crew;
    }

    private PropScheduleBinding binding(long version, LocalDate startDate, LocalDate endDate) {
        PropScheduleBinding binding = new PropScheduleBinding();
        binding.setId(10L);
        binding.setPropId(1L);
        binding.setCrewId(2L);
        binding.setStartDate(startDate);
        binding.setEndDate(endDate);
        binding.setStatus("active");
        binding.setBindingType("formal");
        binding.setVersion(version);
        return binding;
    }

    private BindingUpdateRequest request(long version, LocalDate startDate, LocalDate endDate) {
        BindingUpdateRequest req = new BindingUpdateRequest();
        req.setId(10L);
        req.setVersion(version);
        req.setStartDate(startDate);
        req.setEndDate(endDate);
        req.setOperator("后保存的人");
        return req;
    }

    @Test
    void 后保存的版本过期时拒绝覆盖先保存的日期() {
        LocalDate oldStart = LocalDate.of(2026, 10, 1);
        LocalDate oldEnd = LocalDate.of(2026, 10, 10);
        PropScheduleBinding latest = binding(1L, oldStart.plusDays(1), oldEnd.plusDays(2));
        when(bindingRepository.findById(10L)).thenReturn(Optional.of(latest));

        BindingUpdateRequest request = request(0L, LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 20));

        ObjectOptimisticLockingFailureException error = assertThrows(
                ObjectOptimisticLockingFailureException.class,
                () -> bindingService.updateBinding(request));

        assertTrue(error.getMessage().contains("10"));
        assertEquals(1L, latest.getVersion());
        assertEquals(LocalDate.of(2026, 10, 2), latest.getStartDate());
        assertEquals(LocalDate.of(2026, 10, 12), latest.getEndDate());
        verify(bindingRepository, never()).saveAndFlush(any());
        verify(changeLogRepository, never()).save(any());
    }

    @Test
    void 两个请求同时提交时后落库的一方也会被拒绝() {
        PropScheduleBinding current = binding(0L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10));
        when(bindingRepository.findById(10L)).thenReturn(Optional.of(current));
        when(propService.getPropById(1L)).thenReturn(prop());
        when(crewService.getCrewById(2L)).thenReturn(crew());
        when(bindingRepository.findConflictingBindings(eq(1L), any(), any(), eq(10L)))
                .thenReturn(List.of());
        when(bindingRepository.saveAndFlush(current))
                .thenThrow(new ObjectOptimisticLockingFailureException(PropScheduleBinding.class, 10L));

        BindingUpdateRequest request = request(0L, LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 20));

        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> bindingService.updateBinding(request));
        verify(changeLogRepository, never()).save(any());
    }

    @Test
    void 版本匹配时保存并返回新版本依据的结束日期() {
        LocalDate firstSavedStart = LocalDate.of(2026, 10, 1);
        LocalDate firstSavedEnd = LocalDate.of(2026, 10, 10);
        PropScheduleBinding current = binding(1L, firstSavedStart, firstSavedEnd);
        when(bindingRepository.findById(10L)).thenReturn(Optional.of(current));
        when(propService.getPropById(1L)).thenReturn(prop());
        when(crewService.getCrewById(2L)).thenReturn(crew());
        when(bindingRepository.findConflictingBindings(eq(1L), any(), any(), eq(10L)))
                .thenReturn(List.of());
        when(bindingRepository.saveAndFlush(current)).thenAnswer(invocation -> {
            PropScheduleBinding saved = invocation.getArgument(0);
            saved.setVersion(2L);
            return saved;
        });

        BindingDetailResponse result = bindingService.updateBinding(
                request(1L, LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 15)));

        assertEquals(LocalDate.of(2026, 10, 3), result.getStartDate());
        assertEquals(LocalDate.of(2026, 10, 15), result.getEndDate());
        assertEquals(2L, result.getVersion());
        verify(bindingRepository).saveAndFlush(current);
        verify(changeLogRepository).save(any());
    }
}

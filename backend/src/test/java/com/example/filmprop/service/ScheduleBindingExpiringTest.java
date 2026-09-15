package com.example.filmprop.service;

import com.example.filmprop.dto.response.ExpiringBindingResponse;
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

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleBindingExpiringTest {

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

    private Prop buildProp(Long id, String code, String name) {
        Prop prop = new Prop();
        prop.setId(id);
        prop.setPropCode(code);
        prop.setPropName(name);
        prop.setSceneType("古装");
        return prop;
    }

    private Crew buildCrew(Long id, String name) {
        Crew crew = new Crew();
        crew.setId(id);
        crew.setCrewName(name);
        crew.setProjectName("项目" + name);
        crew.setGenre("古装");
        return crew;
    }

    private PropScheduleBinding buildBinding(Long id, Long propId, Long crewId, LocalDate endDate) {
        PropScheduleBinding binding = new PropScheduleBinding();
        binding.setId(id);
        binding.setPropId(propId);
        binding.setCrewId(crewId);
        binding.setStartDate(endDate.minusDays(10));
        binding.setEndDate(endDate);
        binding.setStatus("active");
        binding.setBindingType("formal");
        return binding;
    }

    @Test
    void returnsExpiringBindingsWithRemainingDaysAscending() {
        LocalDate today = LocalDate.now();
        // 仓库按结束日期升序返回（数据库 ORDER BY），服务层保持该顺序并计算剩余天数
        PropScheduleBinding b1 = buildBinding(1L, 11L, 21L, today);
        PropScheduleBinding b2 = buildBinding(2L, 12L, 22L, today.plusDays(2));
        PropScheduleBinding b3 = buildBinding(3L, 13L, 23L, today.plusDays(7));
        when(bindingRepository.findExpiringBindings(today, today.plusDays(7)))
                .thenReturn(List.of(b1, b2, b3));

        when(propService.getPropById(11L)).thenReturn(buildProp(11L, "PROP-001", "青铜剑"));
        when(propService.getPropById(12L)).thenReturn(buildProp(12L, "PROP-002", "木质屏风"));
        when(propService.getPropById(13L)).thenReturn(buildProp(13L, "PROP-003", "仿古灯笼"));
        when(crewService.getCrewById(21L)).thenReturn(buildCrew(21L, "长风剧组"));
        when(crewService.getCrewById(22L)).thenReturn(buildCrew(22L, "山河剧组"));
        when(crewService.getCrewById(23L)).thenReturn(buildCrew(23L, "明月剧组"));

        List<ExpiringBindingResponse> result = bindingService.getExpiringBindings(7);

        assertEquals(3, result.size());
        // 剩余天数从少到多
        assertEquals(0L, result.get(0).getRemainingDays());
        assertEquals(2L, result.get(1).getRemainingDays());
        assertEquals(7L, result.get(2).getRemainingDays());
        // 道具编号与剧组名随结果返回
        assertEquals("PROP-001", result.get(0).getPropCode());
        assertEquals("长风剧组", result.get(0).getCrewName());
        assertEquals("PROP-003", result.get(2).getPropCode());
        assertEquals("明月剧组", result.get(2).getCrewName());
        // 查询窗口为 [今天, 今天+7天]
        verify(bindingRepository).findExpiringBindings(eq(today), eq(today.plusDays(7)));
    }

    @Test
    void returnsEmptyWhenNothingExpiring() {
        LocalDate today = LocalDate.now();
        when(bindingRepository.findExpiringBindings(today, today.plusDays(7)))
                .thenReturn(List.of());

        List<ExpiringBindingResponse> result = bindingService.getExpiringBindings(7);

        assertTrue(result.isEmpty());
    }

    @Test
    void rejectsDaysLessThanOne() {
        assertThrows(IllegalArgumentException.class, () -> bindingService.getExpiringBindings(0));
        verifyNoInteractions(bindingRepository);
    }
}

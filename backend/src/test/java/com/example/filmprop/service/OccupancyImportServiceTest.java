package com.example.filmprop.service;

import com.example.filmprop.dto.response.BindingDetailResponse;
import com.example.filmprop.dto.response.ImportBatchResultResponse;
import com.example.filmprop.dto.response.ImportRowResponse;
import com.example.filmprop.entity.Crew;
import com.example.filmprop.entity.OccupancyImportBatch;
import com.example.filmprop.entity.OccupancyImportRow;
import com.example.filmprop.entity.Prop;
import com.example.filmprop.entity.PropScheduleBinding;
import com.example.filmprop.repository.CrewRepository;
import com.example.filmprop.repository.OccupancyImportBatchRepository;
import com.example.filmprop.repository.OccupancyImportRowRepository;
import com.example.filmprop.repository.PropRepository;
import com.example.filmprop.repository.PropScheduleBindingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OccupancyImportServiceTest {

    @Mock
    private OccupancyImportBatchRepository batchRepository;
    @Mock
    private OccupancyImportRowRepository rowRepository;
    @Mock
    private PropRepository propRepository;
    @Mock
    private CrewRepository crewRepository;
    @Mock
    private PropScheduleBindingRepository bindingRepository;
    @Mock
    private ScheduleBindingService bindingService;

    private OccupancyImportService importService;

    @BeforeEach
    void setUp() {
        // 测试用事务管理器：真实执行 TransactionTemplate 回调，不开启实际事务
        PlatformTransactionManager txManager = new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
            }

            @Override
            public void rollback(TransactionStatus status) {
            }
        };
        importService = new OccupancyImportService(
                batchRepository, rowRepository, propRepository, crewRepository,
                bindingRepository, bindingService, txManager);

        lenient().when(batchRepository.save(any(OccupancyImportBatch.class))).thenAnswer(inv -> {
            OccupancyImportBatch b = inv.getArgument(0);
            b.setId(100L);
            return b;
        });
        lenient().when(rowRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
    }

    private MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "occupancy.csv", "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }

    private Prop prop(long id, String code, String sceneType) {
        Prop p = new Prop();
        p.setId(id);
        p.setPropCode(code);
        p.setPropName("道具" + code);
        p.setSceneType(sceneType);
        return p;
    }

    private Crew crew(long id, String name, String genre) {
        Crew c = new Crew();
        c.setId(id);
        c.setCrewName(name);
        c.setGenre(genre);
        return c;
    }

    private void stubPropAndCrew() {
        lenient().when(propRepository.findByPropCode("PROP-001")).thenReturn(Optional.of(prop(1L, "PROP-001", "古装")));
        lenient().when(propRepository.findByPropCode("PROP-002")).thenReturn(Optional.of(prop(2L, "PROP-002", "现代")));
        lenient().when(crewRepository.findByCrewName("剧组A")).thenReturn(Optional.of(crew(1L, "剧组A", "古装")));
        lenient().when(crewRepository.findByCrewName("剧组B")).thenReturn(Optional.of(crew(2L, "剧组B", "现代")));
        lenient().when(crewRepository.findByCrewName("无片种剧组")).thenReturn(Optional.of(crew(3L, "无片种剧组", null)));
    }

    // ---------- 第一步：校验 ----------

    @Test
    void 校验_合法行全部通过() {
        stubPropAndCrew();
        ImportBatchResultResponse result = importService.validate(csv(
                "道具编号,剧组名,开始日期,结束日期,备注\n" +
                "PROP-001,剧组A,2026-10-01,2026-10-15,备注一\n" +
                "PROP-002,剧组B,2026/11/1,2026/11/10,\n"), "仓管小王");

        assertEquals(2, result.getBatch().getTotalRows());
        assertEquals(2, result.getBatch().getPassedRows());
        assertEquals(0, result.getBatch().getFailedRows());
        assertEquals("validated", result.getBatch().getStatus());
        assertEquals("仓管小王", result.getBatch().getOperator());
        result.getRows().forEach(r -> {
            assertEquals("passed", r.getValidateStatus());
            assertEquals("pending", r.getWriteStatus());
        });
        // 校验阶段绝不写绑定
        verifyNoInteractions(bindingService);
    }

    @Test
    void 校验_必填缺失与日期问题逐行落原因() {
        stubPropAndCrew();
        ImportBatchResultResponse result = importService.validate(csv(
                "道具编号,剧组名,开始日期,结束日期\n" +
                ",剧组A,2026-10-01,2026-10-15\n" +
                "PROP-001,,2026-10-01,2026-10-15\n" +
                "PROP-001,剧组A,2026-10-15,2026-10-01\n" +
                "PROP-001,剧组A,2026-13-01,2026-12-01\n" +
                "PROP-001,剧组A,2026-10-01,abc\n"), null);

        List<ImportRowResponse> rows = result.getRows();
        assertEquals(5, rows.size());
        assertEquals("道具编号不能为空", rows.get(0).getValidateMessage());
        assertEquals("剧组名不能为空", rows.get(1).getValidateMessage());
        assertEquals("开始日期不能晚于结束日期", rows.get(2).getValidateMessage());
        assertTrue(rows.get(3).getValidateMessage().contains("开始日期格式不正确"));
        assertTrue(rows.get(4).getValidateMessage().contains("结束日期格式不正确"));
        assertEquals(0, result.getBatch().getPassedRows());
        assertEquals(5, result.getBatch().getFailedRows());
    }

    @Test
    void 校验_道具剧组存在性与片种核对() {
        stubPropAndCrew();
        ImportBatchResultResponse result = importService.validate(csv(
                "道具编号,剧组名,开始日期,结束日期\n" +
                "PROP-999,剧组A,2026-10-01,2026-10-15\n" +
                "PROP-001,不存在剧组,2026-10-01,2026-10-15\n" +
                "PROP-001,无片种剧组,2026-10-01,2026-10-15\n" +
                "PROP-001,剧组B,2026-10-01,2026-10-15\n"), null);

        List<ImportRowResponse> rows = result.getRows();
        assertTrue(rows.get(0).getValidateMessage().contains("道具编号不存在"));
        assertTrue(rows.get(1).getValidateMessage().contains("剧组不存在"));
        assertTrue(rows.get(2).getValidateMessage().contains("未登记片种"));
        assertTrue(rows.get(3).getValidateMessage().contains("片种不符"));
        rows.forEach(r -> assertEquals("failed", r.getValidateStatus()));
    }

    @Test
    void 校验_库内档期冲突拦截() {
        stubPropAndCrew();
        PropScheduleBinding existing = new PropScheduleBinding();
        existing.setId(9L);
        existing.setCrewId(1L);
        existing.setStartDate(LocalDate.of(2026, 10, 10));
        existing.setEndDate(LocalDate.of(2026, 10, 20));
        when(bindingRepository.findConflictingBindings(eq(1L), any(), any(), eq(-1L)))
                .thenReturn(List.of(existing));
        when(crewRepository.findById(1L)).thenReturn(Optional.of(crew(1L, "剧组A", "古装")));

        ImportBatchResultResponse result = importService.validate(csv(
                "道具编号,剧组名,开始日期,结束日期\n" +
                "PROP-001,剧组A,2026-10-01,2026-10-15\n"), null);

        assertEquals("failed", result.getRows().get(0).getValidateStatus());
        assertTrue(result.getRows().get(0).getValidateMessage().contains("档期冲突"));
    }

    @Test
    void 校验_文件内同道具日期重叠_相关行一并拦截() {
        stubPropAndCrew();
        ImportBatchResultResponse result = importService.validate(csv(
                "道具编号,剧组名,开始日期,结束日期\n" +
                "PROP-001,剧组A,2026-10-01,2026-10-15\n" +
                "PROP-001,剧组A,2026-10-10,2026-10-20\n" +
                "PROP-001,剧组A,2026-11-01,2026-11-05\n" +
                "PROP-002,剧组B,2026-10-01,2026-10-15\n"), null);

        List<ImportRowResponse> rows = result.getRows();
        assertEquals("failed", rows.get(0).getValidateStatus());
        assertEquals("failed", rows.get(1).getValidateStatus());
        assertTrue(rows.get(0).getValidateMessage().contains("文件内档期重叠"));
        assertTrue(rows.get(0).getValidateMessage().contains("第 2 行"));
        // 不重叠的同道具行与另一道具行不受影响
        assertEquals("passed", rows.get(2).getValidateStatus());
        assertEquals("passed", rows.get(3).getValidateStatus());
        assertEquals(2, result.getBatch().getPassedRows());
    }

    @Test
    void 校验_空文件与无数据行直接拦截() {
        assertThrows(IllegalArgumentException.class,
                () -> importService.validate(new MockMultipartFile("file", new byte[0]), null));
        assertThrows(IllegalArgumentException.class,
                () -> importService.validate(csv("道具编号,剧组名,开始日期,结束日期\n\n"), null));
    }

    // ---------- 第二步：写入 ----------

    private OccupancyImportRow row(long id, int rowNo, String validateStatus) {
        OccupancyImportRow r = new OccupancyImportRow();
        r.setId(id);
        r.setBatchId(100L);
        r.setRowNo(rowNo);
        r.setPropCode("PROP-001");
        r.setCrewName("剧组A");
        r.setValidateStatus(validateStatus);
        r.setValidateMessage("passed".equals(validateStatus) ? "校验通过" : "道具编号不存在: PROP-001");
        if ("passed".equals(validateStatus)) {
            r.setPropId(1L);
            r.setCrewId(1L);
            r.setStartDate(LocalDate.of(2026, 10, 1));
            r.setEndDate(LocalDate.of(2026, 10, 15));
        }
        return r;
    }

    @Test
    void 写入_校验未过的行不进入写入() {
        when(batchRepository.markCommitted(eq(100L), any())).thenReturn(1);
        OccupancyImportRow passed = row(1L, 1, "passed");
        OccupancyImportRow failed = row(2L, 2, "failed");
        when(rowRepository.findByBatchIdOrderByRowNo(100L)).thenReturn(List.of(passed, failed));
        when(batchRepository.findById(100L)).thenReturn(Optional.of(new OccupancyImportBatch()));
        BindingDetailResponse created = new BindingDetailResponse();
        created.setId(555L);
        when(bindingService.createBinding(any())).thenReturn(created);

        ImportBatchResultResponse result = importService.commit(100L, "仓管小王");

        // 只有校验通过的行进入写入，且只调用一次
        verify(bindingService, times(1)).createBinding(argThat(req ->
                req.getPropId().equals(1L) && req.getCrewId().equals(1L)
                        && req.getStartDate().equals(LocalDate.of(2026, 10, 1))
                        && req.getEndDate().equals(LocalDate.of(2026, 10, 15))
                        && "仓管小王".equals(req.getOperator())));

        ImportRowResponse written = result.getRows().get(0);
        assertEquals("written", written.getWriteStatus());
        assertEquals(555L, written.getBindingId());

        ImportRowResponse blocked = result.getRows().get(1);
        assertEquals("not_written", blocked.getWriteStatus());
        assertTrue(blocked.getWriteMessage().contains("校验未通过，未进入写入"));
        assertNull(blocked.getBindingId());
    }

    @Test
    void 写入_写入期被兜底拦截的行落未写入() {
        when(batchRepository.markCommitted(eq(100L), any())).thenReturn(1);
        OccupancyImportRow passed = row(1L, 1, "passed");
        when(rowRepository.findByBatchIdOrderByRowNo(100L)).thenReturn(List.of(passed));
        when(batchRepository.findById(100L)).thenReturn(Optional.of(new OccupancyImportBatch()));
        when(bindingService.createBinding(any()))
                .thenThrow(new IllegalArgumentException("档期时间冲突: 道具已被【剧组C】占用"));

        ImportBatchResultResponse result = importService.commit(100L, null);

        ImportRowResponse row = result.getRows().get(0);
        assertEquals("not_written", row.getWriteStatus());
        assertTrue(row.getWriteMessage().contains("写入被拦截"));
        assertTrue(row.getWriteMessage().contains("档期时间冲突"));
    }

    @Test
    void 写入_批次重复提交被拦截() {
        when(batchRepository.markCommitted(eq(100L), any())).thenReturn(0);

        assertThrows(IllegalArgumentException.class, () -> importService.commit(100L, null));
        verify(bindingService, never()).createBinding(any());
    }

    // ---------- 结果查询 ----------

    @Test
    void 查询_可按未写入筛行() {
        when(batchRepository.existsById(100L)).thenReturn(true);
        OccupancyImportRow notWritten = row(2L, 2, "failed");
        notWritten.setWriteStatus("not_written");
        when(rowRepository.findRows(100L, null, "not_written")).thenReturn(List.of(notWritten));

        List<ImportRowResponse> rows = importService.getRows(100L, null, "not_written");

        assertEquals(1, rows.size());
        assertEquals("not_written", rows.get(0).getWriteStatus());
        verify(rowRepository).findRows(100L, null, "not_written");
    }

    @Test
    void 查询_非法筛选值直接拦截() {
        assertThrows(IllegalArgumentException.class, () -> importService.getRows(100L, null, "bad_status"));
        assertThrows(IllegalArgumentException.class, () -> importService.getRows(100L, "bad_status", null));
    }
}

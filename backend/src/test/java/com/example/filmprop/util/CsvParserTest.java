package com.example.filmprop.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvParserTest {

    private List<List<String>> parse(String content) throws Exception {
        return CsvParser.parse(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void 基本解析_多行多列() throws Exception {
        List<List<String>> rows = parse("道具编号,剧组名\nPROP-001,剧组A\nPROP-002,剧组B\n");
        assertEquals(3, rows.size());
        assertEquals(List.of("道具编号", "剧组名"), rows.get(0));
        assertEquals(List.of("PROP-001", "剧组A"), rows.get(1));
        assertEquals(List.of("PROP-002", "剧组B"), rows.get(2));
    }

    @Test
    void 支持CRLF与末尾无换行() throws Exception {
        List<List<String>> rows = parse("a,b\r\nc,d");
        assertEquals(2, rows.size());
        assertEquals(List.of("c", "d"), rows.get(1));
    }

    @Test
    void 引号字段_含逗号与转义引号() throws Exception {
        List<List<String>> rows = parse("PROP-001,\"备注,含逗号\",\"说\"\"你好\"\"\"\n");
        assertEquals(1, rows.size());
        assertEquals("备注,含逗号", rows.get(0).get(1));
        assertEquals("说\"你好\"", rows.get(0).get(2));
    }

    @Test
    void 跳过UTF8BOM() throws Exception {
        List<List<String>> rows = parse("\uFEFF道具编号,剧组名\n");
        assertEquals("道具编号", rows.get(0).get(0));
    }

    @Test
    void 空行保留为空字段行_由上层过滤() throws Exception {
        List<List<String>> rows = parse("a,b\n\n\nc,d\n");
        assertEquals(4, rows.size());
        assertEquals(List.of(""), rows.get(1));
        assertEquals(List.of(""), rows.get(2));
        assertEquals(List.of("c", "d"), rows.get(3));
    }
}

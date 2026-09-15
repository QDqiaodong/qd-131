package com.example.filmprop.dto.response;

import java.util.List;

/**
 * 校验 / 写入完成后的批次完整结果：批次汇总 + 全部行明细。
 */
public class ImportBatchResultResponse {
    private ImportBatchResponse batch;
    private List<ImportRowResponse> rows;

    public ImportBatchResultResponse(ImportBatchResponse batch, List<ImportRowResponse> rows) {
        this.batch = batch;
        this.rows = rows;
    }

    public ImportBatchResponse getBatch() {
        return batch;
    }

    public void setBatch(ImportBatchResponse batch) {
        this.batch = batch;
    }

    public List<ImportRowResponse> getRows() {
        return rows;
    }

    public void setRows(List<ImportRowResponse> rows) {
        this.rows = rows;
    }
}

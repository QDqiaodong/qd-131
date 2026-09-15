<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { occupancyImportApi } from '@/api';
import type { ImportBatch, ImportRow } from '@/types';

const batches = ref<ImportBatch[]>([]);
const currentBatch = ref<ImportBatch | null>(null);
const rows = ref<ImportRow[]>([]);
const selectedFile = ref<File | null>(null);
const operator = ref('');
const validating = ref(false);
const committing = ref(false);
const rowsLoading = ref(false);
const validateFilter = ref('');
const writeFilter = ref('');

const validateStatusMap: Record<string, { label: string; type: string }> = {
  passed: { label: '通过', type: 'success' },
  failed: { label: '未通过', type: 'danger' }
};
const writeStatusMap: Record<string, { label: string; type: string }> = {
  pending: { label: '待写入', type: 'info' },
  written: { label: '已写入', type: 'success' },
  not_written: { label: '未写入', type: 'danger' }
};
const batchStatusMap: Record<string, { label: string; type: string }> = {
  validated: { label: '已校验待写入', type: 'warning' },
  committed: { label: '已写入', type: 'success' }
};

// 先核后写：只有校验完成且仍有待写入行时，写入按钮才放行
const canCommit = computed(() =>
  !!currentBatch.value && currentBatch.value.status === 'validated' && currentBatch.value.passedRows > 0
);

const handleFileChange = (file: any) => {
  selectedFile.value = file.raw as File;
};

// limit=1 时再次选择会触发 on-exceed：清掉旧文件并替换为新文件
const uploadRef = ref();
const handleExceed = (files: File[]) => {
  uploadRef.value?.clearFiles();
  uploadRef.value?.handleStart(files[0]);
  selectedFile.value = files[0];
};

const handleValidate = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择 CSV 文件');
    return;
  }
  validating.value = true;
  try {
    const res = await occupancyImportApi.validate(selectedFile.value, operator.value || undefined);
    if (res.data.code === 200) {
      currentBatch.value = res.data.data.batch;
      rows.value = res.data.data.rows;
      resetFilters();
      const b = currentBatch.value;
      if (b.failedRows > 0) {
        ElMessage.warning(`校验完成：${b.passedRows} 行通过，${b.failedRows} 行未通过，未通过的行不会进入写入`);
      } else {
        ElMessage.success(`校验完成：${b.passedRows} 行全部通过，可执行写入`);
      }
      fetchBatches();
    }
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '校验失败');
  } finally {
    validating.value = false;
  }
};

const handleCommit = async () => {
  if (!currentBatch.value) return;
  try {
    await ElMessageBox.confirm(
      `确认把校验通过的 ${currentBatch.value.passedRows} 行写入档期绑定？校验未通过的 ${currentBatch.value.failedRows} 行不会写入。`,
      '写入确认',
      { type: 'warning' }
    );
  } catch {
    return;
  }
  committing.value = true;
  try {
    const res = await occupancyImportApi.commit(currentBatch.value.id, operator.value || undefined);
    if (res.data.code === 200) {
      currentBatch.value = res.data.data.batch;
      rows.value = res.data.data.rows;
      resetFilters();
      const b = currentBatch.value;
      if (b.notWrittenRows > 0) {
        ElMessage.warning(`写入完成：${b.writtenRows} 行已写入，${b.notWrittenRows} 行未写入，可按「未写入」筛出查看`);
      } else {
        ElMessage.success(`写入完成：${b.writtenRows} 行全部已写入`);
      }
      fetchBatches();
    }
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '写入失败');
  } finally {
    committing.value = false;
  }
};

const fetchBatches = async () => {
  try {
    const res = await occupancyImportApi.getBatches();
    if (res.data.code === 200) {
      batches.value = res.data.data;
    }
  } catch {
    ElMessage.error('获取导入批次失败');
  }
};

// 行筛选走后端接口：未写入的行随时可按 writeStatus=not_written 筛出
const fetchRows = async () => {
  if (!currentBatch.value) return;
  rowsLoading.value = true;
  try {
    const res = await occupancyImportApi.getRows(
      currentBatch.value.id,
      validateFilter.value || undefined,
      writeFilter.value || undefined
    );
    if (res.data.code === 200) {
      rows.value = res.data.data;
    }
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '获取导入行失败');
  } finally {
    rowsLoading.value = false;
  }
};

const handleViewBatch = (batch: ImportBatch) => {
  currentBatch.value = batch;
  resetFilters();
  fetchRows();
};

const resetFilters = () => {
  validateFilter.value = '';
  writeFilter.value = '';
};

const downloadTemplate = () => {
  const content = '道具编号,剧组名,开始日期,结束日期,备注\nPROP-001,示例剧组,2026-10-01,2026-10-15,示例备注\n';
  const blob = new Blob(['\uFEFF' + content], { type: 'text/csv;charset=utf-8' });
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = '道具占用导入模板.csv';
  link.click();
  URL.revokeObjectURL(link.href);
};

onMounted(fetchBatches);
</script>

<template>
  <div class="occupancy-import">
    <el-alert
      title="导入流程：选择 CSV → ① 校验（只核不写）→ ② 写入（仅校验通过的行落档）。校验未通过的行不能进入写入；写入完成后可按「未写入」筛出未落档的行。"
      type="info"
      :closable="false"
      show-icon
      class="flow-tip"
    />

    <div class="toolbar">
      <el-upload
        ref="uploadRef"
        :auto-upload="false"
        :limit="1"
        accept=".csv"
        :on-change="handleFileChange"
        :on-exceed="handleExceed"
        :show-file-list="true"
      >
        <el-button>选择 CSV 文件</el-button>
      </el-upload>
      <el-input v-model="operator" placeholder="操作人（选填）" style="width: 160px; margin: 0 10px;" />
      <el-button type="primary" :loading="validating" :disabled="!selectedFile" @click="handleValidate">
        ① 校验
      </el-button>
      <el-button type="success" :loading="committing" :disabled="!canCommit" @click="handleCommit">
        ② 写入
      </el-button>
      <el-button link type="primary" @click="downloadTemplate">下载 CSV 模板</el-button>
    </div>

    <el-descriptions v-if="currentBatch" :column="6" border size="small" class="batch-info">
      <el-descriptions-item label="批次号">{{ currentBatch.id }}</el-descriptions-item>
      <el-descriptions-item label="文件名">{{ currentBatch.fileName }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="batchStatusMap[currentBatch.status]?.type">
          {{ batchStatusMap[currentBatch.status]?.label }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="总行数">{{ currentBatch.totalRows }}</el-descriptions-item>
      <el-descriptions-item label="校验通过 / 未通过">
        {{ currentBatch.passedRows }} / {{ currentBatch.failedRows }}
      </el-descriptions-item>
      <el-descriptions-item label="已写入 / 未写入">
        {{ currentBatch.writtenRows }} / {{ currentBatch.notWrittenRows }}
      </el-descriptions-item>
    </el-descriptions>

    <div v-if="currentBatch" class="filter-bar">
      <span class="filter-label">校验状态：</span>
      <el-radio-group v-model="validateFilter" @change="fetchRows">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="passed">通过</el-radio-button>
        <el-radio-button value="failed">未通过</el-radio-button>
      </el-radio-group>
      <span class="filter-label">写入状态：</span>
      <el-radio-group v-model="writeFilter" @change="fetchRows">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="pending">待写入</el-radio-button>
        <el-radio-button value="written">已写入</el-radio-button>
        <el-radio-button value="not_written">未写入</el-radio-button>
      </el-radio-group>
    </div>

    <el-table v-if="currentBatch" :data="rows" :loading="rowsLoading" border stripe>
      <el-table-column prop="rowNo" label="行号" width="70" />
      <el-table-column prop="propCode" label="道具编号" width="110" />
      <el-table-column prop="crewName" label="剧组名" width="140" />
      <el-table-column prop="startDateRaw" label="开始日期" width="110" />
      <el-table-column prop="endDateRaw" label="结束日期" width="110" />
      <el-table-column prop="remark" label="备注" width="140" show-overflow-tooltip />
      <el-table-column label="校验结果" min-width="220">
        <template #default="{ row }">
          <el-tag :type="validateStatusMap[row.validateStatus]?.type">
            {{ validateStatusMap[row.validateStatus]?.label }}
          </el-tag>
          <span class="result-message">{{ row.validateMessage }}</span>
        </template>
      </el-table-column>
      <el-table-column label="写入结果" min-width="220">
        <template #default="{ row }">
          <el-tag :type="writeStatusMap[row.writeStatus]?.type">
            {{ writeStatusMap[row.writeStatus]?.label }}
          </el-tag>
          <span class="result-message">{{ row.writeMessage }}</span>
          <span v-if="row.bindingId" class="result-message">（绑定 #{{ row.bindingId }}）</span>
        </template>
      </el-table-column>
    </el-table>

    <el-divider content-position="left">历史导入批次</el-divider>
    <el-table :data="batches" border stripe size="small">
      <el-table-column prop="id" label="批次号" width="80" />
      <el-table-column prop="fileName" label="文件名" min-width="180" show-overflow-tooltip />
      <el-table-column prop="operator" label="操作人" width="100" />
      <el-table-column prop="totalRows" label="总行数" width="80" />
      <el-table-column prop="passedRows" label="通过" width="70" />
      <el-table-column prop="failedRows" label="未通过" width="70" />
      <el-table-column prop="writtenRows" label="已写入" width="70" />
      <el-table-column prop="notWrittenRows" label="未写入" width="70" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="batchStatusMap[row.status]?.type" size="small">
            {{ batchStatusMap[row.status]?.label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="上传时间" width="170" />
      <el-table-column label="操作" width="90">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleViewBatch(row)">查看行</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.occupancy-import {
  padding: 10px;
}

.flow-tip {
  margin-bottom: 15px;
}

.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
}

.batch-info {
  margin-bottom: 15px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 15px;
}

.filter-label {
  font-size: 14px;
  color: #606266;
}

.result-message {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}
</style>

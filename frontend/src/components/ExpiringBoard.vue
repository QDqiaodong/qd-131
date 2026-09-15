<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { bindingApi } from '@/api'
import type { ExpiringBinding } from '@/types'

const EXPIRING_DAYS = 7

const bindings = ref<ExpiringBinding[]>([])
const loading = ref(false)
const tableRef = ref()

// 剩余天数从少到多排（后端已按结束日期升序返回，这里再兜底一次）
const sortedBindings = computed(() =>
  [...bindings.value].sort((a, b) => a.remainingDays - b.remainingDays || a.id - b.id)
)

const remainingTagType = (days: number): 'danger' | 'warning' | 'primary' => {
  if (days <= 1) return 'danger'
  if (days <= 3) return 'warning'
  return 'primary'
}

const remainingText = (days: number) => (days <= 0 ? '今天结束' : `剩 ${days} 天`)

const bindingTypeText = (type: string) => (type === 'temporary' ? '临时借用' : '正式绑定')

const fetchData = async () => {
  loading.value = true
  try {
    const res = await bindingApi.getExpiring(EXPIRING_DAYS)
    if (res.data.code === 200) {
      bindings.value = res.data.data || []
    } else {
      ElMessage.error(res.data.message || '获取临期占用失败')
    }
  } catch (error) {
    ElMessage.error('获取临期占用失败')
  } finally {
    loading.value = false
  }
}

// 点击行展开/收起，查看道具编号和剧组名
const handleRowClick = (row: ExpiringBinding) => {
  tableRef.value?.toggleRowExpansion(row)
}

onMounted(fetchData)
</script>

<template>
  <div class="expiring-board">
    <div class="toolbar">
      <div class="summary">
        <span class="summary-title">临期占用台</span>
        <el-tag type="danger" effect="plain">
          未来 {{ EXPIRING_DAYS }} 天内要结束的占用共 {{ sortedBindings.length }} 条
        </el-tag>
        <span class="summary-tip">点击任意一行可查看道具编号和剧组名</span>
      </div>
      <el-button type="primary" :loading="loading" @click="fetchData">刷新</el-button>
    </div>

    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="sortedBindings"
      row-key="id"
      stripe
      class="expiring-table"
      @row-click="handleRowClick"
    >
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="expand-panel">
            <el-descriptions :column="3" border>
              <el-descriptions-item label="道具编号">
                <span class="highlight">{{ row.propCode }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="剧组名">
                <span class="highlight">{{ row.crewName }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="项目名称">{{ row.projectName || '—' }}</el-descriptions-item>
              <el-descriptions-item label="占用档期">
                {{ row.startDate }} 至 {{ row.endDate }}
              </el-descriptions-item>
              <el-descriptions-item label="绑定类型">{{ bindingTypeText(row.bindingType) }}</el-descriptions-item>
              <el-descriptions-item label="备注">{{ row.remark || '—' }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="剩余天数" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="remainingTagType(row.remainingDays)" effect="dark">
            {{ remainingText(row.remainingDays) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="endDate" label="结束日期" width="130" sortable />
      <el-table-column prop="propName" label="道具名称" min-width="140" />
      <el-table-column label="场景类型" width="110" align="center">
        <template #default="{ row }">
          <el-tag size="small">{{ row.sceneType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="startDate" label="占用开始" width="130" />

      <template #empty>
        <el-empty :description="`未来 ${EXPIRING_DAYS} 天内没有要结束的档期占用`" />
      </template>
    </el-table>
  </div>
</template>

<style scoped>
.expiring-board {
  padding: 10px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.summary {
  display: flex;
  align-items: center;
  gap: 12px;
}

.summary-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.summary-tip {
  font-size: 12px;
  color: #909399;
}

.expiring-table {
  width: 100%;
  cursor: pointer;
}

.expand-panel {
  padding: 12px 48px;
  background: #fafafa;
}

.highlight {
  font-weight: 600;
  color: #1e3a5f;
}
</style>

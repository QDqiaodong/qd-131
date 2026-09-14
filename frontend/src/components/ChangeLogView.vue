<script setup lang="ts">import { ref, computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { changeLogApi, propApi, crewApi } from '@/api';
import type { ScheduleChangeLog, Prop, Crew } from '@/types';
const logs = ref<ScheduleChangeLog[]>([]);
const props = ref<Prop[]>([]);
const crews = ref<Crew[]>([]);
const loading = ref(false);
const filterConflict = ref(false);
const changeTypeMap: Record<string, {
 label: string;
 class: string;
}> = {
 create: { label: '创建', class: 'el-tag--success' },
 update: { label: '改期', class: 'el-tag--warning' },
 cancel: { label: '取消', class: 'el-tag--danger' }
};
// 创建、改期、取消各计一条：仓管看三个数就知道档期被动过多少手。
// 新产生一条取消只会让 cancel +1，create/update 不受影响。
const stats = computed(() => ({
 create: logs.value.filter(l => l.changeType === 'create').length,
 update: logs.value.filter(l => l.changeType === 'update').length,
 cancel: logs.value.filter(l => l.changeType === 'cancel').length
}));
const getPropName = (propId: number): string => {
 const prop = props.value.find(p => p.id === propId);
 return prop ? prop.propName : '未知道具';
};
const getCrewName = (crewId: number): string => {
 const crew = crews.value.find(c => c.id === crewId);
 return crew ? crew.crewName : '未知剧组';
};
const fetchData = async () => {
 loading.value = true;
 try {
 const [logsRes, propsRes, crewsRes] = await Promise.all([
 changeLogApi.getAll(),
 propApi.getAll(),
 crewApi.getAll()
 ]);
 if (logsRes.data.code === 200) {
 logs.value = logsRes.data.data;
 }
 if (propsRes.data.code === 200) {
 props.value = propsRes.data.data;
 }
 if (crewsRes.data.code === 200) {
 crews.value = crewsRes.data.data;
 }
 }
 catch (error) {
 ElMessage.error('获取数据失败');
 }
 finally {
 loading.value = false;
 }
};
const filteredLogs = () => {
 if (filterConflict.value) {
 return logs.value.filter(l => l.conflictDetected);
 }
 return logs.value;
};
onMounted(fetchData);
</script>

<template>
  <div class="change-log-view">
    <div class="stats-cards">
      <el-card shadow="hover" class="stat-card stat-create">
        <div class="stat-label">创建次数</div>
        <div class="stat-value">{{ stats.create }}</div>
      </el-card>
      <el-card shadow="hover" class="stat-card stat-update">
        <div class="stat-label">改期次数</div>
        <div class="stat-value">{{ stats.update }}</div>
      </el-card>
      <el-card shadow="hover" class="stat-card stat-cancel">
        <div class="stat-label">取消次数</div>
        <div class="stat-value">{{ stats.cancel }}</div>
      </el-card>
    </div>

    <div class="toolbar">
      <el-button type="primary" @click="fetchData">
        <el-icon><Refresh /></el-icon>
        刷新日志
      </el-button>
      <el-checkbox v-model="filterConflict">只显示冲突记录</el-checkbox>
    </div>
    
    <el-table :data="filteredLogs()" :loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="changeType" label="变更类型" width="100">
        <template #default="{ row }">
          <el-tag :type="changeTypeMap[row.changeType]?.class">{{ changeTypeMap[row.changeType]?.label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="道具" width="150">
        <template #default="{ row }">
          {{ getPropName(row.propId) }}
        </template>
      </el-table-column>
      <el-table-column label="剧组" width="150">
        <template #default="{ row }">
          {{ getCrewName(row.crewId) }}
        </template>
      </el-table-column>
      <el-table-column label="原档期" width="200">
        <template #default="{ row }">
          <span v-if="row.originalStartDate">{{ row.originalStartDate }} ~ {{ row.originalEndDate }}</span>
          <span v-else class="text-gray">新建</span>
        </template>
      </el-table-column>
      <el-table-column label="新档期" width="200">
        <template #default="{ row }">
          {{ row.newStartDate }} ~ {{ row.newEndDate }}
        </template>
      </el-table-column>
      <el-table-column prop="changeReason" label="变更原因" width="150" />
      <el-table-column prop="operator" label="操作人" width="100" />
      <el-table-column prop="conflictDetected" label="是否冲突" width="100">
        <template #default="{ row }">
          <el-tag :type="row.conflictDetected ? 'danger' : 'success'">
            {{ row.conflictDetected ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="conflictDescription" label="冲突描述" width="200">
        <template #default="{ row }">
          <span v-if="row.conflictDescription" class="conflict-text">{{ row.conflictDescription }}</span>
          <span v-else class="text-gray">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="变更时间" width="180">
        <template #default="{ row }">
          {{ new Date(row.createdAt).toLocaleString() }}
        </template>
      </el-table-column>
    </el-table>
    
    <div v-if="filteredLogs().length === 0" class="empty-tip">
      <el-empty description="暂无变更记录" />
    </div>
  </div>
</template>

<style scoped>
.change-log-view {
  padding: 10px;
}

.stats-cards {
  display: flex;
  gap: 15px;
  margin-bottom: 15px;
}

.stat-card {
  flex: 1;
  text-align: center;
}

.stat-card :deep(.el-card__body) {
  padding: 15px;
}

.stat-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
}

.stat-create .stat-value {
  color: #67c23a;
}

.stat-update .stat-value {
  color: #e6a23c;
}

.stat-cancel .stat-value {
  color: #f56c6c;
}

.toolbar {
  margin-bottom: 15px;
}

.text-gray {
  color: #909399;
}

.conflict-text {
  color: #f56c6c;
}

.empty-tip {
  padding: 40px 0;
}
</style>

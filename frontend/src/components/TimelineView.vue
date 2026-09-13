<script setup lang="ts">import { ref, onMounted, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { bindingApi, propApi, crewApi } from '@/api';
import type { BindingDetail, Prop, Crew } from '@/types';
const bindings = ref<BindingDetail[]>([]);
const props = ref<Prop[]>([]);
const crews = ref<Crew[]>([]);
const loading = ref(false);
const selectedCrewId = ref<number | null>(null);
const viewMode = ref<'all' | 'byProp' | 'byCrew'>('all');
const startDate = ref('');
const endDate = ref('');
const generateDateRange = (start: string, end: string): string[] => {
 const dates: string[] = [];
 const startDt = new Date(start);
 const endDt = new Date(end);
 while (startDt <= endDt) {
 dates.push(startDt.toISOString().split('T')[0]);
 startDt.setDate(startDt.getDate() + 1);
 }
 return dates;
};
const today = new Date();
const defaultStartDate = today.toISOString().split('T')[0];
const defaultEndDate = new Date(today.getTime() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];
const dateRange = computed(() => {
 const s = startDate.value || defaultStartDate;
 const e = endDate.value || defaultEndDate;
 return generateDateRange(s, e);
});
const propsWithBindings = computed(() => {
 const propMap = new Map<number, {
 prop: Prop;
 bindings: BindingDetail[];
 }>();
 const filteredBindings = bindings.value.filter(b => {
 if (selectedCrewId.value && b.crewId !== selectedCrewId.value)
 return false;
 const s = startDate.value || defaultStartDate;
 const e = endDate.value || defaultEndDate;
 return b.startDate <= e && b.endDate >= s;
 });
 props.value.forEach(p => {
 propMap.set(p.id, { prop: p, bindings: [] });
 });
 filteredBindings.forEach(b => {
 const entry = propMap.get(b.propId);
 if (entry) {
 entry.bindings.push(b);
 }
 });
 return Array.from(propMap.values());
});
const isDateInRange = (date: string, start: string, end: string): boolean => {
 return date >= start && date <= end;
};
const getBindingForDate = (bindings: BindingDetail[], date: string): BindingDetail | null => {
 return bindings.find(b => isDateInRange(date, b.startDate, b.endDate)) || null;
};
const checkConflictForProp = (bindings: BindingDetail[]): boolean => {
 if (bindings.length < 2)
 return false;
 for (let i = 0; i < bindings.length; i++) {
 for (let j = i + 1; j < bindings.length; j++) {
 if (bindings[i].startDate <= bindings[j].endDate && bindings[i].endDate >= bindings[j].startDate) {
 return true;
 }
 }
 }
 return false;
};
const isConflictDate = (bindings: BindingDetail[], date: string): boolean => {
 const dateBindings = bindings.filter(b => isDateInRange(date, b.startDate, b.endDate));
 return dateBindings.length > 1;
};
const getCrewColor = (crewName: string): string => {
 const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#722ED1', '#13C2C2', '#EB2F96'];
 let hash = 0;
 for (let i = 0; i < crewName.length; i++) {
 hash = crewName.charCodeAt(i) + ((hash << 5) - hash);
 }
 return colors[Math.abs(hash) % colors.length];
};
const fetchData = async () => {
 loading.value = true;
 try {
 const [bindingsRes, propsRes, crewsRes] = await Promise.all([
 bindingApi.getAll(),
 propApi.getAll(),
 crewApi.getAll()
 ]);
 if (bindingsRes.data.code === 200) {
 bindings.value = bindingsRes.data.data.filter((b: BindingDetail) => b.status === 'active');
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
onMounted(fetchData);
</script>

<template>
  <div class="timeline-view">
    <div class="toolbar">
      <el-select v-model="viewMode" style="width: 150px; margin-right: 10px;">
        <el-option label="全部视图" value="all" />
        <el-option label="按道具查看" value="byProp" />
        <el-option label="按剧组查看" value="byCrew" />
      </el-select>
      <el-select v-model="selectedCrewId" placeholder="选择剧组过滤" style="width: 200px; margin-right: 10px;" :disabled="viewMode !== 'byCrew'">
        <el-option :label="'全部'" :value="null" />
        <el-option v-for="c in crews" :key="c.id" :label="c.crewName" :value="c.id" />
      </el-select>
      <el-date-picker v-model="startDate" type="date" placeholder="开始日期" style="margin-right: 10px;" />
      <el-date-picker v-model="endDate" type="date" placeholder="结束日期" style="margin-right: 10px;" />
      <el-button @click="fetchData">刷新</el-button>
    </div>
    
    <div class="timeline-container">
      <div class="timeline-header">
        <div class="prop-column">道具信息</div>
        <div class="dates-row">
          <div v-for="date in dateRange" :key="date" class="date-cell">
            <div class="date-day">{{ new Date(date).getDate() }}</div>
            <div class="date-weekday">{{ ['日','一','二','三','四','五','六'][new Date(date).getDay()] }}</div>
          </div>
        </div>
      </div>
      
      <div class="timeline-body">
        <div v-for="item in propsWithBindings" :key="item.prop.id" class="prop-row" :class="{ 'has-conflict': checkConflictForProp(item.bindings) }">
          <div class="prop-column">
            <div class="prop-code">{{ item.prop.propCode }}</div>
            <div class="prop-name">{{ item.prop.propName }}</div>
            <div class="prop-scene">
              <el-tag size="small">{{ item.prop.sceneType }}</el-tag>
            </div>
          </div>
          <div class="dates-row">
            <div v-for="date in dateRange" :key="date" class="date-cell" :class="{ 'conflict-cell': isConflictDate(item.bindings, date) }">
              <template v-if="getBindingForDate(item.bindings, date)">
                <div class="binding-block" :style="{ backgroundColor: getCrewColor(getBindingForDate(item.bindings, date)!.crewName) }">
                  <div class="binding-crew">{{ getBindingForDate(item.bindings, date)!.crewName }}</div>
                </div>
              </template>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <div class="legend">
      <span class="legend-title">图例：</span>
      <div v-for="crew in crews" :key="crew.id" class="legend-item">
        <span class="legend-color" :style="{ backgroundColor: getCrewColor(crew.crewName) }"></span>
        <span>{{ crew.crewName }}</span>
      </div>
    </div>
    
    <div class="tips">
      <el-alert title="提示" type="warning" :closable="false" show-icon>
        <ul>
          <li>红色标记的单元格表示存在档期冲突</li>
          <li>每一行代表一个道具，彩色条块表示该道具被哪个剧组占用</li>
          <li>可以通过筛选框按剧组查看特定剧组的道具占用情况</li>
        </ul>
      </el-alert>
    </div>
  </div>
</template>

<style scoped>
.timeline-view {
  padding: 10px;
}

.toolbar {
  margin-bottom: 15px;
}

.timeline-container {
  overflow-x: auto;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: white;
}

.timeline-header {
  display: flex;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
}

.prop-column {
  width: 180px;
  min-width: 180px;
  padding: 10px;
  border-right: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.prop-column .prop-code {
  font-weight: bold;
  color: #303133;
}

.prop-column .prop-name {
  color: #606266;
  font-size: 13px;
  margin-top: 4px;
}

.prop-column .prop-scene {
  margin-top: 4px;
}

.dates-row {
  display: flex;
}

.date-cell {
  width: 60px;
  min-width: 60px;
  height: 40px;
  border-right: 1px solid #e4e7ed;
  border-bottom: 1px solid #e4e7ed;
  text-align: center;
  padding: 4px;
  flex-shrink: 0;
}

.date-day {
  font-weight: bold;
  color: #303133;
}

.date-weekday {
  font-size: 11px;
  color: #909399;
}

.timeline-body {
  max-height: 600px;
  overflow-y: auto;
}

.prop-row {
  display: flex;
}

.prop-row:hover {
  background: #fafafa;
}

.prop-row.has-conflict {
  background: #fef0f0;
}

.date-cell {
  position: relative;
}

.date-cell.conflict-cell {
  background: #fef0f0 !important;
}

.binding-block {
  position: absolute;
  top: 4px;
  left: 4px;
  right: 4px;
  bottom: 4px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.binding-crew {
  font-size: 10px;
  color: white;
  font-weight: 500;
  text-shadow: 0 1px 2px rgba(0,0,0,0.2);
}

.legend {
  margin-top: 15px;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 8px;
}

.legend-title {
  font-weight: bold;
  margin-right: 15px;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  margin-right: 20px;
}

.legend-color {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  margin-right: 6px;
}

.tips {
  margin-top: 15px;
}
</style>

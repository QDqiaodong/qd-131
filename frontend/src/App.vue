<script setup lang="ts">
import { ref } from 'vue'
import ExpiringBoard from '@/components/ExpiringBoard.vue'
import PropManager from '@/components/PropManager.vue'
import CrewManager from '@/components/CrewManager.vue'
import BindingManager from '@/components/BindingManager.vue'
import OccupancyImport from '@/components/OccupancyImport.vue'
import TimelineView from '@/components/TimelineView.vue'
import ChangeLogView from '@/components/ChangeLogView.vue'

const activeTab = ref('expiring')
</script>

<template>
  <div class="app-container">
    <header class="app-header">
      <h1>影视外景基地拍摄道具剧组档期匹配绑定系统</h1>
    </header>

    <nav class="app-nav">
      <el-tabs v-model="activeTab" type="border-card">
        <el-tab-pane label="临期占用台" name="expiring">
          <ExpiringBoard />
        </el-tab-pane>
        <el-tab-pane label="道具管理" name="prop">
          <PropManager />
        </el-tab-pane>
        <el-tab-pane label="剧组管理" name="crew">
          <CrewManager />
        </el-tab-pane>
        <el-tab-pane label="档期绑定" name="binding">
          <BindingManager />
        </el-tab-pane>
        <el-tab-pane label="占用导入" name="import">
          <OccupancyImport />
        </el-tab-pane>
        <el-tab-pane label="时间轴视图" name="timeline">
          <TimelineView />
        </el-tab-pane>
        <el-tab-pane label="变更日志" name="log">
          <ChangeLogView />
        </el-tab-pane>
      </el-tabs>
    </nav>
  </div>
</template>

<style scoped>
.app-container {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.app-header {
  background: linear-gradient(135deg, #1e3a5f 0%, #2c5282 100%);
  color: white;
  padding: 20px;
  text-align: center;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.app-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.app-nav {
  padding: 20px;
}

:deep(.el-tabs) {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}
</style>

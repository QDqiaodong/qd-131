<script setup lang="ts">import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { crewApi } from '@/api';
import type { Crew, CrewCreateRequest } from '@/types';
const crews = ref<Crew[]>([]);
const loading = ref(false);
const showDialog = ref(false);
const isEdit = ref(false);
const currentId = ref<number | null>(null);
const form = ref<CrewCreateRequest>({
 crewName: '',
 projectName: '',
 director: '',
 genre: '',
 startDate: '',
 endDate: ''
});
const genres = ['古装', '现代'];
const statusMap: Record<string, {
 label: string;
 class: string;
}> = {
 active: { label: '拍摄中', class: 'el-tag--success' },
 completed: { label: '已完成', class: 'el-tag--info' }
};
const fetchCrews = async () => {
 loading.value = true;
 try {
 const response = await crewApi.getAll();
 if (response.data.code === 200) {
 crews.value = response.data.data;
 }
 }
 catch (error) {
 ElMessage.error('获取剧组列表失败');
 }
 finally {
 loading.value = false;
 }
};
const handleAdd = () => {
 isEdit.value = false;
 currentId.value = null;
 form.value = {
 crewName: '',
 projectName: '',
 director: '',
 genre: '',
 startDate: '',
 endDate: ''
 };
 showDialog.value = true;
};
const handleEdit = (crew: Crew) => {
 isEdit.value = true;
 currentId.value = crew.id;
 form.value = {
 crewName: crew.crewName,
 projectName: crew.projectName || '',
 director: crew.director || '',
 genre: crew.genre || '',
 startDate: crew.startDate || '',
 endDate: crew.endDate || ''
 };
 showDialog.value = true;
};
const handleDelete = async (crew: Crew) => {
 try {
 await ElMessageBox.confirm(`确定删除剧组【${crew.crewName}】吗？`, '确认删除', {
 type: 'warning'
 });
 await crewApi.delete(crew.id);
 ElMessage.success('删除成功');
 fetchCrews();
 }
 catch {
 // cancelled
 }
};
const handleSubmit = async () => {
 try {
 if (!form.value.crewName) {
 ElMessage.warning('请填写剧组名称');
 return;
 }
 if (!form.value.genre) {
 ElMessage.warning('请选择剧组片种');
 return;
 }
 if (isEdit.value && currentId.value) {
 await crewApi.update(currentId.value, form.value);
 ElMessage.success('更新成功');
 }
 else {
 await crewApi.create(form.value);
 ElMessage.success('创建成功');
 }
 showDialog.value = false;
 fetchCrews();
 }
 catch (error: any) {
 const message = error?.response?.data?.message || '操作失败';
 ElMessage.error(message);
 }
};
onMounted(fetchCrews);
</script>

<template>
  <div class="crew-manager">
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        添加剧组
      </el-button>
    </div>
    
    <el-table :data="crews" :loading="loading" border stripe>
      <el-table-column prop="crewName" label="剧组名称" width="150" />
      <el-table-column prop="projectName" label="项目名称" width="200" />
      <el-table-column prop="director" label="导演" width="100" />
      <el-table-column prop="genre" label="片种" width="90">
        <template #default="{ row }">
          <el-tag :type="row.genre === '古装' ? 'warning' : 'primary'">{{ row.genre || '未登记' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="startDate" label="拍摄开始" width="120" />
      <el-table-column prop="endDate" label="拍摄结束" width="120" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusMap[row.status]?.class">{{ statusMap[row.status]?.label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ new Date(row.createdAt).toLocaleString() }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <el-dialog :title="isEdit ? '编辑剧组' : '添加剧组'" v-model="showDialog" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="剧组名称" required>
          <el-input v-model="form.crewName" placeholder="请输入剧组名称" />
        </el-form-item>
        <el-form-item label="项目名称">
          <el-input v-model="form.projectName" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="导演">
          <el-input v-model="form.director" placeholder="请输入导演姓名" />
        </el-form-item>
        <el-form-item label="片种" required>
          <el-select v-model="form.genre" placeholder="请选择片种">
            <el-option v-for="g in genres" :key="g" :label="g" :value="g" />
          </el-select>
        </el-form-item>
        <el-form-item label="拍摄开始日期">
          <el-date-picker v-model="form.startDate" type="date" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="拍摄结束日期">
          <el-date-picker v-model="form.endDate" type="date" placeholder="选择日期" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.crew-manager {
  padding: 10px;
}

.toolbar {
  margin-bottom: 15px;
}
</style>

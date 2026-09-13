<script setup lang="ts">import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { propApi } from '@/api';
import type { Prop, PropCreateRequest } from '@/types';
const props = ref<Prop[]>([]);
const loading = ref(false);
const showDialog = ref(false);
const isEdit = ref(false);
const currentId = ref<number | null>(null);
const form = ref<PropCreateRequest>({
 propCode: '',
 propName: '',
 sceneType: '古装',
 material: '',
 specification: '',
 quantity: 1
});
const sceneTypes = ['古装', '现代'];
const statusMap: Record<string, {
 label: string;
 class: string;
}> = {
 available: { label: '可用', class: 'el-tag--success' },
 in_use: { label: '使用中', class: 'el-tag--warning' }
};
const fetchProps = async () => {
 loading.value = true;
 try {
 const response = await propApi.getAll();
 if (response.data.code === 200) {
 props.value = response.data.data;
 }
 }
 catch (error) {
 ElMessage.error('获取道具列表失败');
 }
 finally {
 loading.value = false;
 }
};
const handleAdd = () => {
 isEdit.value = false;
 currentId.value = null;
 form.value = {
 propCode: '',
 propName: '',
 sceneType: '古装',
 material: '',
 specification: '',
 quantity: 1
 };
 showDialog.value = true;
};
const handleEdit = (prop: Prop) => {
 isEdit.value = true;
 currentId.value = prop.id;
 form.value = {
 propCode: prop.propCode,
 propName: prop.propName,
 sceneType: prop.sceneType,
 material: prop.material || '',
 specification: prop.specification || '',
 quantity: prop.quantity
 };
 showDialog.value = true;
};
const handleDelete = async (prop: Prop) => {
 try {
 await ElMessageBox.confirm(`确定删除道具【${prop.propName}】吗？`, '确认删除', {
 type: 'warning'
 });
 await propApi.delete(prop.id);
 ElMessage.success('删除成功');
 fetchProps();
 }
 catch {
 // cancelled
 }
};
const handleSubmit = async () => {
 try {
 if (!form.value.propCode || !form.value.propName) {
 ElMessage.warning('请填写必填字段');
 return;
 }
 if (isEdit.value && currentId.value) {
 await propApi.update(currentId.value, form.value);
 ElMessage.success('更新成功');
 }
 else {
 await propApi.create(form.value);
 ElMessage.success('创建成功');
 }
 showDialog.value = false;
 fetchProps();
 }
 catch (error: any) {
 const message = error?.response?.data?.message || '操作失败';
 ElMessage.error(message);
 }
};
onMounted(fetchProps);
</script>

<template>
  <div class="prop-manager">
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        添加道具
      </el-button>
    </div>
    
    <el-table :data="props" :loading="loading" border stripe>
      <el-table-column prop="propCode" label="道具编号" width="120" />
      <el-table-column prop="propName" label="道具名称" width="150" />
      <el-table-column prop="sceneType" label="适用场景" width="100">
        <template #default="{ row }">
          <el-tag>{{ row.sceneType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="material" label="材质" width="120" />
      <el-table-column prop="specification" label="规格" width="150" />
      <el-table-column prop="quantity" label="数量" width="80" />
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
    
    <el-dialog :title="isEdit ? '编辑道具' : '添加道具'" v-model="showDialog" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="道具编号" required>
          <el-input v-model="form.propCode" placeholder="请输入道具编号" />
        </el-form-item>
        <el-form-item label="道具名称" required>
          <el-input v-model="form.propName" placeholder="请输入道具名称" />
        </el-form-item>
        <el-form-item label="适用场景">
          <el-select v-model="form.sceneType">
            <el-option v-for="type in sceneTypes" :key="type" :label="type" :value="type" />
          </el-select>
        </el-form-item>
        <el-form-item label="材质">
          <el-input v-model="form.material" placeholder="请输入材质" />
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="form.specification" placeholder="请输入规格" />
        </el-form-item>
        <el-form-item label="数量" required>
          <el-input-number v-model="form.quantity" :min="1" />
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
.prop-manager {
  padding: 10px;
}

.toolbar {
  margin-bottom: 15px;
}
</style>

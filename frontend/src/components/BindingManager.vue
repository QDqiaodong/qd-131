<script setup lang="ts">import { ref, onMounted, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { propApi, crewApi, bindingApi } from '@/api';
import type { Prop, Crew, BindingDetail, BindingCreateRequest, BindingUpdateRequest } from '@/types';
const bindings = ref<BindingDetail[]>([]);
const props = ref<Prop[]>([]);
const crews = ref<Crew[]>([]);
const loading = ref(false);
const showDialog = ref(false);
const isEdit = ref(false);
const currentId = ref<number | null>(null);
const currentVersion = ref(0);
const searchCrewName = ref('');
const searchStartDate = ref('');
const searchEndDate = ref('');
const form = ref<BindingCreateRequest>({
 propId: 0,
 crewId: 0,
 startDate: '',
 endDate: '',
 bindingType: 'formal',
 remark: ''
});
const bindingTypes = [
 { label: '正式绑定', value: 'formal' },
 { label: '临时借用', value: 'temporary' }
];
const statusMap: Record<string, {
 label: string;
 class: string;
}> = {
 active: { label: '生效中', class: 'el-tag--success' },
 completed: { label: '已完成', class: 'el-tag--info' },
 cancelled: { label: '已取消', class: 'el-tag--danger' }
};
const filteredBindings = computed(() => {
 let result = bindings.value;
 if (searchCrewName.value) {
 result = result.filter(b => b.crewName.includes(searchCrewName.value));
 }
 if (searchStartDate.value && searchEndDate.value) {
 result = result.filter(b => {
 return b.startDate <= searchEndDate.value && b.endDate >= searchStartDate.value;
 });
 }
 return result;
});
// 片种核对：提交绑定前对照剧组片种与道具场景类型，不一致即为片种不符
const selectedProp = computed(() => props.value.find(p => p.id === form.value.propId) || null);
const selectedCrew = computed(() => crews.value.find(c => c.id === form.value.crewId) || null);
const genreMismatch = computed(() => {
 if (!selectedProp.value || !selectedCrew.value) return false;
 return !selectedCrew.value.genre || selectedCrew.value.genre !== selectedProp.value.sceneType;
});
const genreMismatchMessage = computed(() => {
 if (!genreMismatch.value || !selectedProp.value || !selectedCrew.value) return '';
 if (!selectedCrew.value.genre) {
 return `片种核对失败：剧组【${selectedCrew.value.crewName}】未登记片种，请先完善剧组片种信息`;
 }
 return `片种不符：剧组【${selectedCrew.value.crewName}】片种为【${selectedCrew.value.genre}】，道具【${selectedProp.value.propName}】适用场景为【${selectedProp.value.sceneType}】，跨片种绑定将被拦截`;
});
const fetchData = async () => {
 loading.value = true;
 try {
 const [bindingsRes, propsRes, crewsRes] = await Promise.all([
 bindingApi.getAll(),
 propApi.getAll(),
 crewApi.getAll()
 ]);
 if (bindingsRes.data.code === 200) {
 bindings.value = bindingsRes.data.data;
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
const handleAdd = () => {
 isEdit.value = false;
 currentId.value = null;
 currentVersion.value = 0;
 form.value = {
 propId: 0,
 crewId: 0,
 startDate: '',
 endDate: '',
 bindingType: 'formal',
 remark: ''
 };
 showDialog.value = true;
};
const fillEditForm = (binding: BindingDetail) => {
 currentId.value = binding.id;
 currentVersion.value = binding.version;
 form.value = {
 propId: binding.propId,
 crewId: binding.crewId,
 startDate: binding.startDate,
 endDate: binding.endDate,
 bindingType: binding.bindingType,
 remark: binding.remark
 };
};
const handleEdit = (binding: BindingDetail) => {
 isEdit.value = true;
 fillEditForm(binding);
 showDialog.value = true;
};
const handleCancel = async (binding: BindingDetail) => {
 try {
 // 取消原因必填：不填或只填空格时拦截，不允许进入下一步
 const { value: reason } = await ElMessageBox.prompt('请输入取消原因', '取消绑定', {
 inputPlaceholder: '请输入原因',
 inputValidator: (value: string) => !!(value && value.trim()),
 inputErrorMessage: '取消原因不能为空'
 });
 await bindingApi.cancel(binding.id, reason.trim(), 'admin');
 ElMessage.success('绑定已取消');
 fetchData();
 }
 catch (error: any) {
 // 用户主动关闭对话框不提示；后端校验拦截时展示失败原因
 if (error === 'cancel' || error === 'close') return;
 const message = error?.response?.data?.message || '取消失败';
 ElMessage.error(message);
 }
};
const handleConflictCheck = async () => {
 if (!form.value.propId || !form.value.startDate || !form.value.endDate) {
 ElMessage.warning('请选择道具和日期');
 return;
 }
 if (genreMismatch.value) {
 ElMessage.error(genreMismatchMessage.value);
 return;
 }
 try {
 const response = await bindingApi.checkConflict(
 form.value.propId,
 form.value.startDate,
 form.value.endDate,
 isEdit.value && currentId.value ? currentId.value : undefined
 );
 const result = response.data.data;
 if (result.hasConflict) {
 ElMessage.warning(`档期冲突：${result.conflictMessage}`);
 }
 else {
 ElMessage.success('档期可用，无冲突');
 }
 }
 catch (error) {
 ElMessage.error('冲突检测失败');
 }
};
const handleSubmit = async () => {
 try {
 if (!form.value.propId || !form.value.crewId || !form.value.startDate || !form.value.endDate) {
 ElMessage.warning('请填写完整信息');
 return;
 }
 // 片种不符的跨片种绑定在前端直接拦截，不提交后端
 if (!isEdit.value && genreMismatch.value) {
 ElMessage.error(genreMismatchMessage.value);
 return;
 }
 if (isEdit.value && currentId.value) {
 const updateReq: BindingUpdateRequest = {
 id: currentId.value,
 version: currentVersion.value,
 startDate: form.value.startDate,
 endDate: form.value.endDate,
 bindingType: form.value.bindingType,
 remark: form.value.remark
 };
 await bindingApi.update(updateReq);
 ElMessage.success('更新成功');
 }
 else {
 await bindingApi.create(form.value);
 ElMessage.success('创建成功');
 }
 showDialog.value = false;
 fetchData();
 }
 catch (error: any) {
 const message = error?.response?.data?.message || '操作失败';
 if (error?.response?.status === 409 && isEdit.value && currentId.value) {
 // 后保存失败时回填先保存者的最新日期，避免继续拿旧表单重试
 try {
 const latestRes = await bindingApi.getById(currentId.value);
 if (latestRes.data.code === 200) {
 fillEditForm(latestRes.data.data);
 ElMessage.warning(message);
 await fetchData();
 return;
 }
 }
 catch {
 // 最新数据拉取失败时沿用统一错误提示
 }
 }
 ElMessage.error(message);
 }
};
const handleSearch = () => {
};
onMounted(fetchData);
</script>

<template>
  <div class="binding-manager">
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增绑定
      </el-button>
    </div>
    
    <div class="search-bar">
      <el-input v-model="searchCrewName" placeholder="按剧组名称搜索" style="width: 200px; margin-right: 10px;" />
      <el-date-picker v-model="searchStartDate" type="date" placeholder="开始日期" style="margin-right: 10px;" />
      <el-date-picker v-model="searchEndDate" type="date" placeholder="结束日期" style="margin-right: 10px;" />
      <el-button @click="handleSearch">搜索</el-button>
      <el-button @click="searchCrewName = ''; searchStartDate = ''; searchEndDate = ''">重置</el-button>
    </div>
    
    <el-table :data="filteredBindings" :loading="loading" border stripe>
      <el-table-column prop="propName" label="道具名称" width="150" />
      <el-table-column prop="propCode" label="道具编号" width="100" />
      <el-table-column prop="sceneType" label="场景类型" width="100">
        <template #default="{ row }">
          <el-tag>{{ row.sceneType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="crewName" label="剧组名称" width="150" />
      <el-table-column prop="crewGenre" label="剧组片种" width="100">
        <template #default="{ row }">
          <el-tag :type="row.crewGenre === '古装' ? 'warning' : 'primary'">{{ row.crewGenre || '未登记' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="projectName" label="项目名称" width="200" />
      <el-table-column prop="startDate" label="开始日期" width="120" />
      <el-table-column prop="endDate" label="结束日期" width="120" />
      <el-table-column prop="bindingType" label="绑定类型" width="100">
        <template #default="{ row }">
          <el-tag :type="row.bindingType === 'formal' ? 'primary' : 'warning'">
            {{ row.bindingType === 'formal' ? '正式绑定' : '临时借用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusMap[row.status]?.class">{{ statusMap[row.status]?.label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" width="150" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleEdit(row)" v-if="row.status === 'active'">编辑</el-button>
          <el-button type="danger" link @click="handleCancel(row)" v-if="row.status === 'active'">取消</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <el-dialog :title="isEdit ? '编辑绑定' : '新增绑定'" v-model="showDialog" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="道具" required>
          <el-select v-model="form.propId" placeholder="请选择道具">
            <el-option v-for="p in props" :key="p.id" :label="`${p.propCode} - ${p.propName}（${p.sceneType}）`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="剧组" required>
          <el-select v-model="form.crewId" placeholder="请选择剧组">
            <el-option v-for="c in crews" :key="c.id" :label="`${c.crewName} - ${c.projectName || ''}（片种：${c.genre || '未登记'}）`" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="genreMismatch && !isEdit">
          <el-alert :title="genreMismatchMessage" type="error" :closable="false" show-icon />
        </el-form-item>
        <el-form-item label="开始日期" required>
          <el-date-picker v-model="form.startDate" type="date" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="结束日期" required>
          <el-date-picker v-model="form.endDate" type="date" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="绑定类型">
          <el-select v-model="form.bindingType">
            <el-option v-for="t in bindingTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="info" @click="handleConflictCheck">冲突检测</el-button>
        <el-button type="primary" @click="handleSubmit" :disabled="genreMismatch && !isEdit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.binding-manager {
  padding: 10px;
}

.toolbar {
  margin-bottom: 15px;
}

.search-bar {
  margin-bottom: 15px;
}
</style>

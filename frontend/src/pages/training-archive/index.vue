<template>
  <view class="archive-page">
    <view class="hero">
      <text class="eyebrow">TRAINING TRACE</text>
      <text class="title">训练档案</text>
      <text class="subtitle">这都是你来时的路</text>
    </view>

    <view v-if="loading" class="empty-card">正在整理你的训练轨迹...</view>

    <template v-else>
      <view class="module-grid">
        <view
          v-for="module in modules"
          :key="module.module"
          class="module-card"
          :class="{ active: activeModule === module.module }"
          @click="switchModule(module.module)"
        >
          <text class="module-name">{{ module.moduleName }}</text>
          <text class="module-count">{{ module.subtitle }}</text>
          <text class="module-title">{{ module.title }}</text>
          <text v-if="module.importantCount" class="module-mark">重要 {{ module.importantCount }}</text>
        </view>
      </view>

      <view class="section-head">
        <text class="section-title">{{ currentModule?.moduleName || '写作' }}</text>
        <text class="section-sub">{{ currentModule?.title || '开始搭建写作骨架' }}</text>
      </view>

      <view v-if="records.length === 0" class="empty-card">
        <text>这个专项还没有训练记录。</text>
        <text class="empty-sub">完成一次 AI 教练反馈后，这里会自动留下题目。</text>
      </view>

      <view v-for="record in records" :key="record.id" class="record-card">
        <view class="record-top">
          <view>
            <text class="record-module">{{ record.moduleName }}</text>
            <text class="record-title">{{ record.title }}</text>
          </view>
          <view class="important-btn" :class="{ on: record.important }" @click="toggleImportant(record)">
            <text>{{ record.important ? '已重要' : '标重要' }}</text>
          </view>
        </view>
        <text class="record-summary">{{ record.summary }}</text>
        <view v-if="record.selectedOption" class="meta-line">
          <text class="meta-label">选择/方向</text>
          <text class="meta-value">{{ record.selectedOption }}</text>
        </view>
        <view v-if="record.userAnswer" class="meta-line">
          <text class="meta-label">我的思路</text>
          <text class="meta-value">{{ record.userAnswer }}</text>
        </view>
        <view v-if="record.coachReply" class="coach-box">
          <text class="coach-label">AI 反馈</text>
          <text class="coach-text">{{ record.coachReply }}</text>
        </view>
        <view class="record-foot">
          <text>{{ record.completed ? '已完成' : '进行中' }}</text>
          <text>{{ formatTime(record.trainedAt) }}</text>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { ensureAuthed } from '@/utils/auth';
import { getUserId } from '@/utils/session';
import { get, post } from '@/utils/request';

interface ModuleStat {
  module: string;
  moduleName: string;
  completedCount: number;
  importantCount: number;
  title: string;
  subtitle: string;
}

interface ArchiveRecord {
  id: number;
  module: string;
  moduleName: string;
  title: string;
  summary: string;
  selectedOption: string;
  userAnswer: string;
  coachReply: string;
  completed: boolean;
  important: boolean;
  trainedAt: string;
}

const loading = ref(true);
const modules = ref<ModuleStat[]>([]);
const records = ref<ArchiveRecord[]>([]);
const activeModule = ref('reading');

const currentModule = computed(() => modules.value.find((item) => item.module === activeModule.value));

async function loadSummary() {
  const userId = getUserId();
  if (!userId) return;
  const res = await get<{ modules: ModuleStat[] }>('/practice/archive/summary', { userId });
  modules.value = res.data.modules || [];
  if (!modules.value.find((item) => item.module === activeModule.value)) {
    activeModule.value = modules.value[0]?.module || 'reading';
  }
}

async function loadRecords() {
  const userId = getUserId();
  if (!userId) return;
  const res = await get<ArchiveRecord[]>('/practice/archive', { userId, module: activeModule.value });
  records.value = res.data || [];
}

async function switchModule(module: string) {
  activeModule.value = module;
  await loadRecords();
}

async function toggleImportant(record: ArchiveRecord) {
  const userId = getUserId();
  if (!userId) return;
  const next = !record.important;
  const res = await post<ArchiveRecord>(`/practice/archive/${record.id}/important?userId=${userId}&important=${next}`, {});
  record.important = res.data.important;
  await loadSummary();
}

function formatTime(value: string) {
  if (!value) return '';
  return value.replace('T', ' ').slice(0, 16);
}

onShow(async () => {
  loading.value = true;
  try {
    await ensureAuthed();
    await loadSummary();
    await loadRecords();
  } catch (e) {
    uni.showToast({ title: '题目回看加载失败', icon: 'none' });
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.archive-page { min-height: 100vh; padding: 34rpx 26rpx 80rpx; background:
  radial-gradient(circle at top left, rgba(31, 111, 91, 0.16), transparent 32%),
  linear-gradient(180deg, #f1f7ef 0%, #f8f5ef 100%); }
.hero,.module-card,.record-card,.empty-card { background: rgba(255,255,255,0.9); border: 1rpx solid rgba(255,255,255,0.7); box-shadow: 0 18rpx 38rpx rgba(35, 57, 45, 0.08); }
.hero { padding: 32rpx; border-radius: 32rpx; }
.eyebrow { display:block; font-size:22rpx; letter-spacing:4rpx; color:#1f6f5b; font-weight:900; }
.title { display:block; margin-top:12rpx; font-size:48rpx; font-weight:900; color:#1f2c28; }
.subtitle { display:block; margin-top:10rpx; font-size:24rpx; color:rgba(31,44,40,0.52); }
.module-grid { display:grid; grid-template-columns: repeat(2, 1fr); gap:14rpx; margin-top:24rpx; }
.module-card { position:relative; min-height:178rpx; padding:24rpx; border-radius:26rpx; overflow:hidden; }
.module-card.active { border-color:rgba(31,111,91,0.45); background:linear-gradient(180deg, #e8f6eb 0%, #fffef8 100%); }
.module-name { display:block; font-size:30rpx; font-weight:900; color:#1f2c28; }
.module-count { display:block; margin-top:10rpx; font-size:22rpx; color:#6f7a70; }
.module-title { display:block; margin-top:16rpx; font-size:22rpx; line-height:1.45; color:#1f6f5b; font-weight:800; }
.module-mark { position:absolute; top:18rpx; right:18rpx; padding:6rpx 12rpx; border-radius:999rpx; background:#1f6f5b; color:#fff; font-size:19rpx; }
.section-head { margin:30rpx 0 16rpx; }
.section-title { display:block; font-size:34rpx; font-weight:900; color:#1f2c28; }
.section-sub { display:block; margin-top:8rpx; font-size:23rpx; color:#718074; }
.empty-card { margin-top:20rpx; padding:34rpx; border-radius:28rpx; color:#667469; font-size:25rpx; line-height:1.6; }
.empty-sub { display:block; margin-top:8rpx; color:#8a958c; font-size:23rpx; }
.record-card { margin-top:16rpx; padding:26rpx; border-radius:28rpx; }
.record-top { display:flex; justify-content:space-between; gap:18rpx; align-items:flex-start; }
.record-module { display:block; width:max-content; padding:7rpx 12rpx; border-radius:999rpx; background:#e8f6eb; color:#1f6f5b; font-size:20rpx; font-weight:900; }
.record-title { display:block; margin-top:12rpx; font-size:30rpx; font-weight:900; color:#1f2c28; line-height:1.35; }
.important-btn { flex-shrink:0; padding:12rpx 18rpx; border-radius:999rpx; background:#f0eee7; color:#746d62; font-size:22rpx; font-weight:800; }
.important-btn.on { background:#f6c567; color:#382814; }
.record-summary { display:block; margin-top:16rpx; font-size:24rpx; line-height:1.65; color:#657166; }
.meta-line { margin-top:16rpx; padding:16rpx; border-radius:18rpx; background:#f7faf6; }
.meta-label { display:block; font-size:21rpx; color:#1f6f5b; font-weight:900; }
.meta-value { display:block; margin-top:8rpx; font-size:23rpx; line-height:1.6; color:#3e4b43; white-space:pre-wrap; }
.coach-box { margin-top:16rpx; padding:18rpx; border-radius:20rpx; background:#fff9ed; }
.coach-label { display:block; font-size:21rpx; color:#9a5a12; font-weight:900; }
.coach-text { display:block; margin-top:8rpx; font-size:23rpx; line-height:1.65; color:#5f5244; white-space:pre-wrap; }
.record-foot { display:flex; justify-content:space-between; margin-top:18rpx; color:#899387; font-size:21rpx; }
</style>

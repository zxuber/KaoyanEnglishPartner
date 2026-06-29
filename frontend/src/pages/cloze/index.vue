<template>
  <view class="cloze-page">
    <view class="hero">
      <text class="eyebrow">CLOZE COACH</text>
      <text class="title">完形填空</text>
      <text class="sub">不要只选答案，先说依据。AI 会判断你是在看语义、搭配、逻辑，还是在猜。</text>
    </view>

    <view v-if="loading" class="card">正在加载完形训练...</view>

    <template v-else>
      <view class="passage-card">
        <text class="label">{{ task?.title }}</text>
        <text class="passage">{{ task?.passage }}</text>
        <text class="stem">{{ task?.stem }}</text>
      </view>

      <view class="option-grid">
        <view
          v-for="option in task?.options"
          :key="option.label"
          class="option"
          :class="{ active: selectedOption === option.label }"
          @click="selectedOption = option.label"
        >
          <text class="option-label">{{ option.label }}</text>
          <text class="option-content">{{ option.content }}</text>
        </view>
      </view>

      <view class="reason-card">
        <text class="section-title">说出你的选择理由</text>
        <textarea
          v-model="reasoning"
          class="textarea"
          maxlength="800"
          placeholder="比如：我觉得这里后面接 broader public understanding，需要一个表示“促进、有助于”的搭配。"
        />
        <button class="primary-btn" :disabled="submitting" @click="submitCoach">{{ submitting ? 'AI 正在追问...' : '提交理由' }}</button>
      </view>

      <view v-if="coach.coachReply" class="coach-card">
        <text class="coach-title">AI 完形教练</text>
        <text class="coach-text">{{ coach.coachReply }}</text>
        <text v-if="coach.nextQuestion" class="coach-question">{{ coach.nextQuestion }}</text>
        <view v-if="coach.answer" class="answer-box">
          <text class="answer-title">答案与错因</text>
          <text class="answer-text">{{ coach.answer }}</text>
          <text class="answer-text">{{ coach.explanation }}</text>
        </view>
      </view>

      <view v-if="coach.assetCandidates?.length" class="section">
        <text class="section-title">可沉淀到误解本</text>
        <view v-for="asset in coach.assetCandidates" :key="asset.sourceText" class="asset-card">
          <view>
            <text class="asset-text">{{ asset.sourceText }}</text>
            <text class="asset-translation">{{ asset.translation }}</text>
          </view>
          <button class="asset-btn" @click="addAsset(asset)">加入误解本</button>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { ensureAuthed } from '@/utils/auth';
import { getUserId } from '@/utils/session';
import { get, post } from '@/utils/request';

interface ClozeTask {
  taskId: string;
  title: string;
  passage: string;
  stem: string;
  options: Array<{ label: string; content: string }>;
}

interface CoachResponse {
  coachReply?: string;
  nextQuestion?: string;
  answer?: string;
  explanation?: string;
  assetCandidates?: Array<{ type: string; sourceText: string; translation: string; sourceModule: string }>;
}

const loading = ref(true);
const submitting = ref(false);
const task = ref<ClozeTask | null>(null);
const selectedOption = ref('B');
const reasoning = ref('');
const turn = ref(1);
const coach = ref<CoachResponse>({});

async function loadTask() {
  const userId = getUserId();
  if (!userId) return;
  const res = await get<ClozeTask>('/practice/cloze/task', { userId });
  task.value = res.data;
}

async function submitCoach() {
  const userId = getUserId();
  if (!userId || !task.value) return;
  if (!selectedOption.value) {
    uni.showToast({ title: '先选择一个选项', icon: 'none' });
    return;
  }
  submitting.value = true;
  try {
    const res = await post<CoachResponse>('/practice/cloze/coach', {
      userId,
      taskId: task.value.taskId,
      selectedOption: selectedOption.value,
      userAnswer: reasoning.value,
      turn: turn.value,
    });
    coach.value = res.data;
    turn.value += 1;
  } finally {
    submitting.value = false;
  }
}

async function addAsset(asset: { type: string; sourceText: string; translation: string; sourceModule: string }) {
  const userId = getUserId();
  if (!userId) return;
  await post('/mistakes', {
    userId,
    type: asset.type,
    sourceText: asset.sourceText,
    translation: asset.translation,
    sourceModule: asset.sourceModule || '完形',
    articleId: task.value?.taskId,
  });
  uni.showToast({ title: '已加入误解本', icon: 'success' });
}

onLoad(async () => {
  try {
    await ensureAuthed();
    await loadTask();
  } catch (e) {
    uni.showToast({ title: '完形训练加载失败', icon: 'none' });
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.cloze-page { min-height: 100vh; padding: 34rpx 26rpx 80rpx; background:
  radial-gradient(circle at top left, rgba(91, 33, 182, 0.13), transparent 30%),
  linear-gradient(180deg, #f7f6ff 0%, #f8f5ef 100%); }
.hero,.card,.passage-card,.reason-card,.coach-card,.asset-card { background: rgba(255,255,255,0.9); border: 1rpx solid rgba(255,255,255,0.66); box-shadow: 0 18rpx 36rpx rgba(76,35,43,0.08); }
.hero { padding: 30rpx; border-radius: 30rpx; }
.eyebrow { display:block; font-size:22rpx; letter-spacing:4rpx; color:#6d28d9; font-weight:900; }
.title { display:block; margin-top:12rpx; font-size:46rpx; font-weight:900; color:#2d2341; }
.sub { display:block; margin-top:12rpx; font-size:24rpx; line-height:1.68; color:#6e6683; }
.card,.passage-card,.reason-card,.coach-card { margin-top:22rpx; padding:28rpx; border-radius:26rpx; color:#6e6683; }
.label { display:block; font-size:22rpx; font-weight:900; color:#6d28d9; }
.passage { display:block; margin-top:14rpx; font-size:30rpx; line-height:1.75; color:#2d2341; font-weight:800; }
.stem { display:block; margin-top:18rpx; font-size:25rpx; color:#6e6683; }
.option-grid { display:grid; grid-template-columns: repeat(2, 1fr); gap:14rpx; margin-top:20rpx; }
.option { min-height:120rpx; padding:22rpx; border-radius:22rpx; background:#fff; border:2rpx solid transparent; box-shadow: 0 12rpx 28rpx rgba(76,35,43,0.06); }
.option.active { border-color:#6d28d9; background:#f1ebff; }
.option-label { display:block; width:44rpx; height:44rpx; line-height:44rpx; text-align:center; border-radius:50%; background:#6d28d9; color:#fff; font-weight:900; }
.option-content { display:block; margin-top:14rpx; font-size:25rpx; color:#2d2341; font-weight:800; }
.section-title { display:block; margin-bottom:16rpx; font-size:30rpx; font-weight:900; color:#2d2341; }
.textarea { width:100%; min-height:170rpx; padding:20rpx; border-radius:20rpx; background:#fbfaff; box-sizing:border-box; font-size:25rpx; line-height:1.6; color:#2d2341; }
.primary-btn { margin-top:18rpx; border-radius:999rpx; background:#6d28d9; color:#fff; font-weight:900; }
.coach-title { display:block; font-size:30rpx; font-weight:900; color:#2d2341; }
.coach-text,.coach-question,.answer-text { display:block; margin-top:14rpx; font-size:25rpx; line-height:1.7; color:#62587a; white-space:pre-wrap; }
.coach-question { padding:18rpx; border-radius:18rpx; background:#f1ebff; color:#5b21b6; }
.answer-box { margin-top:18rpx; padding:18rpx; border-radius:18rpx; background:#f1ebff; }
.answer-title { display:block; font-size:23rpx; color:#5b21b6; font-weight:900; }
.section { margin-top:28rpx; }
.asset-card { display:flex; justify-content:space-between; gap:18rpx; align-items:center; padding:22rpx; border-radius:22rpx; margin-top:14rpx; }
.asset-text { display:block; font-size:25rpx; font-weight:800; color:#2d2341; }
.asset-translation { display:block; margin-top:8rpx; font-size:22rpx; color:#6e6683; }
.asset-btn { flex-shrink:0; margin:0; padding:0 18rpx; height:60rpx; line-height:60rpx; border-radius:999rpx; background:#1f6f5b; color:#fff; font-size:22rpx; }
</style>

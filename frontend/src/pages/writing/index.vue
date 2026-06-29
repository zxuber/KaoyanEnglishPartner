<template>
  <view class="writing-page">
    <view class="hero">
      <text class="eyebrow">WRITING COACH</text>
      <text class="title">{{ task?.title || '写作搭建训练' }}</text>
      <text class="sub">先选结构方向，再用大白话说想法。AI 负责纠偏，并把可复用表达沉淀到误解本。</text>
    </view>

    <view v-if="loading" class="card muted">正在生成写作训练...</view>

    <template v-else>
      <view class="prompt-card">
        <text class="label">今日题目</text>
        <text class="prompt">{{ task?.prompt }}</text>
        <text class="goal">{{ task?.goal }}</text>
      </view>

      <view class="section">
        <text class="section-title">先选你的写作方向</text>
        <view v-for="question in task?.thinkingQuestions" :key="question.id" class="question-card">
          <text class="question-title">{{ question.title }}</text>
          <view class="option-list">
            <view
              v-for="option in question.options"
              :key="option.id"
              class="option"
              :class="{ active: selected[question.id] === option.id }"
              @click="selected[question.id] = option.id"
            >
              <text class="option-label">{{ option.label }}</text>
              <text class="option-text">{{ option.content }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="section">
        <text class="section-title">用大白话补充你的想法</text>
        <textarea
          v-model="userAnswer"
          class="textarea"
          maxlength="1000"
          placeholder="不用写正式英文。比如：我想先说明对方阅读没方向，然后建议他先做定位，再复盘错因。"
        />
        <button class="primary-btn" :disabled="submitting" @click="submitCoach">{{ submitting ? 'AI 正在纠偏...' : '提交给 AI 教练' }}</button>
      </view>

      <view v-if="coach.coachReply" class="coach-card">
        <text class="coach-title">AI 纠偏</text>
        <text class="coach-text">{{ coach.coachReply }}</text>
        <text v-if="coach.nextQuestion" class="coach-question">{{ coach.nextQuestion }}</text>
        <view v-if="coach.answer" class="answer-box">
          <text class="answer-title">本题结构骨架</text>
          <text class="answer-text">{{ coach.answer }}</text>
        </view>
      </view>

      <view v-if="coach.assetCandidates?.length" class="section">
        <text class="section-title">可沉淀表达</text>
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
import { reactive, ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { ensureAuthed } from '@/utils/auth';
import { getUserId } from '@/utils/session';
import { get, post } from '@/utils/request';

interface WritingTask {
  taskId: string;
  writingType: string;
  title: string;
  prompt: string;
  goal: string;
  thinkingQuestions: Array<{
    id: string;
    title: string;
    options: Array<{ id: string; label: string; content: string }>;
  }>;
}

interface CoachResponse {
  coachReply?: string;
  nextQuestion?: string;
  answer?: string;
  assetCandidates?: Array<{ type: string; sourceText: string; translation: string; sourceModule: string }>;
}

const writingType = ref('small');
const loading = ref(true);
const submitting = ref(false);
const task = ref<WritingTask | null>(null);
const selected = reactive<Record<string, string>>({});
const userAnswer = ref('');
const coach = ref<CoachResponse>({});
const turn = ref(1);

async function loadTask() {
  const userId = getUserId();
  if (!userId) return;
  const res = await get<WritingTask>('/practice/writing/task', { userId, type: writingType.value });
  task.value = res.data;
  res.data.thinkingQuestions.forEach((question) => {
    selected[question.id] = question.options[0]?.id || '';
  });
}

async function submitCoach() {
  const userId = getUserId();
  if (!userId || !task.value) return;
  const selectedOptionIds = Object.values(selected).filter(Boolean);
  if (!selectedOptionIds.length) {
    uni.showToast({ title: '先选写作方向', icon: 'none' });
    return;
  }
  submitting.value = true;
  try {
    const res = await post<CoachResponse>('/practice/writing/coach', {
      userId,
      taskId: task.value.taskId,
      practiceType: task.value.writingType,
      selectedOptionIds,
      userAnswer: userAnswer.value,
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
    sourceModule: asset.sourceModule || '写作',
    articleId: task.value?.taskId,
  });
  uni.showToast({ title: '已加入误解本', icon: 'success' });
}

onLoad(async (query) => {
  writingType.value = query?.type === 'large' ? 'large' : 'small';
  try {
    await ensureAuthed();
    await loadTask();
  } catch (e) {
    uni.showToast({ title: '写作训练加载失败', icon: 'none' });
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.writing-page { min-height: 100vh; padding: 34rpx 26rpx 80rpx; background:
  radial-gradient(circle at top left, rgba(154, 90, 18, 0.14), transparent 32%),
  linear-gradient(180deg, #fff7ec 0%, #f8f5ef 100%); }
.hero,.card,.prompt-card,.question-card,.coach-card,.asset-card { background: rgba(255,255,255,0.9); border: 1rpx solid rgba(255,255,255,0.68); box-shadow: 0 18rpx 36rpx rgba(76,35,43,0.08); }
.hero { padding: 30rpx; border-radius: 30rpx; }
.eyebrow { display:block; font-size:22rpx; letter-spacing:4rpx; color:#9a5a12; font-weight:800; }
.title { display:block; margin-top:12rpx; font-size:46rpx; font-weight:900; color:#35261b; }
.sub { display:block; margin-top:12rpx; font-size:24rpx; line-height:1.68; color:#766354; }
.card,.prompt-card,.coach-card { margin-top: 22rpx; padding: 28rpx; border-radius: 26rpx; }
.muted { color:#766354; }
.label { display:block; font-size:22rpx; color:#9a5a12; font-weight:800; }
.prompt { display:block; margin-top:12rpx; font-size:29rpx; line-height:1.7; color:#30231a; font-weight:700; }
.goal { display:block; margin-top:14rpx; font-size:23rpx; color:#8a6d54; }
.section { margin-top: 28rpx; }
.section-title { display:block; margin-bottom:16rpx; font-size:30rpx; font-weight:900; color:#35261b; }
.question-card { padding: 24rpx; border-radius: 24rpx; margin-bottom: 16rpx; }
.question-title { display:block; font-size:27rpx; font-weight:800; color:#35261b; }
.option-list { display:flex; flex-direction:column; gap:12rpx; margin-top:16rpx; }
.option { display:flex; gap:14rpx; padding:18rpx; border-radius:18rpx; background:#fffaf3; border:2rpx solid transparent; }
.option.active { border-color:#9a5a12; background:#fff1d8; }
.option-label { flex-shrink:0; width:42rpx; height:42rpx; border-radius:50%; display:flex; align-items:center; justify-content:center; background:#9a5a12; color:#fff; font-weight:900; }
.option-text { flex:1; font-size:24rpx; line-height:1.55; color:#554336; }
.textarea { width:100%; min-height:190rpx; padding:22rpx; border-radius:22rpx; background:#fff; box-sizing:border-box; font-size:25rpx; line-height:1.6; color:#35261b; }
.primary-btn { margin-top:18rpx; border-radius:999rpx; background:#9a5a12; color:#fff; font-weight:900; }
.coach-title { display:block; font-size:30rpx; font-weight:900; color:#35261b; }
.coach-text,.coach-question,.answer-text { display:block; margin-top:14rpx; font-size:25rpx; line-height:1.7; color:#604f42; white-space:pre-wrap; }
.coach-question { padding:18rpx; border-radius:18rpx; background:#fff4df; color:#8a4f0f; }
.answer-box { margin-top:18rpx; padding:18rpx; border-radius:18rpx; background:#f9efe2; }
.answer-title { display:block; font-size:23rpx; font-weight:900; color:#8a4f0f; }
.asset-card { display:flex; justify-content:space-between; gap:18rpx; align-items:center; padding:22rpx; border-radius:22rpx; margin-bottom:14rpx; }
.asset-text { display:block; font-size:25rpx; font-weight:800; color:#35261b; }
.asset-translation { display:block; margin-top:8rpx; font-size:22rpx; color:#766354; }
.asset-btn { flex-shrink:0; margin:0; padding:0 18rpx; height:60rpx; line-height:60rpx; border-radius:999rpx; background:#1f6f5b; color:#fff; font-size:22rpx; }
</style>

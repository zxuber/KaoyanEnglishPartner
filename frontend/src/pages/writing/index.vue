<template>
  <view class="writing-page">
    <view class="hero">
      <text class="eyebrow">WRITING COACH</text>
      <text class="title">{{ task?.title || '写作搭建训练' }}</text>
      <text class="sub">先选方向，再进入高质量框架。你只需要在关键空白处自由写，AI 负责批改和表达升级。</text>
    </view>

    <view v-if="loading" class="card muted">正在生成写作训练...</view>

    <template v-else>
      <view class="prompt-card">
        <text class="label">今日题目</text>
        <text class="prompt">{{ task?.prompt }}</text>
        <text class="goal">{{ task?.goal }}</text>
      </view>

      <view class="phase-bar">
        <text :class="{ active: phase === 'direction' }">1 选方向</text>
        <text :class="{ active: phase === 'frame' }">2 填框架</text>
        <text :class="{ active: phase === 'review' }">3 看批改</text>
      </view>

      <view v-if="phase === 'direction'" class="section">
        <text class="section-title">先选你的写作方向</text>
        <view v-if="currentQuestion" class="question-card step-card">
          <view class="step-head">
            <text class="step-count">{{ directionIndex + 1 }} / {{ task?.thinkingQuestions.length || 0 }}</text>
            <text class="step-hint">每次只做一个判断</text>
          </view>
          <text class="question-title">{{ currentQuestion.title }}</text>
          <view class="option-list">
            <view
              v-for="option in currentQuestion.options"
              :key="option.id"
              class="option"
              :class="{ active: selected[currentQuestion.id] === option.id }"
              @click="selected[currentQuestion.id] = option.id"
            >
              <text class="option-label">{{ option.label }}</text>
              <text class="option-text">{{ option.content }}</text>
            </view>
          </view>
          <view class="step-actions">
            <button class="ghost-btn" :disabled="directionIndex === 0" @click="prevDirection">上一步</button>
            <button class="primary-btn small" @click="nextDirection">{{ isLastDirection ? '继续说想法' : '下一步' }}</button>
          </view>
        </view>
      </view>

      <view v-if="phase === 'thought'" class="section">
        <text class="section-title">用大白话补充你的想法</text>
        <text class="frame-instruction">可以说作文思路，也可以直接说你卡在哪里。AI 会先识别你的真实状态。</text>
        <textarea
          v-model="userAnswer"
          class="textarea"
          maxlength="1000"
          placeholder="可以说作文思路，也可以直接说你卡在哪里。比如：我不会起笔、不知道中间分几段、词汇量不够。"
        />
        <view class="step-actions">
          <button class="ghost-btn" @click="phase = 'direction'">返回选方向</button>
          <button class="primary-btn small" :disabled="submitting" @click="submitDirection">{{ submitting ? 'AI 正在分析...' : '提交方向，生成框架' }}</button>
        </view>
      </view>

      <view v-if="coach.coachReply" class="coach-card">
        <text class="coach-title">{{ phase === 'review' ? 'AI 批改' : 'AI 方向分析' }}</text>
        <text class="coach-text">{{ coach.coachReply }}</text>
        <text v-if="coach.nextQuestion" class="coach-question">{{ coach.nextQuestion }}</text>
        <view v-if="phase === 'review' && coach.answer" class="answer-box">
          <text class="answer-title">本题结构骨架</text>
          <text class="answer-text">{{ coach.answer }}</text>
        </view>
      </view>

      <view v-if="phase === 'frame' && coach.writingFrame" class="section">
        <text class="section-title">{{ coach.writingFrame.title }}</text>
        <text class="frame-instruction">{{ coach.writingFrame.instruction }}</text>
        <view v-if="currentFrameLine" class="frame-card step-card">
          <view class="step-head">
            <text class="step-count">{{ frameIndex + 1 }} / {{ coach.writingFrame.lines.length }}</text>
            <text class="step-hint">一个空白就是一个小任务</text>
          </view>
          <text class="frame-label">{{ currentFrameLine.label }}</text>
          <text class="frame-before">{{ currentFrameLine.beforeText }}</text>
          <textarea
            v-model="frameAnswers[currentFrameLine.id]"
            class="frame-textarea"
            :maxlength="currentFrameLine.maxLength || 500"
            :placeholder="currentFrameLine.placeholder"
          />
          <text class="frame-after">{{ currentFrameLine.afterText }}</text>
          <view class="step-actions">
            <button class="ghost-btn" :disabled="frameIndex === 0" @click="prevFrame">上一空</button>
            <button v-if="!isLastFrame" class="primary-btn small" @click="nextFrame">下一空</button>
            <button v-else class="primary-btn small" :disabled="submitting" @click="submitFrame">{{ submitting ? 'AI 正在批改...' : '提交批改' }}</button>
          </view>
        </view>
      </view>

      <view v-if="phase === 'review' && coach.assetCandidates?.length" class="section">
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
import { computed, reactive, ref } from 'vue';
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
  explanation?: string;
  writingFrame?: {
    title: string;
    instruction: string;
    lines: Array<{
      id: string;
      beforeText: string;
      afterText: string;
      label: string;
      placeholder: string;
      maxLength: number;
    }>;
  };
  assetCandidates?: Array<{ type: string; sourceText: string; translation: string; sourceModule: string }>;
}

const writingType = ref('small');
const loading = ref(true);
const submitting = ref(false);
const task = ref<WritingTask | null>(null);
const selected = reactive<Record<string, string>>({});
const frameAnswers = reactive<Record<string, string>>({});
const userAnswer = ref('');
const coach = ref<CoachResponse>({});
const turn = ref(1);
const phase = ref<'direction' | 'thought' | 'frame' | 'review'>('direction');
const directionIndex = ref(0);
const frameIndex = ref(0);

const currentQuestion = computed(() => task.value?.thinkingQuestions?.[directionIndex.value] || null);
const isLastDirection = computed(() => directionIndex.value >= ((task.value?.thinkingQuestions.length || 1) - 1));
const currentFrameLine = computed(() => coach.value.writingFrame?.lines?.[frameIndex.value] || null);
const isLastFrame = computed(() => frameIndex.value >= ((coach.value.writingFrame?.lines?.length || 1) - 1));

async function loadTask() {
  const userId = getUserId();
  if (!userId) return;
  const res = await get<WritingTask>('/practice/writing/task', { userId, type: writingType.value });
  task.value = res.data;
  res.data.thinkingQuestions.forEach((question) => {
    selected[question.id] = question.options[0]?.id || '';
  });
}

function prevDirection() {
  directionIndex.value = Math.max(0, directionIndex.value - 1);
}

function nextDirection() {
  if (!currentQuestion.value || !selected[currentQuestion.value.id]) {
    uni.showToast({ title: '先选择一个方向', icon: 'none' });
    return;
  }
  if (isLastDirection.value) {
    phase.value = 'thought';
    return;
  }
  directionIndex.value += 1;
}

function prevFrame() {
  frameIndex.value = Math.max(0, frameIndex.value - 1);
}

function nextFrame() {
  frameIndex.value = Math.min((coach.value.writingFrame?.lines?.length || 1) - 1, frameIndex.value + 1);
}

async function submitDirection() {
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
      step: 1,
      turn: turn.value,
    });
    coach.value = res.data;
    if (res.data.writingFrame?.lines?.length) {
      res.data.writingFrame.lines.forEach((line) => {
        if (frameAnswers[line.id] === undefined) frameAnswers[line.id] = '';
      });
      frameIndex.value = 0;
      phase.value = 'frame';
    }
    turn.value += 1;
  } finally {
    submitting.value = false;
  }
}

async function submitFrame() {
  const userId = getUserId();
  if (!userId || !task.value || !coach.value.writingFrame) return;
  const writingFrameAnswers = coach.value.writingFrame.lines.map((line) => ({
    slotId: line.id,
    label: line.label,
    prompt: line.placeholder,
    answer: frameAnswers[line.id] || '',
  }));
  const hasAnyAnswer = writingFrameAnswers.some((item) => item.answer.trim());
  if (!hasAnyAnswer) {
    uni.showToast({ title: '先至少填一个空白', icon: 'none' });
    return;
  }
  submitting.value = true;
  try {
    const res = await post<CoachResponse>('/practice/writing/coach', {
      userId,
      taskId: task.value.taskId,
      practiceType: task.value.writingType,
      selectedOptionIds: Object.values(selected).filter(Boolean),
      userAnswer: userAnswer.value,
      writingFrameAnswers,
      step: 2,
      turn: turn.value,
    });
    coach.value = res.data;
    phase.value = 'review';
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
.hero,.card,.prompt-card,.question-card,.coach-card,.asset-card,.frame-card { background: rgba(255,255,255,0.9); border: 1rpx solid rgba(255,255,255,0.68); box-shadow: 0 18rpx 36rpx rgba(76,35,43,0.08); }
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
.phase-bar { display:flex; gap:12rpx; margin-top:22rpx; padding:10rpx; border-radius:999rpx; background:rgba(255,255,255,0.7); }
.phase-bar text { flex:1; text-align:center; padding:14rpx 8rpx; border-radius:999rpx; font-size:21rpx; color:#8a7665; font-weight:800; }
.phase-bar text.active { background:#9a5a12; color:#fff; }
.section-title { display:block; margin-bottom:16rpx; font-size:30rpx; font-weight:900; color:#35261b; }
.question-card { padding: 24rpx; border-radius: 24rpx; margin-bottom: 16rpx; }
.step-card { min-height: 560rpx; display:flex; flex-direction:column; }
.step-head { display:flex; justify-content:space-between; align-items:center; margin-bottom:18rpx; }
.step-count { padding:8rpx 14rpx; border-radius:999rpx; background:#fff1d8; color:#8a4f0f; font-size:21rpx; font-weight:900; }
.step-hint { color:#9b8979; font-size:21rpx; }
.question-title { display:block; font-size:27rpx; font-weight:800; color:#35261b; }
.option-list { display:flex; flex-direction:column; gap:12rpx; margin-top:16rpx; }
.option { display:flex; gap:14rpx; padding:18rpx; border-radius:18rpx; background:#fffaf3; border:2rpx solid transparent; }
.option.active { border-color:#9a5a12; background:#fff1d8; }
.option-label { flex-shrink:0; width:42rpx; height:42rpx; border-radius:50%; display:flex; align-items:center; justify-content:center; background:#9a5a12; color:#fff; font-weight:900; }
.option-text { flex:1; font-size:24rpx; line-height:1.55; color:#554336; }
.textarea { width:100%; min-height:190rpx; padding:22rpx; border-radius:22rpx; background:#fff; box-sizing:border-box; font-size:25rpx; line-height:1.6; color:#35261b; }
.primary-btn { margin-top:18rpx; border-radius:999rpx; background:#9a5a12; color:#fff; font-weight:900; }
.primary-btn.small,.ghost-btn { flex:1; margin-top:0; height:72rpx; line-height:72rpx; border-radius:999rpx; font-size:24rpx; }
.ghost-btn { background:#f5eadb; color:#8a4f0f; font-weight:900; }
.step-actions { display:flex; gap:14rpx; margin-top:auto; padding-top:24rpx; }
.coach-title { display:block; font-size:30rpx; font-weight:900; color:#35261b; }
.coach-text,.coach-question,.answer-text { display:block; margin-top:14rpx; font-size:25rpx; line-height:1.7; color:#604f42; white-space:pre-wrap; }
.coach-question { padding:18rpx; border-radius:18rpx; background:#fff4df; color:#8a4f0f; }
.answer-box { margin-top:18rpx; padding:18rpx; border-radius:18rpx; background:#f9efe2; }
.answer-title { display:block; font-size:23rpx; font-weight:900; color:#8a4f0f; }
.frame-instruction { display:block; margin-bottom:18rpx; font-size:24rpx; line-height:1.6; color:#766354; }
.frame-card { padding:24rpx; border-radius:24rpx; margin-bottom:16rpx; }
.frame-label { display:block; width:max-content; padding:8rpx 14rpx; border-radius:999rpx; background:#fff1d8; color:#8a4f0f; font-size:21rpx; font-weight:900; }
.frame-before,.frame-after { display:block; margin-top:14rpx; font-size:26rpx; line-height:1.65; color:#35261b; font-weight:700; }
.frame-after { margin-top:10rpx; }
.frame-textarea { width:100%; min-height:150rpx; margin-top:14rpx; padding:20rpx; border-radius:20rpx; background:#fffaf3; box-sizing:border-box; font-size:25rpx; line-height:1.6; color:#35261b; }
.asset-card { display:flex; justify-content:space-between; gap:18rpx; align-items:center; padding:22rpx; border-radius:22rpx; margin-bottom:14rpx; }
.asset-text { display:block; font-size:25rpx; font-weight:800; color:#35261b; }
.asset-translation { display:block; margin-top:8rpx; font-size:22rpx; color:#766354; }
.asset-btn { flex-shrink:0; margin:0; padding:0 18rpx; height:60rpx; line-height:60rpx; border-radius:999rpx; background:#1f6f5b; color:#fff; font-size:22rpx; }
</style>

<template>
  <view class="translate-page">
    <view class="hero">
      <text class="eyebrow">TRANSLATION COACH</text>
      <text class="title">翻译专项</text>
      <text class="sub">不直接背译文，先找主干，再拆修饰，最后把中文顺出来。</text>
    </view>

    <view v-if="loading" class="card">正在加载长难句...</view>

    <template v-else>
      <view class="sentence-card">
        <text class="label">{{ task?.title }}</text>
        <text class="sentence">{{ task?.sentence }}</text>
        <text class="hint">{{ task?.hint }}</text>
      </view>

      <view class="steps">
        <view class="step-card">
          <text class="step-index">01</text>
          <text class="step-title">找主干</text>
          <textarea v-model="mainStructure" class="textarea" maxlength="500" placeholder="比如：it prevents students from developing patience" />
        </view>
        <view class="step-card">
          <text class="step-index">02</text>
          <text class="step-title">拆修饰</text>
          <textarea v-model="modifiers" class="textarea" maxlength="500" placeholder="比如：although 引导让步，required for... 修饰 patience" />
        </view>
        <view class="step-card">
          <text class="step-index">03</text>
          <text class="step-title">整句翻译</text>
          <textarea v-model="translation" class="textarea" maxlength="1000" placeholder="写出你的中文译文，不用完美，先让 AI 帮你纠偏。" />
        </view>
      </view>

      <button class="primary-btn" :disabled="submitting" @click="submitCoach">{{ submitting ? 'AI 正在拆句...' : `提交第 ${step} 步` }}</button>

      <view v-if="coach.coachReply" class="coach-card">
        <text class="coach-title">AI 拆句反馈</text>
        <text class="coach-text">{{ coach.coachReply }}</text>
        <view v-if="coach.answer" class="answer-box">
          <text class="answer-title">参考译文</text>
          <text class="answer-text">{{ coach.answer }}</text>
          <text class="answer-text">{{ coach.explanation }}</text>
        </view>
      </view>

      <view v-if="coach.assetCandidates?.length" class="section">
        <text class="section-title">建议沉淀</text>
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

interface TranslationTask {
  taskId: string;
  title: string;
  sentence: string;
  hint: string;
  standardTranslation: string;
  checkpoints: string[];
}

interface CoachResponse {
  coachReply?: string;
  answer?: string;
  explanation?: string;
  assetCandidates?: Array<{ type: string; sourceText: string; translation: string; sourceModule: string }>;
}

const loading = ref(true);
const submitting = ref(false);
const task = ref<TranslationTask | null>(null);
const mainStructure = ref('');
const modifiers = ref('');
const translation = ref('');
const step = ref(1);
const coach = ref<CoachResponse>({});

async function loadTask() {
  const userId = getUserId();
  if (!userId) return;
  const res = await get<TranslationTask>('/practice/translation/task', { userId });
  task.value = res.data;
}

async function submitCoach() {
  const userId = getUserId();
  if (!userId || !task.value) return;
  submitting.value = true;
  try {
    const res = await post<CoachResponse>('/practice/translation/coach', {
      userId,
      taskId: task.value.taskId,
      mainStructure: mainStructure.value,
      modifiers: modifiers.value,
      translation: translation.value,
      step: step.value,
    });
    coach.value = res.data;
    step.value = Math.min(3, step.value + 1);
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
    sourceModule: asset.sourceModule || '翻译',
    articleId: task.value?.taskId,
  });
  uni.showToast({ title: '已加入误解本', icon: 'success' });
}

onLoad(async () => {
  try {
    await ensureAuthed();
    await loadTask();
  } catch (e) {
    uni.showToast({ title: '翻译训练加载失败', icon: 'none' });
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.translate-page { min-height: 100vh; padding: 34rpx 26rpx 80rpx; background:
  radial-gradient(circle at top left, rgba(14, 116, 144, 0.14), transparent 30%),
  linear-gradient(180deg, #f3fbff 0%, #f8f5ef 100%); }
.hero,.card,.sentence-card,.step-card,.coach-card,.asset-card { background: rgba(255,255,255,0.9); border: 1rpx solid rgba(255,255,255,0.66); box-shadow: 0 18rpx 36rpx rgba(46, 84, 103, 0.08); }
.hero { padding: 30rpx; border-radius: 30rpx; }
.eyebrow { display:block; font-size:22rpx; letter-spacing:4rpx; color:#0e7490; font-weight:900; }
.title { display:block; margin-top:12rpx; font-size:46rpx; font-weight:900; color:#173543; }
.sub { display:block; margin-top:12rpx; font-size:24rpx; line-height:1.68; color:#60727b; }
.card,.sentence-card,.coach-card { margin-top:22rpx; padding:28rpx; border-radius:26rpx; color:#60727b; }
.label { display:block; font-size:22rpx; font-weight:900; color:#0e7490; }
.sentence { display:block; margin-top:14rpx; font-size:31rpx; line-height:1.72; color:#173543; font-weight:800; }
.hint { display:block; margin-top:14rpx; font-size:23rpx; color:#60727b; }
.steps { display:flex; flex-direction:column; gap:16rpx; margin-top:24rpx; }
.step-card { padding:24rpx; border-radius:24rpx; }
.step-index { display:block; font-size:21rpx; letter-spacing:2rpx; color:#0e7490; font-weight:900; }
.step-title { display:block; margin-top:8rpx; font-size:29rpx; color:#173543; font-weight:900; }
.textarea { width:100%; min-height:150rpx; margin-top:16rpx; padding:20rpx; border-radius:20rpx; background:#f8fcff; box-sizing:border-box; font-size:25rpx; line-height:1.6; color:#173543; }
.primary-btn { margin-top:22rpx; border-radius:999rpx; background:#0e7490; color:#fff; font-weight:900; }
.coach-title,.section-title { display:block; font-size:30rpx; font-weight:900; color:#173543; }
.coach-text,.answer-text { display:block; margin-top:14rpx; font-size:25rpx; line-height:1.7; color:#536a76; white-space:pre-wrap; }
.answer-box { margin-top:18rpx; padding:18rpx; border-radius:18rpx; background:#eaf7fb; }
.answer-title { display:block; font-size:23rpx; color:#0e7490; font-weight:900; }
.section { margin-top:28rpx; }
.asset-card { display:flex; justify-content:space-between; gap:18rpx; align-items:center; padding:22rpx; border-radius:22rpx; margin-top:14rpx; }
.asset-text { display:block; font-size:25rpx; font-weight:800; color:#173543; }
.asset-translation { display:block; margin-top:8rpx; font-size:22rpx; color:#60727b; }
.asset-btn { flex-shrink:0; margin:0; padding:0 18rpx; height:60rpx; line-height:60rpx; border-radius:999rpx; background:#1f6f5b; color:#fff; font-size:22rpx; }
</style>

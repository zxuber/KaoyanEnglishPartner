<template>
  <view class="tutor-page">
    <view class="hero">
      <text class="eyebrow">INTELLIGENT COACH</text>
      <text class="title">高级智能英语教练系统</text>
      <text class="sub">像一位持续陪跑的英语教练一样，告诉你今天最该补哪里、怎么补。</text>
    </view>

    <view class="diagnose-card">
      <text class="diagnose-label">今日聚焦</text>
      <text class="diagnose-title">{{ focusTitle }}</text>
      <text class="diagnose-sub">{{ focusSub }}</text>
    </view>

    <view class="section">
      <text class="section-title">系统能力</text>
      <view class="grid">
        <view class="feature-card" @click="showToast('今日诊断内容后续接入')">
          <text class="feature-title">今日诊断</text>
          <text class="feature-sub">今天先练什么，为什么先练它</text>
        </view>
        <view class="feature-card" @click="goOnboardingReview">
          <text class="feature-title">提分方案</text>
          <text class="feature-sub">回看你的学习画像、阶段目标与路径</text>
        </view>
        <view class="feature-card" @click="showToast('主题词库内容后续接入')">
          <text class="feature-title">主题词库</text>
          <text class="feature-sub">教育、科技、环保、青年成长等高频主题</text>
        </view>
        <view class="feature-card" @click="showToast('专项纠偏内容后续接入')">
          <text class="feature-title">专项纠偏</text>
          <text class="feature-sub">阅读、翻译、写作、完形、新题型逐项纠偏</text>
        </view>
      </view>
    </view>

    <view class="section">
      <text class="section-title">你现在最适合的 3 个入口</text>
      <view class="route-list">
        <view class="route-card" @click="goPage('/pages/reading/index')">
          <text class="route-title">阅读思路陪练</text>
          <text class="route-sub">先说定位和排错思路，再由 AI 继续追问</text>
        </view>
        <view class="route-card" @click="goPage('/pages/mistake/index')">
          <text class="route-title">误解本轻复盘</text>
          <text class="route-sub">先把最近积累的单词、短句、表达和搭配翻一轮</text>
        </view>
        <view class="route-card" @click="goPage('/pages/special/index')">
          <text class="route-title">写作专项入口</text>
          <text class="route-sub">先在二级页里区分大作文和小作文，再进入训练</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { ensureAuthed } from "@/utils/auth";
import { getUserId } from "@/utils/session";
import { get } from "@/utils/request";

const weakModules = ref<string[]>([]);

const focusTitle = computed(() => {
  if (weakModules.value.includes("阅读")) return "优先把阅读的定位与排错能力拉起来";
  if (weakModules.value.includes("写作")) return "优先把写作表达和结构感拉起来";
  if (weakModules.value.includes("翻译")) return "优先补断句、主干和整句翻译";
  return "先稳住今日主线，再把薄弱项分层补上";
});

const focusSub = computed(() => {
  if (weakModules.value.length) {
    return `系统识别到你当前更需要先补：${weakModules.value.join("、")}`;
  }
  return "后续这里会结合学习画像、误解本和训练记录做动态诊断。";
});

async function loadPlanHint() {
  const userId = getUserId();
  if (!userId) return;
  try {
    const res = await get<any>(`/users/${userId}/plan`);
    const profile = res.data?.diagnosisSummary || [];
    const merged = Array.isArray(profile) ? profile.join(" ") : "";
    const next: string[] = [];
    if (merged.includes("阅读")) next.push("阅读");
    if (merged.includes("写作")) next.push("写作");
    if (merged.includes("翻译")) next.push("翻译");
    if (merged.includes("词汇")) next.push("词汇");
    weakModules.value = next;
  } catch (e) {}
}

function showToast(title: string) {
  uni.showToast({ title, icon: "none" });
}

function goPage(url: string) {
  uni.navigateTo({ url });
}

function goOnboardingReview() {
  uni.navigateTo({ url: "/pages/onboarding/index?review=1" });
}

onLoad(async () => {
  try {
    await ensureAuthed();
  } catch (e) {
    uni.showToast({ title: "微信登录失败，请重试", icon: "none" });
    return;
  }
  await loadPlanHint();
});
</script>

<style scoped>
.tutor-page { min-height: 100vh; padding: 34rpx 26rpx 70rpx; background:
  radial-gradient(circle at top right, rgba(159, 18, 57, 0.12), transparent 30%),
  linear-gradient(180deg, #faf4f6 0%, #f8f5ef 100%); }
.hero,.diagnose-card,.feature-card,.route-card { background: rgba(255,255,255,0.9); border:1rpx solid rgba(255,255,255,0.66); box-shadow:0 18rpx 36rpx rgba(76,35,43,0.08); }
.hero { padding:28rpx; border-radius:28rpx; }
.eyebrow { display:block; font-size:22rpx; letter-spacing:3rpx; color:#9f1239; }
.title { display:block; margin-top:12rpx; font-size:44rpx; font-weight:800; color:#3d1f27; }
.sub { display:block; margin-top:12rpx; font-size:24rpx; line-height:1.68; color:#6f5b5f; }
.diagnose-card { margin-top:18rpx; padding:28rpx; border-radius:28rpx; background:linear-gradient(135deg, #9f1239 0%, #d9466e 100%); }
.diagnose-label { display:block; font-size:22rpx; color:rgba(255,248,251,0.72); }
.diagnose-title { display:block; margin-top:12rpx; font-size:36rpx; line-height:1.28; font-weight:800; color:#fff8fb; }
.diagnose-sub { display:block; margin-top:12rpx; font-size:24rpx; line-height:1.6; color:rgba(255,248,251,0.84); }
.section { margin-top:24rpx; }
.section-title { display:block; margin-bottom:14rpx; font-size:28rpx; font-weight:800; color:#3d1f27; }
.grid { display:grid; grid-template-columns:repeat(2,1fr); gap:14rpx; }
.feature-card { min-height:182rpx; padding:24rpx; border-radius:24rpx; display:flex; flex-direction:column; justify-content:space-between; }
.feature-title { font-size:30rpx; font-weight:800; color:#3d1f27; }
.feature-sub { font-size:22rpx; line-height:1.55; color:#7e6a6e; }
.route-list { display:flex; flex-direction:column; gap:14rpx; }
.route-card { padding:24rpx; border-radius:24rpx; }
.route-title { display:block; font-size:30rpx; font-weight:800; color:#3d1f27; }
.route-sub { display:block; margin-top:10rpx; font-size:22rpx; line-height:1.6; color:#7e6a6e; }
</style>

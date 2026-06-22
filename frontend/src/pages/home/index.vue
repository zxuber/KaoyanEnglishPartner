<template>
  <view class="home">
    <view class="hero">
      <view class="hero-copy">
        <text class="eyebrow">Today</text>
        <text class="h1">今日训练</text>
        <text class="sub">{{ dashboard.greeting || '先把今天最重要的 1 到 3 件事做掉。' }}</text>
      </view>
      <view class="hero-pill" @click="doCheckin">
        <text>{{ checkedIn ? '已打卡' : '今日打卡' }}</text>
      </view>
    </view>

    <view v-if="loading" class="loading-card">
      <text>正在整理你的今日训练...</text>
    </view>

    <template v-else>
      <view class="continue-card" @click="goPage(dashboard.continueTraining?.page || '/pages/word/index')">
        <view class="continue-top">
          <text class="continue-tag">继续上次训练</text>
          <text class="continue-module">{{ dashboard.continueTraining?.module }}</text>
        </view>
        <text class="continue-title">{{ dashboard.continueTraining?.title }}</text>
        <text v-if="dashboard.continueTraining?.subtitle" class="continue-sub">{{ dashboard.continueTraining?.subtitle }}</text>
        <view class="continue-progress">
          <view class="continue-bar">
            <view
              class="continue-fill"
              :style="{ width: continuePercent + '%' }"
            ></view>
          </view>
          <text class="continue-metric">{{ dashboard.continueTraining?.progressCurrent || 0 }} / {{ dashboard.continueTraining?.progressTotal || 0 }}</text>
        </view>
      </view>

      <view class="stats">
        <view class="stat-card">
          <text class="stat-num">{{ dashboard.stats?.masteredWords || 0 }}</text>
          <text class="stat-label">已掌握单词</text>
        </view>
        <view class="stat-card">
          <text class="stat-num">{{ dashboard.stats?.totalCheckins || 0 }}</text>
          <text class="stat-label">累计打卡</text>
        </view>
        <view class="stat-card">
          <text class="stat-num">{{ dashboard.stats?.targetScore || '--' }}</text>
          <text class="stat-label">目标分数</text>
        </view>
      </view>

      <view class="section">
        <view class="section-head">
          <text class="sec-title">今日训练</text>
          <text class="sec-sub">系统按你的学习画像和最近状态推荐</text>
        </view>
        <view
          v-for="task in dashboard.todayTasks"
          :key="task.title"
          class="task-card"
          @click="goPage(task.page)"
        >
          <view class="task-line">
            <text class="task-badge" :style="{ background: task.accent || '#0f766e' }">{{ task.badge }}</text>
            <text class="task-title">{{ task.title }}</text>
          </view>
          <text class="task-sub">{{ task.subtitle }}</text>
          <text class="task-reason">{{ task.reason }}</text>
        </view>
      </view>

      <view class="section">
        <view class="section-head">
          <text class="sec-title">专项入口</text>
          <text class="sec-sub">你也可以自由切换训练模块</text>
        </view>
        <view class="quick-grid">
          <view
            v-for="entry in dashboard.quickEntries"
            :key="entry.title"
            class="quick-card"
            @click="goPage(entry.page)"
          >
            <text class="quick-title">{{ entry.title }}</text>
            <text class="quick-sub">{{ entry.subtitle }}</text>
          </view>
        </view>
      </view>

      <view class="section">
        <view class="section-head">
          <text class="sec-title">最近复盘</text>
          <text class="sec-sub">别只看正确率，先看你最容易掉队的地方</text>
        </view>
        <view class="review-card">
          <view v-for="item in dashboard.reviewItems" :key="item.label" class="review-line">
            <view>
              <text class="review-label">{{ item.label }}</text>
              <text class="review-hint">{{ item.hint }}</text>
            </view>
            <text class="review-value">{{ item.value }}</text>
          </view>
        </view>
      </view>

      <view class="footer-action" @click="goOnboarding">
        <text>重新查看学习画像与专属方案</text>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue';
import { ensureAuthed } from '@/utils/auth';
import { getUserId } from '@/utils/session';
import { get, post } from '@/utils/request';

interface DashboardData {
  greeting?: string;
  continueTraining?: {
    module?: string;
    title?: string;
    subtitle?: string;
    page?: string;
    progressCurrent?: number;
    progressTotal?: number;
  };
  todayTasks?: Array<{ title: string; subtitle: string; reason: string; page: string; badge: string; accent?: string }>;
  quickEntries?: Array<{ title: string; subtitle: string; page: string }>;
  reviewItems?: Array<{ label: string; value: string; hint: string }>;
  stats?: { masteredWords?: number; totalCheckins?: number; targetScore?: number };
}

const dashboard = ref<DashboardData>({});
const loading = ref(true);
const checkedIn = ref(false);
const days = ref(0);

const continuePercent = computed(() => {
  const current = dashboard.value.continueTraining?.progressCurrent || 0;
  const total = dashboard.value.continueTraining?.progressTotal || 0;
  return total ? Math.min(100, Math.round((current / total) * 100)) : 0;
});

onMounted(async () => {
  try {
    await ensureAuthed();
  } catch (e) {
    uni.showToast({ title: '微信登录失败，请重试', icon: 'none' });
    loading.value = false;
    return;
  }

  const uid = getUserId();
  const today = new Date().toDateString();
  const last = uni.getStorageSync('last_checkin_date');
  if (last === today) checkedIn.value = true;
  const cd = uni.getStorageSync('continuous_days');
  days.value = cd ? parseInt(String(cd)) : 0;

  if (uid) {
    try {
      const res = await get<DashboardData>(`/users/${uid}/dashboard`);
      if (res && res.data) {
        dashboard.value = res.data;
        days.value = res.data.stats?.totalCheckins || days.value;
      }
    } catch (e) {}
  }
  loading.value = false;
});

function goPage(url: string) { uni.navigateTo({ url }); }
function goOnboarding() { uni.navigateTo({ url: '/pages/onboarding/index' }); }
async function doCheckin() {
  if (checkedIn.value) return;
  const uid = getUserId();
  if (uid) {
    try {
      await post(`/users/${uid}/checkin`, {});
    } catch (e) {}
  }
  checkedIn.value = true;
  const today = new Date().toDateString();
  uni.setStorageSync('last_checkin_date', today);
  days.value++;
  uni.setStorageSync('continuous_days', String(days.value));
  uni.showToast({ title: '打卡成功', icon: 'success' });
}
</script>

<style scoped>
.home { min-height: 100vh; padding: 36rpx 28rpx 60rpx; background:
  radial-gradient(circle at top left, rgba(9, 71, 67, 0.14), transparent 34%),
  linear-gradient(180deg, #f4f1e9 0%, #f7f4ef 28%, #f5f3ee 100%); }
.hero { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 28rpx; padding-top: 24rpx; }
.hero-copy { max-width: 520rpx; }
.eyebrow { display: block; margin-bottom: 10rpx; font-size: 22rpx; letter-spacing: 4rpx; color: #7c7468; text-transform: uppercase; }
.h1 { display: block; font-size: 56rpx; line-height: 1.06; font-weight: 800; color: #18302b; }
.sub { display: block; margin-top: 14rpx; font-size: 26rpx; line-height: 1.6; color: #5d5a54; }
.hero-pill { padding: 18rpx 24rpx; border-radius: 999rpx; background: rgba(255,255,255,0.7); backdrop-filter: blur(16rpx); box-shadow: 0 12rpx 30rpx rgba(24,48,43,0.08); color: #18302b; font-size: 24rpx; font-weight: 600; }
.loading-card,
.continue-card,
.task-card,
.review-card,
.quick-card,
.stat-card { background: rgba(255,255,255,0.8); backdrop-filter: blur(18rpx); box-shadow: 0 20rpx 40rpx rgba(31, 41, 35, 0.07); border: 1rpx solid rgba(255,255,255,0.55); }
.loading-card { padding: 40rpx; border-radius: 28rpx; color: #5d5a54; text-align: center; }
.continue-card { padding: 30rpx; border-radius: 32rpx; margin-bottom: 24rpx; background: linear-gradient(135deg, #163c37 0%, #245a52 100%); color: #f7f5ef; }
.continue-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18rpx; }
.continue-tag { font-size: 22rpx; padding: 10rpx 16rpx; border-radius: 999rpx; background: rgba(255,255,255,0.16); }
.continue-module { font-size: 22rpx; opacity: 0.8; }
.continue-title { display: block; font-size: 38rpx; font-weight: 700; line-height: 1.2; }
.continue-sub { display: block; margin-top: 12rpx; font-size: 24rpx; line-height: 1.5; color: rgba(247,245,239,0.8); }
.continue-progress { display: flex; align-items: center; gap: 16rpx; margin-top: 24rpx; }
.continue-bar { flex: 1; height: 10rpx; background: rgba(255,255,255,0.18); border-radius: 999rpx; overflow: hidden; }
.continue-fill { height: 100%; background: #f6c567; border-radius: 999rpx; }
.continue-metric { font-size: 22rpx; color: rgba(247,245,239,0.85); }
.stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14rpx; margin-bottom: 28rpx; }
.stat-card { padding: 24rpx 18rpx; border-radius: 24rpx; text-align: center; }
.stat-num { display: block; font-size: 42rpx; font-weight: 800; color: #18302b; }
.stat-label { display: block; margin-top: 8rpx; font-size: 22rpx; color: #786f63; }
.section { margin-bottom: 30rpx; }
.section-head { display: flex; flex-direction: column; gap: 8rpx; margin-bottom: 16rpx; }
.sec-title { font-size: 30rpx; font-weight: 700; color: #1f2c28; }
.sec-sub { font-size: 22rpx; color: #847a6d; }
.task-card { padding: 28rpx; border-radius: 28rpx; margin-bottom: 14rpx; }
.task-line { display: flex; align-items: center; gap: 14rpx; margin-bottom: 14rpx; }
.task-badge { color: #fff; border-radius: 999rpx; padding: 8rpx 14rpx; font-size: 20rpx; }
.task-title { flex: 1; font-size: 30rpx; font-weight: 700; color: #1f2c28; }
.task-sub { display: block; font-size: 24rpx; color: #574f45; line-height: 1.55; }
.task-reason { display: block; margin-top: 12rpx; font-size: 22rpx; color: #8c816f; line-height: 1.5; }
.quick-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14rpx; }
.quick-card { min-height: 154rpx; padding: 24rpx; border-radius: 24rpx; display: flex; flex-direction: column; justify-content: space-between; }
.quick-title { font-size: 30rpx; font-weight: 700; color: #1f2c28; }
.quick-sub { font-size: 22rpx; color: #7a7166; line-height: 1.45; }
.review-card { padding: 10rpx 24rpx; border-radius: 28rpx; }
.review-line { display: flex; justify-content: space-between; gap: 18rpx; padding: 22rpx 0; border-bottom: 1rpx solid rgba(31,44,40,0.08); }
.review-line:last-child { border-bottom: 0; }
.review-label { display: block; font-size: 26rpx; font-weight: 600; color: #1f2c28; }
.review-hint { display: block; margin-top: 6rpx; font-size: 22rpx; line-height: 1.45; color: #887f74; max-width: 430rpx; }
.review-value { flex-shrink: 0; font-size: 24rpx; font-weight: 700; color: #18302b; text-align: right; }
.footer-action { margin-top: 10rpx; text-align: center; color: #6f675b; font-size: 24rpx; padding: 26rpx 0; }
</style>

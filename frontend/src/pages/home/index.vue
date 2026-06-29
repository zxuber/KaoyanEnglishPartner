<template>
  <view class="home">
    <view class="hero">
      <view class="hero-copy">
        <text class="eyebrow">TODAY'S RUN</text>
        <text class="h1">训练中枢</text>
        <text class="sub">{{ dashboard.greeting || '先把今天最关键的训练做完，再决定要不要扩项。' }}</text>
      </view>
      <view class="hero-pill" @click="doCheckin">
        <text>{{ checkedIn ? '已打卡' : '今日打卡' }}</text>
      </view>
    </view>

    <view v-if="loading" class="loading-card">
      <text>正在整理你的训练入口...</text>
    </view>

    <template v-else>
      <view class="word-hero-card" @click="goPage('/pages/word/index')">
        <view class="word-hero-top">
          <text class="word-hero-tag">今日单词训练</text>
          <text class="word-hero-meta">20 题</text>
        </view>
        <text class="word-hero-title">开口背单词</text>
        <text class="word-hero-sub">看英文，说中文释义。答错自动回炉，误解本单词会穿插回来继续巩固。</text>
        <view class="word-hero-action">
          <text>开始今日训练</text>
          <text class="word-hero-arrow">→</text>
        </view>
      </view>

      <view class="recent-card" @click="goPage(dashboard.continueTraining?.page || '/pages/word/index')">
        <view class="recent-main">
          <view>
            <text class="recent-tag">继续上次训练</text>
            <text class="recent-title">{{ dashboard.continueTraining?.title || '暂无上次训练，先从单词开始' }}</text>
            <text v-if="dashboard.continueTraining?.subtitle" class="recent-sub">{{ dashboard.continueTraining?.subtitle }}</text>
          </view>
          <text class="recent-module">{{ dashboard.continueTraining?.module || '推荐起点' }}</text>
        </view>
        <view class="recent-progress">
          <view class="recent-bar">
            <view class="recent-fill" :style="{ width: continuePercent + '%' }"></view>
          </view>
          <text class="recent-metric">{{ dashboard.continueTraining?.progressCurrent || 0 }} / {{ dashboard.continueTraining?.progressTotal || 0 }}</text>
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
          <text class="sec-title">专项训练</text>
          <text class="sec-sub">专项入口会吸收今日推荐理由，优先项会直接标出来</text>
        </view>
        <view class="special-grid">
          <view v-for="entry in enhancedSpecialEntries" :key="entry.title" class="special-card" :class="[entry.skin, { wide: entry.wide, priority: entry.priority }]" @click="goPage(entry.page)">
            <view class="special-top">
              <text class="special-title">{{ entry.title }}</text>
              <text v-if="entry.priority" class="special-badge">今日优先</text>
            </view>
            <text class="special-sub">{{ entry.subtitle }}</text>
            <text v-if="entry.reason" class="special-reason">{{ entry.reason }}</text>
          </view>
        </view>
      </view>

      <view class="section">
        <view class="section-head">
          <text class="sec-title">工具与检测</text>
          <text class="sec-sub">复盘资产和阶段检测放在主训练流之外，但随时可进</text>
        </view>
        <view class="tool-list">
          <view class="tool-card" @click="switchTabPage('/pages/mistake/index')">
            <text class="tool-title">误解本</text>
            <text class="tool-sub">单词、短句、写作表达、固定搭配、易混词</text>
          </view>
          <view class="tool-card accent" @click="switchTabPage('/pages/tutor/index')">
            <text class="tool-title">高级智能英语教练系统</text>
            <text class="tool-sub">今日诊断、提分方案、主题词库、专项纠偏</text>
          </view>
          <view class="tool-card" @click="goPage('/pages/exam/index')">
            <text class="tool-title">模考</text>
            <text class="tool-sub">阶段性套卷检查，后续可承接正式模考链路</text>
          </view>
        </view>
      </view>

      <view class="section">
        <view class="section-head">
          <text class="sec-title">最近复盘</text>
          <text class="sec-sub">不只看做了多少，也看最近最容易掉队在哪</text>
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
import { onShow } from '@dcloudio/uni-app';
import { getApiRuntimeSummary } from '@/config/api';
import { ensureAuthed } from '@/utils/auth';
import { clearSession, getUserId } from '@/utils/session';
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
  reviewItems?: Array<{ label: string; value: string; hint: string }>;
  stats?: { masteredWords?: number; totalCheckins?: number; targetScore?: number };
}

const dashboard = ref<DashboardData>({});
const loading = ref(true);
const checkedIn = ref(false);
const days = ref(0);

const specialEntries = [
  { title: '阅读', subtitle: '阅读理解 · 定位与排错', page: '/pages/reading/index', skin: 'reading' },
  { title: '写作', subtitle: '小作文 / 大作文', page: '/pages/special/index', skin: 'writing' },
  { title: '翻译', subtitle: '断句、主干、整句翻译', page: '/pages/translate/index', skin: 'translate' },
  { title: '完形填空', subtitle: '逻辑、搭配、上下文判断', page: '/pages/cloze/index', skin: 'cloze' },
  { title: '新题型', subtitle: '排序题、7选五、小标题等', page: '/pages/new-question/index', skin: 'new-question', wide: true },
];

const tabPages = new Set(['/pages/home/index', '/pages/mistake/index', '/pages/tutor/index']);

const continuePercent = computed(() => {
  const current = dashboard.value.continueTraining?.progressCurrent || 0;
  const total = dashboard.value.continueTraining?.progressTotal || 0;
  return total ? Math.min(100, Math.round((current / total) * 100)) : 0;
});

const enhancedSpecialEntries = computed(() => {
  const tasks = dashboard.value.todayTasks || [];
  return specialEntries.map((entry) => {
    const task = tasks.find((item) => item.page === entry.page);
    return {
      ...entry,
      subtitle: task?.subtitle || entry.subtitle,
      reason: task?.reason || "",
      priority: !!task,
    };
  });
});

async function fetchDashboard(userId: number) {
  const res = await get<DashboardData>(`/users/${userId}/dashboard`);
  if (res && res.data) {
    dashboard.value = res.data;
    days.value = res.data.stats?.totalCheckins || 0;
    checkedIn.value = !!uni.getStorageSync('last_checkin_date') && uni.getStorageSync('last_checkin_date') === new Date().toDateString();
  }
}

function isMissingUserError(error: unknown) {
  return error instanceof Error && error.message.includes('用户不存在');
}

async function loadDashboard() {
  let uid = getUserId();
  if (!uid) return;

  try {
    await fetchDashboard(uid);
  } catch (error) {
    if (!isMissingUserError(error)) throw error;

    clearSession();
    const login = await ensureAuthed(true);
    uid = login.userId;
    await fetchDashboard(uid);
  }
}

onMounted(async () => {
  console.log("[HOME] onMounted", {
    runtime: getApiRuntimeSummary(),
    cachedUserId: getUserId(),
  });
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
      await loadDashboard();
    } catch (e) {}
  }
  loading.value = false;
});

onShow(async () => {
  const uid = getUserId();
  if (!uid) return;
  try {
    await loadDashboard();
  } catch (e) {}
});

function goPage(url: string) {
  if (tabPages.has(url)) {
    uni.switchTab({ url });
    return;
  }
  uni.navigateTo({ url });
}

function switchTabPage(url: string) {
  uni.switchTab({ url });
}

function goOnboarding() { uni.navigateTo({ url: '/pages/onboarding/index?review=1' }); }
async function doCheckin() {
  if (checkedIn.value) return;
  const uid = getUserId();
  if (uid) {
    try {
      await post(`/users/${uid}/checkin`, {});
      await loadDashboard();
    } catch (e) {}
  }
  checkedIn.value = true;
  const today = new Date().toDateString();
  uni.setStorageSync('last_checkin_date', today);
  days.value = dashboard.value.stats?.totalCheckins || (days.value + 1);
  uni.setStorageSync('continuous_days', String(days.value));
  uni.showToast({ title: '打卡成功', icon: 'success' });
}
</script>

<style scoped>
.home { min-height: 100vh; padding: 36rpx 28rpx 80rpx; background:
  radial-gradient(circle at top left, rgba(9, 71, 67, 0.14), transparent 34%),
  linear-gradient(180deg, #f4f1e9 0%, #f7f4ef 28%, #f5f3ee 100%); }
.hero { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 28rpx; padding-top: 24rpx; }
.hero-copy { max-width: 520rpx; }
.eyebrow { display: block; margin-bottom: 10rpx; font-size: 22rpx; letter-spacing: 4rpx; color: #7c7468; text-transform: uppercase; }
.h1 { display: block; font-size: 56rpx; line-height: 1.06; font-weight: 800; color: #18302b; }
.sub { display: block; margin-top: 14rpx; font-size: 26rpx; line-height: 1.6; color: #5d5a54; }
.hero-pill { padding: 18rpx 24rpx; border-radius: 999rpx; background: rgba(255,255,255,0.7); backdrop-filter: blur(16rpx); box-shadow: 0 12rpx 30rpx rgba(24,48,43,0.08); color: #18302b; font-size: 24rpx; font-weight: 600; }
.loading-card,.word-hero-card,.recent-card,.review-card,.special-card,.stat-card,.tool-card { background: rgba(255,255,255,0.8); backdrop-filter: blur(18rpx); box-shadow: 0 20rpx 40rpx rgba(31, 41, 35, 0.07); border: 1rpx solid rgba(255,255,255,0.55); }
.loading-card { padding: 40rpx; border-radius: 28rpx; color: #5d5a54; text-align: center; }
.word-hero-card { padding: 34rpx; border-radius: 34rpx; margin-bottom: 18rpx; background:
  radial-gradient(circle at top right, rgba(236, 201, 75, 0.28), transparent 34%),
  linear-gradient(135deg, #0f766e 0%, #134e4a 100%); color: #f5fffb; }
.word-hero-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 22rpx; }
.word-hero-tag { font-size: 24rpx; padding: 10rpx 18rpx; border-radius: 999rpx; background: rgba(255,255,255,0.16); font-weight: 700; }
.word-hero-meta { font-size: 22rpx; color: rgba(245,255,251,0.78); }
.word-hero-title { display: block; font-size: 52rpx; line-height: 1.08; font-weight: 900; letter-spacing: 1rpx; }
.word-hero-sub { display: block; margin-top: 16rpx; font-size: 25rpx; line-height: 1.65; color: rgba(245,255,251,0.82); }
.word-hero-action { display: inline-flex; align-items: center; gap: 12rpx; margin-top: 28rpx; padding: 16rpx 22rpx; border-radius: 999rpx; background: #f6c567; color: #18302b; font-size: 25rpx; font-weight: 800; }
.word-hero-arrow { font-size: 28rpx; }
.recent-card { padding: 24rpx; border-radius: 28rpx; margin-bottom: 24rpx; background: rgba(255,255,255,0.78); }
.recent-main { display: flex; justify-content: space-between; gap: 18rpx; align-items: flex-start; }
.recent-tag { display: block; margin-bottom: 10rpx; font-size: 21rpx; color: #8a5a43; font-weight: 800; }
.recent-module { flex-shrink: 0; padding: 8rpx 14rpx; border-radius: 999rpx; background: #f4e8dc; color: #8a5a43; font-size: 20rpx; font-weight: 700; }
.recent-title { display: block; font-size: 30rpx; font-weight: 800; line-height: 1.25; color: #2f2924; }
.recent-sub { display: block; margin-top: 10rpx; font-size: 23rpx; line-height: 1.5; color: #77695d; }
.recent-progress { display: flex; align-items: center; gap: 14rpx; margin-top: 20rpx; }
.recent-bar { flex: 1; height: 9rpx; background: #efe6dc; border-radius: 999rpx; overflow: hidden; }
.recent-fill { height: 100%; background: #8a5a43; border-radius: 999rpx; }
.recent-metric { font-size: 21rpx; color: #7d6e62; }
.stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14rpx; margin-bottom: 28rpx; }
.stat-card { padding: 24rpx 18rpx; border-radius: 24rpx; text-align: center; }
.stat-num { display: block; font-size: 42rpx; font-weight: 800; color: #18302b; }
.stat-label { display: block; margin-top: 8rpx; font-size: 22rpx; color: #786f63; }
.section { margin-bottom: 30rpx; }
.section-head { display: flex; flex-direction: column; gap: 8rpx; margin-bottom: 16rpx; }
.sec-title { font-size: 30rpx; font-weight: 700; color: #1f2c28; }
.sec-sub { font-size: 22rpx; color: #847a6d; }
.special-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14rpx; }
.special-card { min-height: 176rpx; padding: 24rpx; border-radius: 24rpx; display: flex; flex-direction: column; justify-content: space-between; }
.special-card.priority { border-color: rgba(15, 118, 110, 0.22); box-shadow: 0 22rpx 42rpx rgba(15, 118, 110, 0.1); }
.special-card.reading { background: linear-gradient(180deg, #fff7ed 0%, #fffdf9 100%); }
.special-card.writing { background: linear-gradient(180deg, #fff4f7 0%, #fffdfb 100%); }
.special-card.translate { background: linear-gradient(180deg, #f4fbff 0%, #fffefe 100%); }
.special-card.cloze { background: linear-gradient(180deg, #f6f5ff 0%, #fffefe 100%); }
.special-card.new-question { background: linear-gradient(180deg, #eefbf5 0%, #fffefe 100%); }
.special-card.wide { grid-column: 1 / -1; min-height: 154rpx; }
.special-top { display: flex; justify-content: space-between; align-items: flex-start; gap: 12rpx; }
.special-title { font-size: 30rpx; font-weight: 700; color: #1f2c28; }
.special-badge { flex-shrink: 0; padding: 7rpx 12rpx; border-radius: 999rpx; background: #0f766e; color: #f8fffc; font-size: 19rpx; font-weight: 800; }
.special-sub { margin-top: 12rpx; font-size: 22rpx; color: #574f45; line-height: 1.45; }
.special-reason { display: block; margin-top: 12rpx; font-size: 20rpx; color: #8c816f; line-height: 1.45; }
.tool-list { display:flex; flex-direction:column; gap:14rpx; }
.tool-card { padding:24rpx; border-radius:24rpx; }
.tool-card.accent { background: linear-gradient(135deg, #fff1f6 0%, #fffaf9 100%); }
.tool-title { display:block; font-size:30rpx; font-weight:700; color:#1f2c28; }
.tool-sub { display:block; margin-top:10rpx; font-size:22rpx; line-height:1.55; color:#7a7166; }
.review-card { padding: 10rpx 24rpx; border-radius: 28rpx; }
.review-line { display: flex; justify-content: space-between; gap: 18rpx; padding: 22rpx 0; border-bottom: 1rpx solid rgba(31,44,40,0.08); }
.review-line:last-child { border-bottom: 0; }
.review-label { display: block; font-size: 26rpx; font-weight: 600; color: #1f2c28; }
.review-hint { display: block; margin-top: 6rpx; font-size: 22rpx; line-height: 1.45; color: #887f74; max-width: 430rpx; }
.review-value { flex-shrink: 0; font-size: 24rpx; font-weight: 700; color: #18302b; text-align: right; }
.footer-action { margin-top: 10rpx; text-align: center; color: #6f675b; font-size: 24rpx; padding: 26rpx 0; }
</style>

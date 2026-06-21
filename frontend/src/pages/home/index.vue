<template>
  <view class="home">
    <view class="header">
      <text class="h1">考研英语陪跑</text>
    </view>

    <view class="cards">
      <view class="card">
        <text class="num">{{ mastered }}</text>
        <text class="label">已掌握单词</text>
      </view>
      <view class="card">
        <text class="num">{{ days }}</text>
        <text class="label">连续打卡</text>
      </view>
    </view>

    <view class="section">
      <text class="sec-title">功能</text>
      <view class="menu-item" @click="goWord">
        <text>📖 单词速记</text>
        <text class="arrow">→</text>
      </view>
      <view class="menu-item" @click="goOnboarding">
        <text>⚙️ 修改问卷</text>
        <text class="arrow">→</text>
      </view>
    </view>

    <view class="checkin" :class="{ done: checkedIn }" @click="doCheckin">
      <text>{{ checkedIn ? '✅ 今日已打卡' : '🔥 打卡' }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { getUserId } from '@/utils/session';
import { get } from '@/utils/request';

const mastered = ref(0);
const days = ref(0);
const checkedIn = ref(false);

onMounted(async () => {
  const uid = getUserId();
  const today = new Date().toDateString();
  const last = uni.getStorageSync('last_checkin_date');
  if (last === today) checkedIn.value = true;
  const cd = uni.getStorageSync('continuous_days');
  days.value = cd ? parseInt(String(cd)) : 0;

  if (uid) {
    try {
      const res = await get<any>('/words/stats', { userId: uid });
      if (res && res.data) {
        mastered.value = res.data.mastered || 0;
      }
    } catch (e) {}
  }
});

function goWord() { uni.navigateTo({ url: '/pages/word/index' }); }
function goOnboarding() { uni.navigateTo({ url: '/pages/onboarding/index' }); }
function doCheckin() {
  if (checkedIn.value) return;
  checkedIn.value = true;
  const today = new Date().toDateString();
  uni.setStorageSync('last_checkin_date', today);
  days.value++;
  uni.setStorageSync('continuous_days', String(days.value));
  uni.showToast({ title: '打卡成功', icon: 'success' });
}
</script>

<style scoped>
.home { padding: 40rpx; min-height: 100vh; background: #f5f5f5; }
.header { margin-bottom: 40rpx; padding-top: 20rpx; }
.h1 { font-size: 44rpx; font-weight: 800; }
.cards { display: flex; gap: 16rpx; margin-bottom: 40rpx; }
.card { flex: 1; background: #fff; border-radius: 16rpx; padding: 32rpx 20rpx; text-align: center; }
.num { font-size: 48rpx; font-weight: 800; color: #4CAF50; display: block; margin-bottom: 8rpx; }
.label { font-size: 22rpx; color: #999; }
.section { margin-bottom: 40rpx; }
.sec-title { font-size: 30rpx; font-weight: 700; display: block; margin-bottom: 20rpx; }
.menu-item { background: #fff; border-radius: 16rpx; padding: 28rpx 24rpx; display: flex; justify-content: space-between; margin-bottom: 12rpx; font-size: 28rpx; }
.arrow { color: #4CAF50; font-weight: 700; }
.checkin { background: #4CAF50; color: #fff; text-align: center; padding: 24rpx; border-radius: 16rpx; font-size: 30rpx; font-weight: 600; margin-top: 40rpx; }
.checkin.done { background: #e0e0e0; color: #999; }
</style>

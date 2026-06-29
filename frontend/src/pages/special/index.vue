<template>
  <view class="special-page">
    <view class="hero">
      <text class="eyebrow">WRITING TRACK</text>
      <text class="title">写作专项</text>
      <text class="sub">这里只做写作分流，把大作文和小作文拆开，避免一级页面太重。</text>
    </view>

    <view class="section">
      <view class="detail-list">
        <view class="detail-card" @click="goPage('/pages/writing/index?type=small')">
          <text class="detail-title">小作文</text>
          <text class="detail-sub">应用文：书信、通知、邀请、致歉等</text>
          <text class="detail-tip">更适合先做模板、语气和格式训练</text>
        </view>
        <view class="detail-card" @click="goPage('/pages/writing/index?type=large')">
          <text class="detail-title">大作文</text>
          <text class="detail-sub">{{ examType === 'english-2' ? '英语二：图表作文' : '英语一：图画作文' }}</text>
          <text class="detail-tip">更适合练观点展开、例证和收束能力</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { ensureAuthed } from "@/utils/auth";
import { getUserId } from "@/utils/session";
import { get } from "@/utils/request";

const examType = ref("english-1");

async function loadUserProfile() {
  const userId = getUserId();
  if (!userId) return;
  try {
    const res = await get<any>(`/users/${userId}/plan`);
    const profile = res.data?.profile || "";
    if (String(profile).includes("英语二")) {
      examType.value = "english-2";
    }
  } catch (e) {}
}

function goPage(url: string) {
  uni.navigateTo({ url });
}

onLoad(async () => {
  try {
    await ensureAuthed();
  } catch (e) {
    uni.showToast({ title: "微信登录失败，请重试", icon: "none" });
    return;
  }
  await loadUserProfile();
});
</script>

<style scoped>
.special-page { min-height: 100vh; padding: 34rpx 26rpx 70rpx; background:
  radial-gradient(circle at top left, rgba(180, 83, 9, 0.12), transparent 32%),
  linear-gradient(180deg, #fbf5ef 0%, #f8f5ef 100%); }
.hero,.detail-card { background: rgba(255,255,255,0.9); border: 1rpx solid rgba(255,255,255,0.66); box-shadow: 0 18rpx 36rpx rgba(76,35,43,0.08); }
.hero { padding: 28rpx; border-radius: 28rpx; }
.eyebrow { display:block; font-size:22rpx; letter-spacing:4rpx; color:#9a5a12; }
.title { display:block; margin-top:12rpx; font-size:44rpx; font-weight:800; color:#35261b; }
.sub { display:block; margin-top:12rpx; font-size:24rpx; line-height:1.68; color:#726154; }
.section { margin-top: 24rpx; }
.detail-list { display:flex; flex-direction:column; gap:14rpx; }
.detail-card { padding: 28rpx; border-radius: 24rpx; background: linear-gradient(180deg, #fffaf3 0%, #fffefe 100%); }
.detail-title { display:block; font-size:30rpx; font-weight:800; color:#35261b; }
.detail-sub { display:block; margin-top:10rpx; font-size:23rpx; line-height:1.6; color:#7a685c; }
.detail-tip { display:block; margin-top:16rpx; font-size:22rpx; line-height:1.55; color:#9a5a12; }
</style>

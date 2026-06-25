<template>
  <view class="special-page">
    <view class="hero">
      <text class="eyebrow">NEW QUESTION TRACK</text>
      <text class="title">新题型专项</text>
      <text class="sub">这里也和写作一样做成二级分流。先按真实考研新题型拆开，后面逐个接训练链路。</text>
    </view>

    <view class="section">
      <view class="summary-card">
        <text class="summary-title">{{ examType === 'english-2' ? '你当前更偏英语二结构' : '你当前更偏英语一结构' }}</text>
        <text class="summary-sub">{{ examType === 'english-2' ? '英语二常见是多项对应和小标题。' : '英语一常见是排序题、7选五和小标题。' }}</text>
      </view>
    </view>

    <view class="section">
      <text class="section-title">英语一常见题型</text>
      <view class="detail-list">
        <view class="detail-card" @click="showToast('排序题训练内容后续接入')">
          <text class="detail-title">排序题</text>
          <text class="detail-sub">先判断段落承接，再口头说明为什么放在这里。</text>
          <text class="detail-tip">重点能力：逻辑顺序、指代衔接、结构还原</text>
        </view>
        <view class="detail-card" @click="showToast('7选五训练内容后续接入')">
          <text class="detail-title">7选五</text>
          <text class="detail-sub">先看前后句关系，再说候选句为什么匹配。</text>
          <text class="detail-tip">重点能力：句间关系、主题一致、代词线索</text>
        </view>
        <view class="detail-card" @click="showToast('小标题训练内容后续接入')">
          <text class="detail-title">小标题</text>
          <text class="detail-sub">先概括段意，再从选项里判断最贴切标题。</text>
          <text class="detail-tip">重点能力：段落主旨、概括压缩、干扰项排除</text>
        </view>
      </view>
    </view>

    <view class="section">
      <text class="section-title">英语二常见题型</text>
      <view class="detail-list">
        <view class="detail-card" @click="showToast('多项对应训练内容后续接入')">
          <text class="detail-title">多项对应</text>
          <text class="detail-sub">先说题干关键词，再回原文判断对应关系。</text>
          <text class="detail-tip">重点能力：定位速度、信息匹配、细节筛选</text>
        </view>
        <view class="detail-card" @click="showToast('小标题训练内容后续接入')">
          <text class="detail-title">小标题</text>
          <text class="detail-sub">对每段先口头压一句，再选最合适的标题。</text>
          <text class="detail-tip">重点能力：段旨提炼、结构辨识、概括表达</text>
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

function showToast(title: string) {
  uni.showToast({ title, icon: "none" });
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
  radial-gradient(circle at top left, rgba(22, 163, 74, 0.10), transparent 30%),
  linear-gradient(180deg, #f2fcf6 0%, #f8f5ef 100%); }
.hero,.summary-card,.detail-card { background: rgba(255,255,255,0.9); border: 1rpx solid rgba(255,255,255,0.66); box-shadow: 0 18rpx 36rpx rgba(46, 94, 59, 0.08); }
.hero { padding: 28rpx; border-radius: 28rpx; }
.eyebrow { display:block; font-size:22rpx; letter-spacing:4rpx; color:#15803d; }
.title { display:block; margin-top:12rpx; font-size:44rpx; font-weight:800; color:#183625; }
.sub { display:block; margin-top:12rpx; font-size:24rpx; line-height:1.68; color:#627264; }
.section { margin-top: 24rpx; }
.summary-card { padding: 24rpx; border-radius: 24rpx; background: linear-gradient(180deg, #eefbf3 0%, #fffefe 100%); }
.summary-title { display:block; font-size:28rpx; font-weight:800; color:#183625; }
.summary-sub { display:block; margin-top:10rpx; font-size:22rpx; line-height:1.6; color:#617267; }
.section-title { display:block; margin-bottom:14rpx; font-size:28rpx; font-weight:800; color:#183625; }
.detail-list { display:flex; flex-direction:column; gap:14rpx; }
.detail-card { padding: 28rpx; border-radius: 24rpx; background: linear-gradient(180deg, #f8fefb 0%, #fffefe 100%); }
.detail-title { display:block; font-size:30rpx; font-weight:800; color:#183625; }
.detail-sub { display:block; margin-top:10rpx; font-size:23rpx; line-height:1.6; color:#617267; }
.detail-tip { display:block; margin-top:16rpx; font-size:22rpx; line-height:1.55; color:#15803d; }
</style>

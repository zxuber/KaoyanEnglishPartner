<template>
  <view class="shell-page">
    <view class="hero">
      <text class="eyebrow">CLOZE</text>
      <text class="title">完形填空</text>
      <text class="sub">这里预留给“先判断逻辑，再确认搭配，再解释为什么不是另外三个”的完形训练方式。</text>
    </view>

    <view class="feature-card">
      <text class="card-title">后续会接入</text>
      <text class="card-sub">上下文逻辑、固定搭配、词义辨析、干扰项排错。</text>
    </view>
  </view>
</template>

<script setup lang="ts">
</script>

<style scoped>
.shell-page { min-height: 100vh; padding: 34rpx 26rpx 70rpx; background:
  radial-gradient(circle at top left, rgba(91, 33, 182, 0.10), transparent 30%),
  linear-gradient(180deg, #f7f6ff 0%, #f8f5ef 100%); }
.hero,.feature-card { background: rgba(255,255,255,0.9); border: 1rpx solid rgba(255,255,255,0.66); box-shadow: 0 18rpx 36rpx rgba(76,35,43,0.08); }
.hero { padding: 28rpx; border-radius: 28rpx; }
.eyebrow { display:block; font-size:22rpx; letter-spacing:4rpx; color:#6d28d9; }
.title { display:block; margin-top:12rpx; font-size:44rpx; font-weight:800; color:#2d2341; }
.sub { display:block; margin-top:12rpx; font-size:24rpx; line-height:1.68; color:#6e6683; }
.feature-card { margin-top:24rpx; padding: 28rpx; border-radius: 24rpx; }
.card-title { display:block; font-size:30rpx; font-weight:800; color:#2d2341; }
.card-sub { display:block; margin-top:12rpx; font-size:23rpx; line-height:1.65; color:#6e6683; }
</style>

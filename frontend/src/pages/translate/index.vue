<template>
  <view class="shell-page">
    <view class="hero">
      <text class="eyebrow">TRANSLATION</text>
      <text class="title">翻译专项</text>
      <text class="sub">这里先把页面骨架立起来，后面会接“断句、抓主干、整句重组、口头解释思路”的训练链路。</text>
    </view>

    <view class="card-list">
      <view class="feature-card">
        <text class="card-title">训练目标</text>
        <text class="card-sub">先读原句，再说主干与从句关系，系统再追问你为什么这么断。</text>
      </view>
      <view class="feature-card">
        <text class="card-title">预留能力</text>
        <text class="card-sub">句法拆解、关键词义、译文润色、错因归档。</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
</script>

<style scoped>
.shell-page { min-height: 100vh; padding: 34rpx 26rpx 70rpx; background:
  radial-gradient(circle at top left, rgba(14, 116, 144, 0.11), transparent 30%),
  linear-gradient(180deg, #f3fbff 0%, #f8f5ef 100%); }
.hero,.feature-card { background: rgba(255,255,255,0.9); border: 1rpx solid rgba(255,255,255,0.66); box-shadow: 0 18rpx 36rpx rgba(46, 84, 103, 0.08); }
.hero { padding: 28rpx; border-radius: 28rpx; }
.eyebrow { display:block; font-size:22rpx; letter-spacing:4rpx; color:#0e7490; }
.title { display:block; margin-top:12rpx; font-size:44rpx; font-weight:800; color:#173543; }
.sub { display:block; margin-top:12rpx; font-size:24rpx; line-height:1.68; color:#60727b; }
.card-list { display:flex; flex-direction:column; gap:14rpx; margin-top:24rpx; }
.feature-card { padding: 28rpx; border-radius: 24rpx; }
.card-title { display:block; font-size:30rpx; font-weight:800; color:#173543; }
.card-sub { display:block; margin-top:12rpx; font-size:23rpx; line-height:1.65; color:#60727b; }
</style>

<template>
  <view class="mistake-page">
    <view class="hero">
      <text class="eyebrow">Misunderstanding Book</text>
      <text class="title">误解本</text>
      <text class="sub">把你在阅读里卡住的词和句子单独拎出来，反复翻面复盘。</text>
    </view>

    <view class="tabs">
      <view class="tab" :class="{ active: activeTab === 'word' }" @click="switchTab('word')">
        <text>单词</text>
      </view>
      <view class="tab" :class="{ active: activeTab === 'sentence' }" @click="switchTab('sentence')">
        <text>短句</text>
      </view>
    </view>

    <view v-if="loading" class="state-card">
      <text>正在整理你的误解本...</text>
    </view>

    <template v-else>
      <view v-if="!items.length" class="empty-card">
        <text class="empty-title">{{ activeTab === 'word' ? '还没有加入单词' : '还没有加入短句' }}</text>
        <text class="empty-sub">回到阅读页，点词或点句，再加入误解本。</text>
      </view>

      <view v-else class="card-list">
        <view
          v-for="item in items"
          :key="item.id"
          class="flip-card"
          :class="{ flipped: flippedMap[item.id] }"
          @click="toggleFlip(item.id)"
        >
          <view class="flip-card-inner">
            <view class="flip-face front">
              <view class="face-top">
                <text class="face-tag">{{ activeTab === 'word' ? 'WORD' : 'SENTENCE' }}</text>
                <text class="face-module">{{ item.sourceModule }}</text>
              </view>
              <text class="front-text" :class="{ sentence: activeTab === 'sentence' }">{{ item.sourceText }}</text>
              <text class="face-tip">点击翻面</text>
            </view>

            <view class="flip-face back">
              <view class="face-top">
                <text class="face-tag alt">中文释义</text>
                <text class="face-module">{{ item.sourceModule }}</text>
              </view>
              <text class="back-text">{{ item.translation }}</text>
              <view class="back-actions">
                <view class="ghost-btn" @click.stop="toastReading">
                  <text>阅读</text>
                </view>
              </view>
            </view>
          </view>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onLoad, onShow } from "@dcloudio/uni-app";
import { ensureAuthed } from "@/utils/auth";
import { getUserId } from "@/utils/session";
import { get } from "@/utils/request";

type TabType = "word" | "sentence";

interface MistakeItem {
  id: number;
  type: string;
  sourceText: string;
  translation: string;
  sourceModule: string;
  articleId: string;
  status: string;
  createdAt: string;
}

const loading = ref(true);
const activeTab = ref<TabType>("word");
const items = ref<MistakeItem[]>([]);
const flippedMap = ref<Record<number, boolean>>({});
const userId = ref<number | null>(null);

async function loadItems() {
  if (!userId.value) return;
  loading.value = true;
  try {
    const res = await get<MistakeItem[]>("/mistakes", {
      userId: userId.value,
      type: activeTab.value,
    });
    items.value = res.data || [];
    flippedMap.value = {};
  } catch (e) {
    uni.showToast({ title: "误解本加载失败", icon: "none" });
  } finally {
    loading.value = false;
  }
}

function switchTab(tab: TabType) {
  if (activeTab.value === tab) return;
  activeTab.value = tab;
  loadItems();
}

function toggleFlip(id: number) {
  flippedMap.value = {
    ...flippedMap.value,
    [id]: !flippedMap.value[id],
  };
}

function toastReading() {
  uni.showToast({ title: "阅读回放功能稍后开放", icon: "none" });
}

onLoad(async () => {
  try {
    await ensureAuthed();
  } catch (e) {
    uni.showToast({ title: "微信登录失败，请重试", icon: "none" });
    loading.value = false;
    return;
  }
  userId.value = getUserId();
  await loadItems();
});

onShow(async () => {
  if (!userId.value) return;
  await loadItems();
});
</script>

<style lang="scss" scoped>
.mistake-page {
  min-height: 100vh;
  padding: 34rpx 26rpx 60rpx;
  background:
    radial-gradient(circle at top left, rgba(190, 24, 93, 0.12), transparent 30%),
    linear-gradient(180deg, #faf4f6 0%, #f8f4ef 100%);
}

.hero,
.tabs,
.state-card,
.empty-card {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 28rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.65);
  box-shadow: 0 18rpx 36rpx rgba(76, 35, 43, 0.08);
}

.hero {
  padding: 28rpx;
}

.eyebrow {
  display: block;
  font-size: 22rpx;
  letter-spacing: 3rpx;
  text-transform: uppercase;
  color: #9f1239;
}

.title {
  display: block;
  margin-top: 12rpx;
  font-size: 42rpx;
  font-weight: 800;
  color: #3d1f27;
}

.sub {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  line-height: 1.65;
  color: #6f5b5f;
}

.tabs {
  display: flex;
  gap: 12rpx;
  margin-top: 18rpx;
  padding: 12rpx;
}

.tab {
  flex: 1;
  text-align: center;
  padding: 18rpx 0;
  border-radius: 999rpx;
  background: #f7e5eb;
  color: #8a4557;
  font-size: 26rpx;
  font-weight: 700;
}

.tab.active {
  background: linear-gradient(135deg, #9f1239 0%, #d9466e 100%);
  color: #fff8fb;
}

.state-card,
.empty-card {
  margin-top: 18rpx;
  padding: 34rpx 28rpx;
}

.empty-title {
  display: block;
  font-size: 30rpx;
  font-weight: 800;
  color: #3d1f27;
}

.empty-sub {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  line-height: 1.65;
  color: #746468;
}

.card-list {
  margin-top: 18rpx;
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.flip-card {
  perspective: 1200rpx;
}

.flip-card-inner {
  position: relative;
  min-height: 228rpx;
  transform-style: preserve-3d;
  transition: transform 0.5s ease;
}

.flip-card.flipped .flip-card-inner {
  transform: rotateY(180deg);
}

.flip-face {
  position: absolute;
  inset: 0;
  backface-visibility: hidden;
  border-radius: 28rpx;
  padding: 26rpx;
  box-sizing: border-box;
  border: 1rpx solid rgba(255, 255, 255, 0.65);
  box-shadow: 0 18rpx 36rpx rgba(76, 35, 43, 0.08);
}

.front {
  background:
    radial-gradient(circle at top right, rgba(159, 18, 57, 0.12), transparent 30%),
    linear-gradient(180deg, #fffdfb 0%, #fff6f2 100%);
}

.back {
  background:
    radial-gradient(circle at bottom left, rgba(180, 83, 9, 0.12), transparent 30%),
    linear-gradient(180deg, #fff7ed 0%, #fffdf8 100%);
  transform: rotateY(180deg);
}

.face-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.face-tag {
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  background: #f8d6df;
  color: #9f1239;
  font-size: 20rpx;
  font-weight: 800;
}

.face-tag.alt {
  background: #f4dcc5;
  color: #9a5a12;
}

.face-module {
  font-size: 20rpx;
  color: #876e73;
}

.front-text,
.back-text {
  display: block;
  margin-top: 22rpx;
  font-size: 38rpx;
  line-height: 1.35;
  font-weight: 800;
  color: #2d2325;
}

.front-text.sentence,
.back-text {
  font-size: 28rpx;
  line-height: 1.75;
  font-weight: 700;
}

.face-tip {
  display: block;
  margin-top: 24rpx;
  font-size: 22rpx;
  color: #997981;
}

.back-actions {
  margin-top: 22rpx;
}

.ghost-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 160rpx;
  padding: 16rpx 26rpx;
  border-radius: 999rpx;
  background: #f3e4d2;
  color: #8a5a2b;
  font-size: 24rpx;
  font-weight: 700;
}
</style>

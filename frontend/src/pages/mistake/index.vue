<template>
  <view class="mistake-page">
    <view class="hero">
      <text class="eyebrow">MISUNDERSTANDING BOOK</text>
      <text class="title">误解本</text>
      <text class="sub">不只收词和短句，也把可复用的写作表达、固定搭配、易混词收成一份轻资产库。</text>
    </view>

    <view class="tabs">
      <view
        v-for="category in categories"
        :key="category.key"
        class="tab"
        :class="{ active: activeTab === category.key }"
        @click="switchTab(category.key)"
      >
        <text>{{ category.label }}</text>
      </view>
    </view>

    <view v-if="loading" class="state-card">
      <text>正在整理你的误解本...</text>
    </view>

    <template v-else>
      <view class="category-tip">
        <text class="category-title">{{ activeCategory.label }}</text>
        <text class="category-sub">{{ activeCategory.description }}</text>
      </view>

      <view v-if="!items.length" class="empty-card">
        <text class="empty-title">还没有内容</text>
        <text class="empty-sub">{{ activeCategory.emptyHint }}</text>
      </view>

      <view v-else class="card-list">
        <view
          v-for="item in items"
          :key="item.id"
          class="swipe-row"
          :class="{ opened: swipedId === item.id }"
          @touchstart="onTouchStart(item.id, $event)"
          @touchend="onTouchEnd(item.id, $event)"
        >
          <view class="swipe-main">
            <view
              class="flip-card"
              :class="{ long: isLongCard, flipped: flippedMap[item.id] }"
              @click="toggleFlip(item.id)"
            >
              <view class="flip-card-inner">
                <view class="flip-face front">
                  <view class="face-top">
                    <text class="face-tag">{{ activeCategory.cardTag }}</text>
                    <text class="face-module">{{ item.sourceModule }}</text>
                  </view>
                  <text class="front-text" :class="{ compact: isLongCard }">{{ item.sourceText }}</text>
                  <text v-if="item.sourceHint" class="front-hint">{{ item.sourceHint }}</text>
                  <text class="face-tip">点击翻面</text>
                </view>

                <view class="flip-face back">
                  <view class="face-top">
                    <text class="face-tag alt">中文释义</text>
                    <text class="face-module">{{ item.sourceModule }}</text>
                  </view>
                  <text class="back-text">{{ item.translation }}</text>
                  <text v-if="item.note" class="back-note">{{ item.note }}</text>

                  <view class="back-actions">
                    <template v-if="supportsReExplain">
                      <view
                        class="ghost-btn subtle"
                        :class="{ disabled: reExplainingId === item.id }"
                        @click.stop="reExplain(item)"
                      >
                        <text>{{ reExplainingId === item.id ? '解释中...' : '重新解释' }}</text>
                      </view>
                      <view class="ghost-btn" @click.stop="toastReading">
                        <text>阅读</text>
                      </view>
                    </template>
                    <template v-else>
                      <view class="ghost-btn subtle" @click.stop="toastSaved">
                        <text>加入今日复习</text>
                      </view>
                      <view class="ghost-btn" @click.stop="toastSource(item)">
                        <text>看来源</text>
                      </view>
                    </template>
                  </view>
                </view>
              </view>
            </view>
          </view>
          <view class="swipe-action" @click="markDone(item)">
            <text>done</text>
          </view>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { onLoad, onShow } from "@dcloudio/uni-app";
import { ensureAuthed } from "@/utils/auth";
import { getUserId } from "@/utils/session";
import { del, get, post } from "@/utils/request";

type CategoryType = "word" | "sentence" | "writing" | "phrase" | "confusion";

interface MistakeItem {
  id: number | string;
  type?: string;
  category?: string;
  subcategory?: string;
  sourceText: string;
  translation: string;
  sourceModule: string;
  sourceHint?: string;
  note?: string;
  articleId?: string;
  status?: string;
  createdAt?: string;
}

interface CategoryMeta {
  key: CategoryType;
  label: string;
  cardTag: string;
  description: string;
  emptyHint: string;
}

const categories: CategoryMeta[] = [
  {
    key: "word",
    label: "单词",
    cardTag: "WORD",
    description: "阅读里卡住的高频词，继续保留真实误解本数据。",
    emptyHint: "回到阅读页，短按选词后加入误解本。",
  },
  {
    key: "sentence",
    label: "短句",
    cardTag: "SENTENCE",
    description: "阅读里真正没吃透的句子，优先做整句复盘。",
    emptyHint: "回到阅读页，长按选句后加入误解本。",
  },
  {
    key: "writing",
    label: "写作表达",
    cardTag: "WRITING",
    description: "把可直接复用到大小作文里的表达拆开收纳。",
    emptyHint: "这里后续会叠加你的写作收藏与系统精选表达。",
  },
  {
    key: "phrase",
    label: "固定搭配",
    cardTag: "PHRASE",
    description: "比单词更接近考试实战的短语资产，适合集中翻卡。",
    emptyHint: "这里后续会叠加阅读、翻译、完形中的高频固定搭配。",
  },
  {
    key: "confusion",
    label: "易混词",
    cardTag: "CONFUSION",
    description: "专门留给总是容易混掉的一组词或表达。",
    emptyHint: "这里后续会叠加系统自动识别出的易混项。",
  },
];

const loading = ref(true);
const activeTab = ref<CategoryType>("word");
const items = ref<MistakeItem[]>([]);
const flippedMap = ref<Record<string | number, boolean>>({});
const userId = ref<number | null>(null);
const reExplainingId = ref<number | string | null>(null);
const swipedId = ref<number | string | null>(null);
const touchStartX = ref(0);

const activeCategory = computed(() => categories.find((item) => item.key === activeTab.value) || categories[0]);
const supportsReExplain = computed(() => activeTab.value === "word" || activeTab.value === "sentence");
const isLongCard = computed(() => activeTab.value !== "word");

async function loadItems() {
  loading.value = true;
  try {
    if (supportsReExplain.value) {
      if (!userId.value) {
        items.value = [];
        return;
      }
      const res = await get<MistakeItem[]>("/mistakes", {
        userId: userId.value,
        type: activeTab.value,
      });
      items.value = res.data || [];
    } else {
      if (!userId.value) {
        items.value = [];
        return;
      }
      const res = await get<MistakeItem[]>("/mistake-assets", {
        userId: userId.value,
        category: activeTab.value,
      });
      items.value = res.data || [];
    }
    flippedMap.value = {};
  } catch (e) {
    uni.showToast({ title: "误解本加载失败", icon: "none" });
  } finally {
    loading.value = false;
  }
}

function switchTab(tab: CategoryType) {
  if (activeTab.value === tab) return;
  activeTab.value = tab;
  swipedId.value = null;
  loadItems();
}

function toggleFlip(id: number | string) {
  if (swipedId.value === id) return;
  flippedMap.value = {
    ...flippedMap.value,
    [id]: !flippedMap.value[id],
  };
}

function toastReading() {
  uni.showToast({ title: "阅读回放功能稍后开放", icon: "none" });
}

function toastSaved() {
  uni.showToast({ title: "已加入今日复习候选", icon: "none" });
}

function toastSource(item: MistakeItem) {
  uni.showToast({ title: `来源：${item.sourceModule}`, icon: "none" });
}

async function reExplain(item: MistakeItem) {
  if (!userId.value || reExplainingId.value === item.id || !supportsReExplain.value) return;
  reExplainingId.value = item.id;
  try {
    const res = await post<{ id: number; translation: string }>(`/mistakes/${item.id}/re-explain?userId=${userId.value}`);
    const nextTranslation = res.data?.translation;
    if (nextTranslation) {
      items.value = items.value.map((current) =>
        current.id === item.id ? { ...current, translation: nextTranslation } : current
      );
      uni.showToast({ title: "已重新解释", icon: "none" });
    }
  } finally {
    reExplainingId.value = null;
  }
}

function onTouchStart(id: number | string, event: any) {
  touchStartX.value = event.changedTouches?.[0]?.clientX || 0;
  if (swipedId.value && swipedId.value !== id) {
    swipedId.value = null;
  }
}

function onTouchEnd(id: number | string, event: any) {
  const endX = event.changedTouches?.[0]?.clientX || 0;
  const deltaX = endX - touchStartX.value;
  if (deltaX < -50) {
    swipedId.value = id;
  } else if (deltaX > 30 && swipedId.value === id) {
    swipedId.value = null;
  }
}

async function markDone(item: MistakeItem) {
  if (supportsReExplain.value) {
    if (!userId.value) return;
    await del(`/mistakes/${item.id}`, { userId: userId.value });
    uni.showToast({ title: "已移出误解本", icon: "none" });
  } else {
    if (!userId.value) return;
    await del(`/mistake-assets/${item.id}`, { userId: userId.value });
    uni.showToast({ title: "已从当前列表隐藏", icon: "none" });
  }
  items.value = items.value.filter((current) => current.id !== item.id);
  delete flippedMap.value[item.id];
  swipedId.value = null;
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
.empty-card,
.category-tip {
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
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12rpx;
  margin-top: 18rpx;
  padding: 12rpx;
}

.tab {
  text-align: center;
  padding: 18rpx 0;
  border-radius: 999rpx;
  background: #f7e5eb;
  color: #8a4557;
  font-size: 24rpx;
  font-weight: 700;
}

.tab.active {
  background: linear-gradient(135deg, #9f1239 0%, #d9466e 100%);
  color: #fff8fb;
}

.state-card,
.empty-card,
.category-tip {
  margin-top: 18rpx;
  padding: 30rpx 28rpx;
}

.category-title,
.empty-title {
  display: block;
  font-size: 30rpx;
  font-weight: 800;
  color: #3d1f27;
}

.category-sub,
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

.swipe-row {
  position: relative;
  overflow: hidden;
  border-radius: 28rpx;
  background: transparent;
}

.swipe-row.opened .swipe-main {
  transform: translateX(-136rpx);
}

.swipe-main {
  position: relative;
  z-index: 2;
  transform: translateX(0);
  transition: transform 0.25s ease;
  background: transparent;
}

.swipe-action {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  width: 136rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 28rpx;
  background: linear-gradient(135deg, #16a34a 0%, #22c55e 100%);
  color: #ffffff;
  font-size: 26rpx;
  font-weight: 800;
  z-index: 1;
  overflow: hidden;
}

.flip-card {
  perspective: 1200rpx;
  overflow: hidden;
  position: relative;
  z-index: 3;
  border-radius: 28rpx;
}

.flip-card-inner {
  position: relative;
  min-height: 228rpx;
  transform-style: preserve-3d;
  transition: transform 0.5s ease;
}

.flip-card.long .flip-card-inner {
  min-height: 380rpx;
}

.flip-card.flipped .flip-card-inner {
  transform: rotateY(180deg);
}

.flip-face {
  position: absolute;
  inset: 0;
  backface-visibility: hidden;
  -webkit-backface-visibility: hidden;
  border-radius: 28rpx;
  padding: 26rpx;
  box-sizing: border-box;
  border: 1rpx solid rgba(255, 255, 255, 0.65);
  box-shadow: 0 18rpx 36rpx rgba(76, 35, 43, 0.08);
  overflow: hidden;
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

.front-text.compact,
.back-text {
  font-size: 28rpx;
  line-height: 1.75;
  font-weight: 700;
}

.front-hint,
.back-note,
.face-tip {
  display: block;
  font-size: 22rpx;
  line-height: 1.55;
}

.front-hint {
  margin-top: 16rpx;
  color: #7e676d;
}

.back-note {
  margin-top: 16rpx;
  color: #8a6b55;
}

.face-tip {
  margin-top: 24rpx;
  color: #997981;
}

.back-actions {
  margin-top: 22rpx;
  display: flex;
  gap: 12rpx;
  flex-wrap: wrap;
  position: absolute;
  left: 26rpx;
  right: 26rpx;
  bottom: 24rpx;
}

.ghost-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 180rpx;
  padding: 16rpx 26rpx;
  border-radius: 999rpx;
  background: #f3e4d2;
  color: #8a5a2b;
  font-size: 24rpx;
  font-weight: 700;
}

.ghost-btn.subtle {
  background: #f4dfe6;
  color: #9f1239;
}

.ghost-btn.disabled {
  background: #ece7e1;
  color: #9b8e81;
}
</style>

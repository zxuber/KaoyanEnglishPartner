<template>
  <view class="onboarding">
    <view class="progress-bar">
      <view class="progress-fill" :style="{ width: (step / 7 * 100) + '%' }"></view>
    </view>

    <!-- Q1 -->
    <view v-if="step === 1" class="step">
      <text class="q">Q1 / 7</text>
      <text class="title">你考英语一还是英语二？</text>
      <view class="two-btns">
        <view class="big-btn" @click="pickExam('英语一')"><text>英语一</text><text class="sub">学术学位</text></view>
        <view class="big-btn" @click="pickExam('英语二')"><text>英语二</text><text class="sub">专业学位</text></view>
      </view>
    </view>

    <!-- Q2 -->
    <view v-if="step === 2" class="step">
      <text class="q">Q2 / 7</text>
      <text class="title">目标分数是多少？</text>
      <text class="big-num">{{ targetScore }} 分</text>
      <slider :value="targetScore" @change="onScoreChange" :min="40" :max="80" :step="1" activeColor="#4CAF50" blockSize="20" />
      <view class="row-labels"><text>40</text><text>50</text><text>60</text><text>70</text><text>80</text></view>
      <view class="next-btn" @click="nextStep">下一步</view>
    </view>

    <!-- Q3 -->
    <view v-if="step === 3" class="step">
      <text class="q">Q3 / 7</text>
      <text class="title">距离考试还有多少天？</text>
      <input class="day-input" type="number" v-model="dayInput" placeholder="输入天数如180" />
      <view class="next-btn" :class="{ dim: !dayValid }" @click="confirmDay">{{ dayValid ? '下一步 (' + dayInput + ' 天)' : '请输入天数' }}</view>
    </view>

    <!-- Q4 -->
    <view v-if="step === 4" class="step">
      <text class="q">Q4 / 7</text>
      <text class="title">每天能学多久？</text>
      <view v-for="h in ['1h','1.5h','2h','2.5h','3h+']" :key="h" class="pick-row" :class="{ on: dailyHours===h }" @click="pickHours(h)">
        <text>{{ h }}</text>
      </view>
    </view>

    <!-- Q5 -->
    <view v-if="step === 5" class="step">
      <text class="q">Q5 / 7</text>
      <text class="title">周末有时间学习吗？</text>
      <view class="two-btns">
        <view class="big-btn" :class="{ on: weekend==='有' }" @click="pickWeekend('有')"><text>有</text></view>
        <view class="big-btn" :class="{ on: weekend==='没有' }" @click="pickWeekend('没有')"><text>没有</text></view>
      </view>
      <view class="skip-btn" @click="pickWeekend('不确定')">不确定</view>
    </view>

    <!-- Q6 -->
    <view v-if="step === 6" class="step">
      <text class="q">Q6 / 7</text>
      <text class="title">当前英语水平？</text>
      <view v-for="lv in levels" :key="lv" class="pick-row" :class="{ on: englishLevel===lv }" @click="pickLevel(lv)">
        <text>{{ lv }}</text>
      </view>
    </view>

    <!-- Q7 -->
    <view v-if="step === 7" class="step">
      <text class="q">Q7 / 7</text>
      <text class="title">手头有哪些资料？</text>
      <text class="sub">可多选</text>
      <view v-for="m in allMat" :key="m" class="pick-row check" :class="{ on: materials.includes(m) }" @click="toggleMat(m)">
        <view class="box" :class="{ fill: materials.includes(m) }"><text v-if="materials.includes(m)">✓</text></view>
        <text>{{ m }}</text>
      </view>
      <view class="next-btn submit" @click="doSubmit">生成我的专属方案</view>
    </view>

    <!-- Loading -->
    <view v-if="loading" class="loading-mask">
      <view class="loading-box"><text>⏳</text><text>AI 正在生成方案...</text></view>
    </view>

    <!-- Plan -->
    <view v-if="plan" class="plan-page">
      <view class="plan-card">
        <text class="plan-h1">你的专属学习方案</text>
        <view class="sec">
          <text class="sec-title">📊 分数拆解</text>
          <view v-for="s in plan.scoreBreakdown" :key="s.type" class="score-line">
            <text class="s-type">{{ s.type }}</text>
            <view class="s-bar-bg"><view class="s-bar" :style="{ width: (s.targetScore/s.fullScore*100) + '%' }"></view></view>
            <text class="s-num">{{ s.targetScore }}/{{ s.fullScore }}</text>
          </view>
        </view>
        <view class="sec">
          <text class="sec-title">📅 复习节奏</text>
          <view v-for="p in plan.phases" :key="p.name" class="phase-box">
            <text class="ph-name">{{ p.name }}</text>
            <text class="ph-range">{{ p.dayRange }}</text>
            <text class="ph-focus">{{ p.focus }}</text>
          </view>
        </view>
        <view class="sec">
          <text class="sec-title">⏰ 每日安排</text>
          <text class="daily">{{ plan.dailySchedule }}</text>
        </view>
        <view class="sec">
          <text class="sec-title">🏁 里程碑</text>
          <view v-for="m in plan.milestones" :key="m.day" class="mile-line">
            <text class="mile-day">Day {{ m.day }}</text>
            <text class="mile-desc">{{ m.description }}</text>
          </view>
        </view>
        <view class="next-btn" @click="goHome">开始学习</view>
      </view>
    </view>
  </view>
</template>

<script>
import { post } from '@/utils/request';
import { saveSession, getUserId, isOnboardingDone } from '@/utils/session';

export default {
  data() {
    return {
      step: 1,
      loading: false,
      plan: null,
      examType: '',
      targetScore: 60,
      remainingDays: 0,
      dailyHours: '',
      weekend: '',
      englishLevel: '',
      materials: [],
      dayInput: '',
      levels: ['四级未过', '四级低分飘过', '四级高分', '六级已过', '六级高分'],
      allMat: ['红宝书', '恋练有词', '刘晓燕系列', '考研真相', '张剑黄皮书', '王江涛作文', '还没有资料'],
    };
  },
  computed: {
    dayValid() {
      const d = parseInt(this.dayInput);
      return d > 0 && d <= 730;
    },
  },
  onShow() {
    if (isOnboardingDone() && getUserId()) {
      uni.reLaunch({ url: '/pages/home/index' });
    }
  },
  methods: {
    pickExam(val) { this.examType = val; this.step = 2; },
    onScoreChange(e) { this.targetScore = e.detail.value; },
    nextStep() { this.step = 3; },
    onDayInput(e) { this.dayInput = e.detail.value; },
    confirmDay() { if (this.dayValid) { this.remainingDays = parseInt(this.dayInput); this.step = 4; } },
    pickHours(h) { this.dailyHours = h; this.step = 5; },
    pickWeekend(w) { this.weekend = w; this.step = 6; },
    pickLevel(lv) { this.englishLevel = lv; this.step = 7; },
    toggleMat(m) {
      const i = this.materials.indexOf(m);
      if (i >= 0) this.materials.splice(i, 1);
      else this.materials.push(m);
    },
    async doSubmit() {
      this.loading = true;
      try {
        const res = await post('/users/onboarding', {
          examType: this.examType,
          targetScore: this.targetScore,
          remainingDays: this.remainingDays,
          dailyHours: this.dailyHours,
          weekendAvailable: this.weekend,
          englishLevel: this.englishLevel,
          materials: this.materials,
        });
        if (res.data && res.data.plan) {
          this.plan = res.data.plan;
          if (res.data.userId) saveSession(res.data.userId);
        }
      } catch (e) {
        this.plan = {
          scoreBreakdown: [
            { type: '阅读理解', fullScore: 40, targetScore: 26, difficulty: 5 },
            { type: '大作文', fullScore: 20, targetScore: 13, difficulty: 3 },
            { type: '小作文', fullScore: 10, targetScore: 6, difficulty: 2 },
            { type: '新题型', fullScore: 10, targetScore: 7, difficulty: 2 },
            { type: '翻译', fullScore: 10, targetScore: 5, difficulty: 1 },
            { type: '完形填空', fullScore: 10, targetScore: 3, difficulty: 0 },
          ],
          phases: [
            { name: '第一阶段：基础清零', dayRange: '第1-45天', focus: '词汇+语法长难句' },
            { name: '第二阶段：阅读攻坚', dayRange: '第46-120天', focus: '真题精读+错题归因' },
            { name: '第三阶段：全题型+套卷', dayRange: '第121-180天', focus: '作文模板+新题型+模考' },
          ],
          dailySchedule: '每天2小时：前30分钟单词，中间70分钟阅读精读，后20分钟复习',
          milestones: [
            { day: 36, description: '完成红宝书必考词第一轮' },
            { day: 80, description: '精读20篇，阅读正确率突破60%' },
            { day: 130, description: '第一次完整套卷模考' },
          ],
        };
      } finally {
        this.loading = false;
      }
    },
    goHome() {
      uni.reLaunch({ url: '/pages/home/index' });
    },
  },
};
</script>

<style scoped>
.onboarding { min-height: 100vh; background: #f5f5f5; }
.progress-bar { height: 6rpx; background: #e0e0e0; position: fixed; top: 0; left: 0; right: 0; z-index: 10; }
.progress-fill { height: 100%; background: #4CAF50; transition: width 0.3s; }
.step { padding: 80rpx 40rpx 40rpx; }
.q { color: #999; font-size: 26rpx; display: block; margin-bottom: 16rpx; }
.title { font-size: 44rpx; font-weight: 700; color: #1a1a1a; display: block; margin-bottom: 60rpx; line-height: 1.3; }
.sub { font-size: 24rpx; color: #999; display: block; margin-top: 8rpx; }
.two-btns { display: flex; gap: 24rpx; }
.big-btn { flex: 1; background: #fff; border: 2rpx solid #e8e8e8; border-radius: 16rpx; padding: 48rpx 24rpx; text-align: center; font-size: 30rpx; font-weight: 600; color: #1a1a1a; }
.big-btn.on { border-color: #4CAF50; background: #f0fff0; }
.big-num { text-align: center; font-size: 80rpx; font-weight: 800; color: #4CAF50; display: block; margin-bottom: 40rpx; }
.row-labels { display: flex; justify-content: space-between; font-size: 22rpx; color: #999; margin: 8rpx 0 40rpx; }
.next-btn { background: #4CAF50; color: #fff; text-align: center; padding: 28rpx; border-radius: 16rpx; font-size: 32rpx; font-weight: 600; margin-top: 40rpx; }
.next-btn.dim { background: #ccc; }
.next-btn.submit { background: #FF9800; }
.day-input { background: #fff; border-radius: 16rpx; padding: 32rpx; font-size: 36rpx; text-align: center; margin-bottom: 40rpx; }
.pick-row { padding: 28rpx 32rpx; background: #fff; border-bottom: 1rpx solid #f0f0f0; font-size: 30rpx; display: flex; align-items: center; }
.pick-row.on { background: #f0fff0; color: #4CAF50; font-weight: 600; }
.pick-row.check { gap: 20rpx; }
.box { width: 40rpx; height: 40rpx; border: 2rpx solid #ccc; border-radius: 8rpx; display: flex; align-items: center; justify-content: center; font-size: 24rpx; color: #fff; flex-shrink: 0; }
.box.fill { background: #4CAF50; border-color: #4CAF50; }
.skip-btn { text-align: center; padding: 30rpx; color: #999; font-size: 28rpx; }
.loading-mask { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 100; }
.loading-box { background: #fff; border-radius: 24rpx; padding: 60rpx 40rpx; text-align: center; font-size: 32rpx; }
.plan-page { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: #f5f5f5; z-index: 100; overflow-y: auto; }
.plan-card { padding: 60rpx 40rpx; }
.plan-h1 { font-size: 40rpx; font-weight: 800; text-align: center; display: block; margin-bottom: 40rpx; }
.sec { margin-bottom: 40rpx; }
.sec-title { font-size: 30rpx; font-weight: 700; display: block; margin-bottom: 20rpx; }
.score-line { display: flex; align-items: center; gap: 16rpx; margin-bottom: 16rpx; }
.s-type { width: 120rpx; font-size: 24rpx; color: #666; flex-shrink: 0; }
.s-bar-bg { flex: 1; height: 12rpx; background: #e8e8e8; border-radius: 6rpx; overflow: hidden; }
.s-bar { height: 100%; background: #4CAF50; border-radius: 6rpx; }
.s-num { width: 80rpx; font-size: 24rpx; color: #4CAF50; font-weight: 600; text-align: right; flex-shrink: 0; }
.phase-box { background: #fff; border-radius: 12rpx; padding: 20rpx 24rpx; margin-bottom: 12rpx; }
.ph-name { font-size: 28rpx; font-weight: 600; display: block; }
.ph-range { font-size: 22rpx; color: #4CAF50; display: block; margin-top: 4rpx; }
.ph-focus { font-size: 24rpx; color: #666; display: block; margin-top: 4rpx; }
.daily { font-size: 26rpx; color: #666; background: #fff; border-radius: 12rpx; padding: 20rpx 24rpx; display: block; line-height: 1.6; }
.mile-line { display: flex; gap: 16rpx; margin-bottom: 16rpx; align-items: flex-start; }
.mile-day { background: #4CAF50; color: #fff; font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 20rpx; flex-shrink: 0; }
.mile-desc { font-size: 26rpx; color: #1a1a1a; }
</style>

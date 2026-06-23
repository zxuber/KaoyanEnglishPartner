<template>
  <view class="onboarding">
    <view class="progress-bar">
      <view class="progress-fill" :style="{ width: (step / 8 * 100) + '%' }"></view>
    </view>

    <!-- Q1 -->
    <view v-if="step === 1" class="step">
      <text class="q">学习画像 1 / 8</text>
      <text class="title">你考英语一还是英语二？</text>
      <view class="two-btns">
        <view class="big-btn" @click="pickExam('英语一')"><text>英语一</text><text class="sub">学术学位</text></view>
        <view class="big-btn" @click="pickExam('英语二')"><text>英语二</text><text class="sub">专业学位</text></view>
      </view>
    </view>

    <!-- Q2 -->
    <view v-if="step === 2" class="step">
      <text class="q">学习画像 2 / 8</text>
      <text class="title">目标分数是多少？</text>
      <text class="big-num">{{ targetScore }} 分</text>
      <slider :value="targetScore" @change="onScoreChange" :min="40" :max="80" :step="1" activeColor="#4CAF50" blockSize="20" />
      <view class="row-labels"><text>40</text><text>50</text><text>60</text><text>70</text><text>80</text></view>
      <view class="next-btn" @click="nextStep">下一步</view>
    </view>

    <!-- Q3 -->
    <view v-if="step === 3" class="step">
      <text class="q">学习画像 3 / 8</text>
      <text class="title">距离考试还有多少天？</text>
      <input class="day-input" type="number" v-model="dayInput" placeholder="输入天数如180" />
      <view class="next-btn" :class="{ dim: !dayValid }" @click="confirmDay">{{ dayValid ? '下一步 (' + dayInput + ' 天)' : '请输入天数' }}</view>
    </view>

    <!-- Q4 -->
    <view v-if="step === 4" class="step">
      <text class="q">学习画像 4 / 8</text>
      <text class="title">最近一次真题或模拟，大概多少分？</text>
      <view v-for="s in scoreBands" :key="s" class="pick-row" :class="{ on: currentScoreBand===s }" @click="pickScoreBand(s)">
        <text>{{ s }}</text>
      </view>
    </view>

    <!-- Q5 -->
    <view v-if="step === 5" class="step">
      <text class="q">学习画像 5 / 8</text>
      <text class="title">你当前的英语基础更接近哪一档？</text>
      <view v-for="lv in levels" :key="lv" class="pick-row" :class="{ on: englishLevel===lv }" @click="pickLevel(lv)">
        <text>{{ lv }}</text>
      </view>
    </view>

    <!-- Q6 -->
    <view v-if="step === 6" class="step">
      <text class="q">学习画像 6 / 8</text>
      <text class="title">你最想优先解决哪些短板？</text>
      <text class="sub">可多选，至少选 1 个</text>
      <view v-for="w in weakOptions" :key="w" class="pick-row check" :class="{ on: weakModules.includes(w) }" @click="toggleWeak(w)">
        <view class="box" :class="{ fill: weakModules.includes(w) }"><text v-if="weakModules.includes(w)">✓</text></view>
        <text>{{ w }}</text>
      </view>
      <view class="next-btn" :class="{ dim: weakModules.length === 0 }" @click="confirmWeakModules">下一步</view>
    </view>

    <!-- Q7 -->
    <view v-if="step === 7" class="step">
      <text class="q">学习画像 7 / 8</text>
      <text class="title">你的时间和习惯大概是怎样的？</text>
      <text class="sub">先选工作日，再选周末，再选最稳定学习时段</text>
      <text class="section-label">工作日每天能学多久？</text>
      <view class="chips">
        <view v-for="h in ['30min','45min','1h','1.5h','2h+']" :key="h" class="chip" :class="{ on: weekdayHours===h }" @click="weekdayHours = h">
          <text>{{ h }}</text>
        </view>
      </view>
      <text class="section-label">周末通常能学多久？</text>
      <view class="chips">
        <view v-for="h in ['1h以内','1-2h','2-3h','3h+']" :key="h" class="chip" :class="{ on: weekendHours===h }" @click="weekendHours = h">
          <text>{{ h }}</text>
        </view>
      </view>
      <text class="section-label">最稳定的学习时段？</text>
      <view class="chips">
        <view v-for="slot in timeSlots" :key="slot" class="chip" :class="{ on: studyTimeSlot===slot }" @click="studyTimeSlot = slot">
          <text>{{ slot }}</text>
        </view>
      </view>
      <view class="next-btn" :class="{ dim: !timeProfileReady }" @click="confirmTimeProfile">下一步</view>
    </view>

    <!-- Q8 -->
    <view v-if="step === 8" class="step">
      <text class="q">学习画像 8 / 8</text>
      <text class="title">最后两件事：你更适合哪种节奏？</text>
      <text class="section-label">你更喜欢哪种计划风格？</text>
      <view class="chips">
        <view v-for="style in planStyles" :key="style" class="chip" :class="{ on: planStyle===style }" @click="planStyle = style">
          <text>{{ style }}</text>
        </view>
      </view>
      <text class="section-label">你当前最大的卡点是什么？</text>
      <view class="chips">
        <view v-for="ob in obstacleOptions" :key="ob" class="chip" :class="{ on: biggestObstacle===ob }" @click="biggestObstacle = ob">
          <text>{{ ob }}</text>
        </view>
      </view>
      <text class="section-label">手头已有资料</text>
      <view v-for="m in allMat" :key="m" class="pick-row check" :class="{ on: materials.includes(m) }" @click="toggleMat(m)">
        <view class="box" :class="{ fill: materials.includes(m) }"><text v-if="materials.includes(m)">✓</text></view>
        <text>{{ m }}</text>
      </view>
      <view class="next-btn submit" :class="{ dim: !submitReady }" @click="doSubmit">生成我的专属方案</view>
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
          <text class="sec-title">诊断摘要</text>
          <view v-for="(item, idx) in plan.diagnosisSummary" :key="idx" class="diag-line">
            <text class="diag-dot">•</text>
            <text class="diag-text">{{ item }}</text>
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
          <text class="sec-title">📌 近期重点</text>
          <view v-for="w in plan.weeklyFocus" :key="w.label" class="phase-box">
            <text class="ph-name">{{ w.label }}</text>
            <text class="ph-focus">{{ w.tasks }}</text>
          </view>
        </view>
        <view class="sec">
          <text class="sec-title">⏰ 工作日模板</text>
          <text class="daily">{{ plan.weekdayTemplate }}</text>
        </view>
        <view class="sec">
          <text class="sec-title">🗓 周末模板</text>
          <text class="daily">{{ plan.weekendTemplate }}</text>
        </view>
        <view class="sec">
          <text class="sec-title">⚠ 风险提醒</text>
          <text class="daily">{{ plan.riskTip }}</text>
        </view>
        <view class="sec">
          <text class="sec-title">🔄 中断补救</text>
          <text class="daily">{{ plan.catchUpAdvice }}</text>
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
import { get, post } from '@/utils/request';
import { ensureAuthed } from '@/utils/auth';
import { saveSession, getUserId, isOnboardingDone } from '@/utils/session';

export default {
  data() {
    return {
      reviewMode: false,
      step: 1,
      loading: false,
      plan: null,
      examType: '',
      targetScore: 60,
      remainingDays: 0,
      currentScoreBand: '',
      englishLevel: '',
      weakModules: [],
      weekdayHours: '',
      weekendHours: '',
      studyTimeSlot: '',
      planStyle: '',
      biggestObstacle: '',
      materials: [],
      dayInput: '',
      levels: ['四级未过', '四级低分飘过', '四级高分', '六级已过', '六级高分'],
      scoreBands: ['从没做过真题/模考', '40分以下', '40-49分', '50-59分', '60分及以上'],
      weakOptions: ['词汇记不住', '长难句看不懂', '阅读总错很多', '翻译不会下手', '作文不会写', '做题节奏很乱'],
      timeSlots: ['早上', '中午/午休', '晚上', '时间很不固定'],
      planStyles: ['固定型：每天告诉我学什么', '弹性型：给我重点和顺序就行'],
      obstacleOptions: ['坚持不下去', '时间不稳定', '单词总忘', '阅读提不上去', '作文没思路', '不知道先学什么'],
      allMat: ['红宝书', '恋练有词', '刘晓燕系列', '考研真相', '张剑黄皮书', '王江涛作文', '还没有资料'],
    };
  },
  computed: {
    dayValid() {
      const d = parseInt(this.dayInput);
      return d > 0 && d <= 730;
    },
    timeProfileReady() {
      return !!this.weekdayHours && !!this.weekendHours && !!this.studyTimeSlot;
    },
    submitReady() {
      return !!this.planStyle && !!this.biggestObstacle;
    },
  },
  async onLoad(options) {
    this.reviewMode = options?.review === '1';
    try {
      await ensureAuthed();
      if (this.reviewMode && getUserId()) {
        this.loading = true;
        const res = await get(`/users/${getUserId()}/plan`);
        if (res.data) {
          this.plan = res.data;
        } else {
          uni.showToast({ title: '未找到已生成方案', icon: 'none' });
        }
      }
    } catch (e) {
      uni.showToast({ title: this.reviewMode ? '读取方案失败，请重试' : '微信登录失败，请重试', icon: 'none' });
    } finally {
      this.loading = false;
    }
  },
  onShow() {
    if (!this.reviewMode && isOnboardingDone() && getUserId()) {
      uni.reLaunch({ url: '/pages/home/index' });
    }
  },
  methods: {
    pickExam(val) { this.examType = val; this.step = 2; },
    onScoreChange(e) { this.targetScore = e.detail.value; },
    nextStep() { this.step = 3; },
    onDayInput(e) { this.dayInput = e.detail.value; },
    confirmDay() { if (this.dayValid) { this.remainingDays = parseInt(this.dayInput); this.step = 4; } },
    pickScoreBand(val) { this.currentScoreBand = val; this.step = 5; },
    pickLevel(lv) { this.englishLevel = lv; this.step = 6; },
    toggleWeak(w) {
      const i = this.weakModules.indexOf(w);
      if (i >= 0) this.weakModules.splice(i, 1);
      else this.weakModules.push(w);
    },
    confirmWeakModules() {
      if (this.weakModules.length > 0) this.step = 7;
    },
    confirmTimeProfile() {
      if (this.timeProfileReady) this.step = 8;
    },
    toggleMat(m) {
      const i = this.materials.indexOf(m);
      if (i >= 0) this.materials.splice(i, 1);
      else this.materials.push(m);
    },
    async doSubmit() {
      this.loading = true;
      try {
        const res = await post('/users/onboarding', {
          userId: getUserId(),
          examType: this.examType,
          targetScore: this.targetScore,
          remainingDays: this.remainingDays,
          currentScoreBand: this.currentScoreBand,
          englishLevel: this.englishLevel,
          weakModules: this.weakModules,
          weekdayHours: this.weekdayHours,
          weekendHours: this.weekendHours,
          studyTimeSlot: this.studyTimeSlot,
          planStyle: this.planStyle,
          biggestObstacle: this.biggestObstacle,
          materials: this.materials,
        });
        if (res.data && res.data.plan) {
          this.plan = res.data.plan;
          if (res.data.userId) saveSession(res.data.userId, undefined, true);
        }
      } catch (e) {
        this.plan = {
          diagnosisSummary: [
            '你的目标分数需要通过分阶段推进来完成，不能只靠突击。',
            '当前最需要优先处理的是你自己勾选的薄弱模块。',
            '计划会优先围绕你的时间条件和可执行性来安排。'
          ],
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
          weeklyFocus: [
            { label: '前两周重点', tasks: '先把词汇和阅读主线立起来，建立稳定节奏。' },
            { label: '中期重点', tasks: '阅读真题精读 + 作文素材积累并行推进。' },
          ],
          weekdayTemplate: '工作日优先做最重要的一项，再留出一点时间复盘和记忆强化。',
          weekendTemplate: '周末安排完整的真题训练、集中复盘或作文专项。',
          riskTip: '最容易掉队的不是不会学，而是任务太大导致中断。',
          catchUpAdvice: '如果断了几天，先恢复词汇和阅读主线，不要想着一次补完全部内容。',
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
.section-label { display: block; margin: 28rpx 0 16rpx; font-size: 26rpx; color: #666; font-weight: 600; }
.chips { display: flex; flex-wrap: wrap; gap: 16rpx; margin-bottom: 8rpx; }
.chip { background: #fff; border: 2rpx solid #e8e8e8; border-radius: 999rpx; padding: 18rpx 24rpx; font-size: 26rpx; color: #333; }
.chip.on { border-color: #4CAF50; background: #f0fff0; color: #2f8f33; font-weight: 600; }
.loading-mask { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 100; }
.loading-box { background: #fff; border-radius: 24rpx; padding: 60rpx 40rpx; text-align: center; font-size: 32rpx; }
.plan-page { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: #f5f5f5; z-index: 100; overflow-y: auto; }
.plan-card { padding: 60rpx 40rpx; }
.plan-h1 { font-size: 40rpx; font-weight: 800; text-align: center; display: block; margin-bottom: 40rpx; }
.sec { margin-bottom: 40rpx; }
.sec-title { font-size: 30rpx; font-weight: 700; display: block; margin-bottom: 20rpx; }
.diag-line { display: flex; gap: 12rpx; margin-bottom: 12rpx; align-items: flex-start; }
.diag-dot { color: #4CAF50; font-size: 28rpx; font-weight: 700; }
.diag-text { flex: 1; font-size: 26rpx; color: #444; line-height: 1.6; }
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

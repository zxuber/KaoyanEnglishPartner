<template>
  <view class="word-flash">
    <!-- ===== 开始屏幕 ===== -->
    <view v-if="!roundActive && !roundFinished" class="start-screen">
      <text class="word-icon">[词]</text>
      <text class="start-title">单词速记</text>
      <text class="start-sub">看英文说中文 · 语音录入 · 每轮 20 词</text>
      <view class="start-info">
        <view class="info-item">
          <text class="info-num">{{ stats.mastered || 0 }}</text>
          <text class="info-label">已掌握</text>
        </view>
        <view class="info-item">
          <text class="info-num">{{ stats.total || 0 }}</text>
          <text class="info-label">学习中</text>
        </view>
      </view>
      <view class="btn-group">
        <view class="btn-primary" @click="startRound(1)">从 Unit 1 开始</view>
      </view>
    </view>

    <!-- ===== 答题中 ===== -->
    <view v-if="roundActive" class="card-screen">
      <!-- 顶栏 -->
      <view class="round-progress">
        <text class="progress-idx">{{ currentIdx + 1 }} / {{ words.length }}</text>
        <view class="pause-btn" @click="togglePause">
          <text class="pause-symbol">{{ paused ? '▶' : '||' }}</text>
        </view>
        <text class="correct-badge">对 {{ correctCount }}</text>
      </view>

      <!-- 单词卡片 -->
      <view class="word-card" :class="{ revealed: revealed }">
        <text class="word-text">{{ currentWord.word }}</text>
        <view v-if="revealed" class="meaning-box">
          <text v-if="feedbackType === 'correct'" class="meaning-correct">正确!</text>
          <text v-else class="meaning-wrong">{{ correctMeaning }}</text>
        </view>
        <view v-else class="meaning-hidden">
          <text>说出中文意思</text>
        </view>
      </view>

      <!-- 倒计时条 -->
      <view class="countdown-wrap">
        <view class="countdown-bar">
          <view
            class="countdown-fill"
            :class="[countdownColor, { paused: paused }]"
            :style="{ width: countdownPercent + '%' }"
          ></view>
        </view>
        <text class="countdown-text">{{ paused ? '[已暂停]' : countdownText }}</text>
      </view>

      <!-- ===== 语音输入 ===== -->
      <view class="voice-area">
        <view
          class="mic-btn"
          :class="{ recording: isRecording, disabled: revealed || paused }"
          @touchstart.prevent="startVoice"
          @touchend.prevent="stopVoice"
          @touchcancel.prevent="stopVoice"
        >
          <view class="mic-ring" :class="{ pulse: isRecording }">
            <text class="mic-icon-text">{{ isRecording ? '...' : 'Mic' }}</text>
          </view>
          <text class="mic-label">{{ isRecording ? '松开发送' : '按住说出中文意思' }}</text>
        </view>
        <view v-if="recognizedText && !revealed" class="recognized-text">{{ recognizedText }}</view>
        <view v-if="uploading" class="recognized-text uploading">识别中...</view>
        <view class="btn-unknown-alone" :class="{ dim: revealed }" @click="markUnknown">
          <text>不认识</text>
        </view>
      </view>

      <!-- ===== 暂停遮罩 ===== -->
      <view v-if="paused" class="pause-overlay" @click="togglePause">
        <view class="pause-card">
          <text class="pause-icon">||</text>
          <text class="pause-title">已暂停</text>
          <text class="pause-sub">点击任意位置继续</text>
        </view>
      </view>
    </view>

    <!-- ===== 轮次结束 ===== -->
    <view v-if="roundFinished" class="result-screen">
      <text class="result-icon">{{ correctCount >= 15 ? '太棒了' : correctCount >= 10 ? '不错' : '加油' }}</text>
      <text class="result-title">本轮结束</text>
      <text class="result-score">{{ correctCount }} / {{ words.length }}</text>
      <text class="result-detail">标记 {{ words.length - correctCount }} 个待复习词</text>
      <view class="btn-group">
        <view class="btn-primary" @click="nextRound">下一轮</view>
        <view class="btn-outline" @click="goHome">回首页</view>
      </view>
    </view>
  </view>
</template>

<script>
import { get, post } from '@/utils/request';
import { getUserId } from '@/utils/session';

const COUNTDOWN = 5;
const SPEECH_API = 'http://localhost:8080/api/v1/speech/recognize';

export default {
  data() {
    return {
      userId: null,
      words: [],
      currentIdx: 0,
      correctCount: 0,
      revealed: false,
      feedbackType: '',
      correctMeaning: '',
      roundActive: false,
      roundFinished: false,
      paused: false,

      // 语音录制
      voiceAvailable: true,
      isRecording: false,
      uploading: false,
      recognizedText: '',
      recorderManager: null,

      // 倒计时
      countdownLeft: COUNTDOWN,
      countdownTimer: null,

      stats: { mastered: 0, total: 0 },
    };
  },
  computed: {
    currentWord() {
      return this.words[this.currentIdx] || {};
    },
    countdownPercent() {
      return (this.countdownLeft / COUNTDOWN) * 100;
    },
    countdownText() {
      return this.countdownLeft + 's';
    },
    countdownColor() {
      if (this.countdownLeft > COUNTDOWN * 0.6) return 'green';
      if (this.countdownLeft > COUNTDOWN * 0.3) return 'yellow';
      return 'red';
    },
  },
  onLoad() {
    this.userId = getUserId();
    this.initRecorder();
  },
  onUnload() {
    clearInterval(this.countdownTimer);
    if (this.recorderManager) {
      try { this.recorderManager.stop(); } catch (e) {}
    }
  },
  methods: {
    // ---- 录音初始化 ----
    initRecorder() {
      try {
        const rm = uni.getRecorderManager();
        rm.onStart(() => {
          this.isRecording = true;
          this.recognizedText = '';
          this.uploading = false;
        });
        rm.onStop((res) => {
          this.isRecording = false;
          if (res.tempFilePath && !this.revealed && this.countdownLeft > 0) {
            this.uploadAndRecognize(res.tempFilePath);
          }
        });
        rm.onError((err) => {
          console.error('录音失败:', err);
          this.isRecording = false;
        });
        this.recorderManager = rm;
        this.voiceAvailable = true;
      } catch (e) {
        this.voiceAvailable = false;
      }
    },

    startVoice() {
      if (this.revealed || this.paused || this.isRecording) return;
      this.recorderManager.start({
        format: 'wav',
        sampleRate: 16000,
        numberOfChannels: 1,
        encodeBitRate: 48000,
      });
    },

    stopVoice() {
      if (!this.isRecording) return;
      clearInterval(this.countdownTimer);
      try { this.recorderManager.stop(); } catch (e) {}
    },

    async uploadAndRecognize(filePath) {
      this.uploading = true;
      clearInterval(this.countdownTimer);
      console.log('[Voice] 开始上传录音:', filePath);
      try {
        const uploadRes = await new Promise((resolve, reject) => {
          uni.uploadFile({
            url: SPEECH_API,
            filePath: filePath,
            name: 'audio',
            timeout: 15000,
            success: (res) => {
              console.log('[Voice] 上传响应 statusCode:', res.statusCode);
              console.log('[Voice] 上传响应 data:', res.data);
              try { resolve(JSON.parse(res.data)); }
              catch (e) {
                console.error('[Voice] JSON解析失败, raw data:', res.data);
                reject(new Error('后端返回非JSON: ' + (res.data || '').substring(0, 100)));
              }
            },
            fail: (err) => {
              console.error('[Voice] uploadFile 失败:', JSON.stringify(err));
              reject(new Error('上传失败: ' + (err.errMsg || 'unknown')));
            },
          });
        });

        this.uploading = false;
        const data = uploadRes.data;
        console.log('[Voice] 解析后 data:', JSON.stringify(data));
        if (data && data.success && data.text && !this.revealed) {
          this.recognizedText = data.text;
          this.submitVoiceAnswer(data.text);
        } else if (!this.revealed) {
          const reason = data && data.error ? data.error : (data && !data.success ? '识别失败' : '未检测到语音内容');
          console.warn('[Voice] 识别失败:', reason);
          this.startCountdown();
          uni.showToast({ title: reason, icon: 'none', duration: 2500 });
        }
      } catch (e) {
        this.uploading = false;
        console.error('[Voice] 异常:', e.message || e);
        if (!this.revealed) {
          this.startCountdown();
          uni.showToast({ title: e.message || '网络错误，请重试', icon: 'none', duration: 2500 });
        }
      }
    },

    async submitVoiceAnswer(text) {
      if (!text || this.revealed) return;
      clearInterval(this.countdownTimer);
      const answer = text.trim();
      this.revealed = true;
      await this.judgeAnswer(answer);
      setTimeout(() => this.nextWord(), 1800);
    },

    // ---- 暂停 ----
    togglePause() {
      if (this.revealed) return;
      this.paused = !this.paused;
    },

    // ---- 倒计时 ----
    startCountdown() {
      this.countdownLeft = COUNTDOWN;
      this.paused = false;
      clearInterval(this.countdownTimer);
      this.countdownTimer = setInterval(() => {
        if (this.paused) return;
        this.countdownLeft--;
        if (this.countdownLeft <= 0) {
          clearInterval(this.countdownTimer);
          if (this.isRecording) {
            try { this.recorderManager.stop(); } catch (e) {}
          }
          if (!this.revealed) {
            this.revealed = true;
            this.feedbackType = 'timeout';
            this.correctMeaning = this.currentWord.meaning || '';
            setTimeout(() => this.nextWord(), 2000);
          }
        }
      }, 1000);
    },

    // ---- 开始一轮 ----
    async startRound(unit) {
      try {
        const res = await get('/words/new', { userId: this.userId, unit });
        if (res.data && res.data.length > 0) {
          this.words = res.data;
        } else {
          this.words = this.getTestWords();
        }
      } catch (e) {
        this.words = this.getTestWords();
      }
      this.currentIdx = 0;
      this.correctCount = 0;
      this.roundActive = true;
      this.roundFinished = false;
      this.revealed = false;
      this.paused = false;
      this.recognizedText = '';
      this.uploading = false;
      this.startCountdown();
    },

    // ---- 判断答案 ----
    async judgeAnswer(answer) {
      try {
        const res = await post('/words/check', {
          userId: this.userId,
          wordId: this.currentWord.id,
          userAnswer: answer,
        });
        if (res.data && res.data.correct) {
          this.correctCount++;
          this.feedbackType = 'correct';
          this.correctMeaning = '';
          uni.showToast({ title: '✔ 正确', icon: 'none', duration: 800 });
        } else {
          this.feedbackType = 'wrong';
          this.correctMeaning = (res.data && res.data.meaning) || this.currentWord.meaning || '';
        }
      } catch (e) {
        console.error('[Word] backend error:', e);
        const meaning = this.currentWord.meaning || '';
        // Simple fallback: check if answer contains any part of the meaning
        const ans = (answer || '').replace(/[，。；：、！？\s]+/g, '');
        if (meaning && ans && meaning.includes(ans)) {
          this.correctCount++;
          this.feedbackType = 'correct';
          this.correctMeaning = '';
        } else {
          this.feedbackType = 'wrong';
          this.correctMeaning = meaning;
        }
      }
    },

    async markUnknown() {
      if (this.revealed) return;
      clearInterval(this.countdownTimer);
      this.revealed = true;
      this.feedbackType = 'wrong';
      this.correctMeaning = this.currentWord.meaning || '';
      this.recognizedText = '';
      try {
        await post('/words/check', {
          userId: this.userId,
          wordId: this.currentWord.id,
          unknown: true,
        });
      } catch (e) {}
      setTimeout(() => this.nextWord(), 2000);
    },

    nextWord() {
      this.revealed = false;
      this.recognizedText = '';
      this.uploading = false;
      this.paused = false;
      if (this.currentIdx < this.words.length - 1) {
        this.currentIdx++;
        this.startCountdown();
      } else {
        this.finishRound();
      }
    },

    finishRound() {
      this.roundActive = false;
      this.roundFinished = true;
    },

    nextRound() {
      this.roundFinished = false;
      this.startRound(1);
    },

    goHome() {
      uni.reLaunch({ url: '/pages/home/index' });
    },

    getTestWords() {
      return [
        { id: 1, word: 'abandon', meaning: '放弃；遗弃' },
        { id: 2, word: 'radiate', meaning: '散发；流露；发出(光、辐射等)' },
        { id: 3, word: 'grievance', meaning: '不满；怨恨' },
        { id: 4, word: 'assumption', meaning: '假设；承担' },
        { id: 5, word: 'capable', meaning: '有能力的' },
        { id: 6, word: 'underlying', meaning: '潜在的；根本的' },
        { id: 7, word: 'articulate', meaning: '清楚表达；明确表达' },
        { id: 8, word: 'phenomenon', meaning: '现象' },
        { id: 9, word: 'significant', meaning: '重要的；显著的' },
        { id: 10, word: 'approach', meaning: '方法；途径；接近' },
        { id: 11, word: 'evidence', meaning: '证据；证明' },
        { id: 12, word: 'establish', meaning: '建立；创立' },
        { id: 13, word: 'consequence', meaning: '结果；后果' },
        { id: 14, word: 'environment', meaning: '环境' },
        { id: 15, word: 'contribute', meaning: '贡献；捐献；导致' },
        { id: 16, word: 'advocate', meaning: '提倡；主张；拥护者' },
        { id: 17, word: 'restrict', meaning: '限制；约束' },
        { id: 18, word: 'controversy', meaning: '争论；争议' },
        { id: 19, word: 'demonstrate', meaning: '展示；证明；示威' },
        { id: 20, word: 'fundamental', meaning: '基本的；根本的' },
      ];
    },
  },
};
</script>

<style scoped>
.word-flash { min-height: 100vh; background: #f5f5f5; }

/* ===== 开始屏幕 ===== */
.start-screen { padding: 120rpx 40rpx; text-align: center; }
.word-icon {
  font-size: 48rpx; font-weight: 800; display: inline-block;
  background: #4CAF50; color: #fff; width: 100rpx; height: 100rpx;
  line-height: 100rpx; border-radius: 24rpx; margin-bottom: 24rpx;
}
.start-title { font-size: 48rpx; font-weight: 800; display: block; margin-bottom: 16rpx; color: #1a1a1a; }
.start-sub { font-size: 26rpx; color: #999; display: block; margin-bottom: 60rpx; }
.start-info { display: flex; justify-content: center; gap: 80rpx; margin-bottom: 60rpx; }
.info-item { text-align: center; }
.info-num { font-size: 48rpx; font-weight: 800; color: #4CAF50; display: block; }
.info-label { font-size: 24rpx; color: #999; margin-top: 8rpx; display: block; }
.btn-group { display: flex; flex-direction: column; gap: 20rpx; align-items: center; }
.btn-primary {
  background: #4CAF50; color: #fff; padding: 28rpx 80rpx;
  border-radius: 16rpx; font-size: 32rpx; font-weight: 600; width: 500rpx; text-align: center;
}
.btn-outline {
  border: 2rpx solid #4CAF50; color: #4CAF50; padding: 28rpx 80rpx;
  border-radius: 16rpx; font-size: 32rpx; font-weight: 600; width: 500rpx; text-align: center;
}

/* ===== 答题卡片区 ===== */
.card-screen { padding: 40rpx; display: flex; flex-direction: column; align-items: center; min-height: 100vh; position: relative; }

/* 顶栏 */
.round-progress { width: 100%; display: flex; justify-content: space-between; align-items: center; margin-bottom: 40rpx; font-size: 26rpx; color: #666; }
.progress-idx { font-weight: 600; }
.pause-btn { width: 56rpx; height: 56rpx; display: flex; align-items: center; justify-content: center; border-radius: 50%; background: #fff; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.08); }
.pause-symbol { font-size: 22rpx; font-weight: 800; color: #666; letter-spacing: -2rpx; }
.correct-badge { color: #4CAF50; font-weight: 600; }

/* 单词卡片 */
.word-card {
  width: 100%; background: #fff; border-radius: 24rpx;
  padding: 80rpx 40rpx; text-align: center; margin-bottom: 30rpx;
  box-shadow: 0 4rpx 24rpx rgba(0,0,0,0.06); min-height: 260rpx;
  display: flex; flex-direction: column; justify-content: center; align-items: center;
}
.word-card.revealed { background: #f8fff8; }
.word-text { font-size: 56rpx; font-weight: 800; color: #1a1a1a; display: block; }
.meaning-hidden { font-size: 28rpx; color: #ccc; margin-top: 24rpx; }
.meaning-box { margin-top: 24rpx; }
.meaning-correct { font-size: 32rpx; color: #4CAF50; font-weight: 600; }
.meaning-wrong { font-size: 28rpx; color: #F44336; }

/* 倒计时 */
.countdown-wrap { width: 100%; margin-bottom: 30rpx; }
.countdown-bar { height: 6rpx; background: #e8e8e8; border-radius: 3rpx; overflow: hidden; }
.countdown-fill { height: 100%; border-radius: 3rpx; transition: width 1s linear; }
.countdown-fill.green { background: #4CAF50; }
.countdown-fill.yellow { background: #FFC107; }
.countdown-fill.red { background: #F44336; }
.countdown-fill.paused { transition: none; background: #bbb; }
.countdown-text { text-align: center; font-size: 22rpx; color: #999; margin-top: 6rpx; display: block; }

/* ===== 语音输入区 ===== */
.voice-area { width: 100%; display: flex; flex-direction: column; align-items: center; }
.mic-btn {
  width: 100%; display: flex; flex-direction: column; align-items: center; padding: 30rpx 0;
}
.mic-btn.disabled { opacity: 0.4; pointer-events: none; }
.mic-ring {
  width: 140rpx; height: 140rpx; border-radius: 50%;
  background: #f0f0f0; display: flex; align-items: center; justify-content: center;
  margin-bottom: 16rpx; transition: transform 0.2s, background 0.2s;
}
.mic-icon-text { font-size: 28rpx; font-weight: 700; color: #999; }
.mic-ring.pulse {
  background: #4CAF50;
  animation: micPulse 0.8s ease-in-out infinite;
}
.mic-ring.pulse .mic-icon-text { color: #fff; }
.mic-label { font-size: 26rpx; color: #999; }
.recognized-text {
  width: 100%; background: #fff; border: 2rpx solid #4CAF50; border-radius: 16rpx;
  padding: 24rpx 28rpx; font-size: 32rpx; text-align: center; margin-bottom: 20rpx;
  box-sizing: border-box; color: #333;
}
.recognized-text.uploading { border-color: #FFC107; color: #999; }
.btn-unknown-alone {
  width: 100%; background: #f0f0f0; color: #666; text-align: center;
  padding: 24rpx; border-radius: 12rpx; font-size: 30rpx; font-weight: 600;
}
.btn-unknown-alone.dim { opacity: 0.4; pointer-events: none; }

@keyframes micPulse {
  0%, 100% { transform: scale(1); box-shadow: 0 0 0 0 rgba(76,175,80,0.4); }
  50% { transform: scale(1.06); box-shadow: 0 0 0 20rpx rgba(76,175,80,0); }
}

/* ===== 暂停遮罩 ===== */
.pause-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.45); display: flex;
  align-items: center; justify-content: center; z-index: 100;
}
.pause-card {
  background: #fff; border-radius: 24rpx; padding: 60rpx 80rpx;
  text-align: center; margin: 0 80rpx;
}
.pause-icon { font-size: 80rpx; display: block; margin-bottom: 16rpx; color: #666; }
.pause-title { font-size: 36rpx; font-weight: 700; color: #1a1a1a; display: block; margin-bottom: 12rpx; }
.pause-sub { font-size: 26rpx; color: #999; display: block; }

/* ===== 结束屏幕 ===== */
.result-screen { padding: 120rpx 40rpx; text-align: center; }
.result-icon { font-size: 48rpx; font-weight: 800; color: #4CAF50; display: block; margin-bottom: 24rpx; }
.result-title { font-size: 36rpx; font-weight: 700; color: #1a1a1a; display: block; margin-bottom: 20rpx; }
.result-score { font-size: 96rpx; font-weight: 800; color: #4CAF50; display: block; margin-bottom: 16rpx; }
.result-detail { font-size: 26rpx; color: #999; display: block; margin-bottom: 60rpx; }
</style>
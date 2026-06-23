<template>
  <view class="reading-page">
    <view class="hero">
      <text class="eyebrow">Reading Coach</text>
      <text class="title">阅读专项</text>
      <text class="sub">先读文章，再说思路，AI 会继续追问你。</text>
    </view>

    <view v-if="loading" class="state-card">
      <text>正在加载阅读材料...</text>
    </view>

    <template v-else>
      <view class="passage-card">
        <view class="passage-head">
          <view>
            <text class="article-label">本篇材料</text>
            <text class="article-title">{{ article.title || "阅读专项" }}</text>
            <text class="article-source">{{ article.source || "阅读教练式训练" }}</text>
            <text class="section-title">文章</text>
            <text class="passage-progress">第 {{ currentParagraphIndex + 1 }} / {{ paragraphs.length || 1 }} 段</text>
          </view>
          <view class="page-pills">
            <view
              v-for="(_, idx) in paragraphs"
              :key="idx"
              class="page-pill"
              :class="{ active: idx === currentParagraphIndex }"
              @click="currentParagraphIndex = idx"
            />
          </view>
        </view>

        <view class="passage-stage">
          <text class="passage-kicker">逐段阅读，减轻整篇压迫感</text>
          <text class="passage-text featured">{{ currentParagraph }}</text>
        </view>

        <view class="passage-actions">
          <view class="nav-btn" :class="{ disabled: currentParagraphIndex === 0 }" @click="goPrevParagraph">
            <text>上一段</text>
          </view>
          <view
            class="nav-btn primary"
            :class="{ disabled: currentParagraphIndex === paragraphs.length - 1 }"
            @click="goNextParagraph"
          >
            <text>{{ currentParagraphIndex === paragraphs.length - 1 ? "已到最后一段" : "下一段" }}</text>
          </view>
        </view>
      </view>

      <view class="question-card">
        <text class="section-title">题目</text>
        <text class="question-stem">{{ currentQuestion.stem }}</text>
        <text class="question-focus">考查点：{{ currentQuestion.focus }}</text>
        <view class="option-list">
          <view
            v-for="option in currentQuestion.options"
            :key="option.label"
            class="option-item"
            :class="{ selected: selectedOption === option.label }"
            @click="selectedOption = option.label"
          >
            <text class="option-label">{{ option.label }}</text>
            <text class="option-content">{{ option.content }}</text>
          </view>
        </view>
      </view>

      <view class="answer-card">
        <view class="input-head">
          <text class="section-title">先说你的思路</text>
          <view class="mode-tabs">
            <view
              class="mode-tab"
              :class="{ active: inputMode === 'text' }"
              @click="switchInputMode('text')"
            >
              <text>文字输入</text>
            </view>
            <view
              class="mode-tab"
              :class="{ active: inputMode === 'voice' }"
              @click="switchInputMode('voice')"
            >
              <text>语音录入</text>
            </view>
          </view>
        </view>

        <template v-if="inputMode === 'text'">
          <view v-if="selectedOption" class="selection-hint">
            <text class="selection-hint-label">当前倾向</text>
            <text class="selection-hint-value">{{ selectedOption }}</text>
          </view>
          <textarea
            v-model="answerText"
            class="answer-box"
            maxlength="1000"
            placeholder="你可以先说：题目问的是什么、你定位到了哪一段、为什么倾向这个选项、排除了哪些干扰项。"
          />
        </template>

        <template v-else>
          <view class="voice-panel">
            <view v-if="selectedOption" class="selection-hint selection-hint-voice">
              <text class="selection-hint-label">当前倾向</text>
              <text class="selection-hint-value">{{ selectedOption }}</text>
            </view>
            <text class="voice-title">先口头讲思路，再提交给 AI 教练</text>
            <text class="voice-sub">
              {{ isRecording ? `剩余 ${voiceCountdown}s` : "单次最多 30 秒，停止后会自动转文字" }}
            </text>

            <view
              class="voice-button"
              :class="{ recording: isRecording, disabled: uploading || submitting }"
              @click="toggleVoiceRecording"
            >
              <text class="voice-button-icon">{{ isRecording ? "■" : "●" }}</text>
              <text class="voice-button-text">
                {{ isRecording ? "停止录音" : (uploading ? "识别中..." : "开始录音") }}
              </text>
            </view>

            <view class="voice-timer">
              <view class="voice-timer-bar">
                <view class="voice-timer-fill" :style="{ width: voiceProgress + '%' }"></view>
              </view>
            </view>

            <view v-if="recognizedText" class="recognized-card">
              <text class="recognized-label">语音转写</text>
              <text class="recognized-text">{{ recognizedText }}</text>
            </view>

            <textarea
              v-model="answerText"
              class="answer-box voice-answer-box"
              maxlength="1000"
              placeholder="转写后你还可以手动补充或修正。"
            />
          </view>
        </template>

        <view class="btn-row">
          <view class="btn btn-primary" :class="{ disabled: !canSubmit }" @click="submitAnswer">
            <text>{{ revealed ? "再来一题" : (submitting ? "AI 思考中..." : "提交思路") }}</text>
          </view>
        </view>
      </view>

      <view v-if="coachReply" class="coach-card">
        <text class="section-title">AI 教练追问</text>
        <text class="coach-text">{{ coachReply }}</text>
      </view>

      <view v-if="revealed" class="result-card">
        <text class="section-title">答案与错因</text>
        <text class="result-answer">参考答案：{{ currentQuestion.answer }}</text>
        <text class="result-explanation">{{ currentQuestion.explanation }}</text>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { onLoad, onUnload } from "@dcloudio/uni-app";
import { getSpeechApiUrl } from "@/config/api";
import { ensureAuthed } from "@/utils/auth";
import { getUserId } from "@/utils/session";
import { get, post } from "@/utils/request";

type InputMode = "text" | "voice";

interface OptionItem {
  label: string;
  content: string;
}

interface QuestionItem {
  id: string;
  stem: string;
  focus: string;
  options: OptionItem[];
  answer: string;
  explanation: string;
}

interface ArticleData {
  articleId: string;
  source: string;
  title: string;
  passage: string;
  questions: QuestionItem[];
}

interface CoachResponse {
  coachReply: string;
  revealAnswer: boolean;
  answer: string;
  explanation: string;
  turn: number;
}

const VOICE_LIMIT_SECONDS = 30;

const emptyQuestion: QuestionItem = {
  id: "",
  stem: "",
  focus: "",
  options: [],
  answer: "",
  explanation: "",
};

const userId = ref<number | null>(null);
const loading = ref(true);
const submitting = ref(false);
const article = ref<ArticleData>({ articleId: "", source: "", title: "", passage: "", questions: [] });
const currentIdx = ref(0);
const currentParagraphIndex = ref(0);
const turn = ref(1);
const answerText = ref("");
const coachReply = ref("");
const revealed = ref(false);
const inputMode = ref<InputMode>("text");
const selectedOption = ref("");
const recognizedText = ref("");
const uploading = ref(false);
const isRecording = ref(false);
const recorderManager = ref<UniApp.RecorderManager | null>(null);
const voiceCountdown = ref(VOICE_LIMIT_SECONDS);
const voiceTimer = ref<number | null>(null);

const paragraphs = computed(() => {
  if (!article.value.passage) return [];
  return article.value.passage
    .split(/\n\s*\n/)
    .map((item) => item.replace(/\s*\n\s*/g, " ").replace(/\s+/g, " ").trim())
    .filter(Boolean);
});

const currentParagraph = computed(() => {
  return paragraphs.value[currentParagraphIndex.value] || article.value.passage || "";
});

const currentQuestion = computed<QuestionItem>(() => {
  return article.value.questions[currentIdx.value] || emptyQuestion;
});

const voiceProgress = computed(() => {
  return (voiceCountdown.value / VOICE_LIMIT_SECONDS) * 100;
});

const canSubmit = computed(() => {
  if (revealed.value) return true;
  return (!!answerText.value.trim() || !!selectedOption.value) && !submitting.value && !uploading.value;
});

async function loadArticle() {
  const res = await get<ArticleData>("/reading/article");
  if (res.data) {
    article.value = res.data;
    currentParagraphIndex.value = 0;
  }
}

async function syncRecentTraining() {
  if (!userId.value) return;
  try {
    await post(`/users/${userId.value}/recent-training`, {
      module: "阅读",
      title: revealed.value ? "阅读题已完成" : "阅读思路陪练",
      subtitle: revealed.value
        ? `已完成第 ${currentIdx.value + 1} 题，查看答案与错因`
        : `第 ${currentIdx.value + 1} 题 · 第 ${turn.value} 轮追问`,
      page: "/pages/reading/index",
      progressCurrent: revealed.value ? currentIdx.value + 1 : currentIdx.value,
      progressTotal: article.value.questions.length || 1,
      accent: "#b45309",
    });
  } catch (e) {}
}

function switchInputMode(mode: InputMode) {
  inputMode.value = mode;
}

function buildUserAnswer() {
  const parts: string[] = [];
  if (selectedOption.value) {
    parts.push(`我当前倾向选项：${selectedOption.value}`);
  }
  if (answerText.value.trim()) {
    parts.push(`我的思路：${answerText.value.trim()}`);
  }
  return parts.join("\n");
}

async function submitAnswer() {
  if (revealed.value) {
    nextQuestion();
    return;
  }
  if (!canSubmit.value || !userId.value) return;

  submitting.value = true;
  try {
    const res = await post<CoachResponse>("/reading/coach", {
      userId: userId.value,
      articleId: article.value.articleId,
      questionId: currentQuestion.value.id,
      userAnswer: buildUserAnswer(),
      turn: turn.value,
    });
    if (res.data) {
      coachReply.value = res.data.coachReply || "";
      revealed.value = !!res.data.revealAnswer;
      if (!revealed.value) {
        turn.value += 1;
      }
      await syncRecentTraining();
    }
  } catch (e) {
    uni.showToast({ title: "阅读教练暂时开小差了", icon: "none" });
  } finally {
    submitting.value = false;
  }
}

function resetVoiceState() {
  if (voiceTimer.value) {
    clearInterval(voiceTimer.value);
    voiceTimer.value = null;
  }
  voiceCountdown.value = VOICE_LIMIT_SECONDS;
  isRecording.value = false;
}

function initRecorder() {
  try {
    const rm = uni.getRecorderManager();
    rm.onStart(() => {
      isRecording.value = true;
      uploading.value = false;
      recognizedText.value = "";
      voiceCountdown.value = VOICE_LIMIT_SECONDS;
      if (voiceTimer.value) clearInterval(voiceTimer.value);
      voiceTimer.value = setInterval(() => {
        voiceCountdown.value -= 1;
        if (voiceCountdown.value <= 0) {
          stopVoiceRecording();
        }
      }, 1000) as unknown as number;
    });
    rm.onStop((res) => {
      const tempFilePath = res.tempFilePath;
      resetVoiceState();
      if (tempFilePath) {
        uploadAndRecognize(tempFilePath);
      }
    });
    rm.onError(() => {
      resetVoiceState();
      uni.showToast({ title: "录音失败，请重试", icon: "none" });
    });
    recorderManager.value = rm;
  } catch (e) {
    recorderManager.value = null;
  }
}

function startVoiceRecording() {
  if (!recorderManager.value || uploading.value || submitting.value) {
    uni.showToast({ title: "当前无法录音", icon: "none" });
    return;
  }
  recognizedText.value = "";
  recorderManager.value.start({
    format: "wav",
    sampleRate: 16000,
    numberOfChannels: 1,
    encodeBitRate: 48000,
  });
}

function stopVoiceRecording() {
  if (!isRecording.value || !recorderManager.value) return;
  try {
    recorderManager.value.stop();
  } catch (e) {
    resetVoiceState();
  }
}

function toggleVoiceRecording() {
  if (isRecording.value) {
    stopVoiceRecording();
    return;
  }
  startVoiceRecording();
}

async function uploadAndRecognize(filePath: string) {
  uploading.value = true;
  try {
    const uploadRes = await new Promise<any>((resolve, reject) => {
      uni.uploadFile({
        url: getSpeechApiUrl(),
        filePath,
        name: "audio",
        timeout: 30000,
        success: (res) => {
          try {
            resolve(JSON.parse(res.data));
          } catch (e) {
            reject(new Error("后端返回非 JSON"));
          }
        },
        fail: (err) => reject(new Error(err.errMsg || "上传失败")),
      });
    });

    const data = uploadRes.data;
    if (data?.success && data?.text) {
      recognizedText.value = data.text;
      answerText.value = answerText.value.trim() ? `${answerText.value}\n${data.text}` : data.text;
    } else {
      uni.showToast({ title: data?.error || "识别失败", icon: "none" });
    }
  } catch (e: any) {
    uni.showToast({ title: e?.message || "上传失败", icon: "none" });
  } finally {
    uploading.value = false;
  }
}

function nextQuestion() {
  if (currentIdx.value < article.value.questions.length - 1) {
    currentIdx.value += 1;
  } else {
    currentIdx.value = 0;
  }
  turn.value = 1;
  answerText.value = "";
  selectedOption.value = "";
  recognizedText.value = "";
  coachReply.value = "";
  revealed.value = false;
  currentParagraphIndex.value = 0;
  inputMode.value = "text";
  syncRecentTraining();
}

function goPrevParagraph() {
  if (currentParagraphIndex.value === 0) return;
  currentParagraphIndex.value -= 1;
}

function goNextParagraph() {
  if (currentParagraphIndex.value >= paragraphs.value.length - 1) return;
  currentParagraphIndex.value += 1;
}

onLoad(async () => {
  initRecorder();
  try {
    await ensureAuthed();
  } catch (e) {
    uni.showToast({ title: "微信登录失败，请重试", icon: "none" });
    loading.value = false;
    return;
  }
  userId.value = getUserId();
  try {
    await loadArticle();
    await syncRecentTraining();
  } catch (e) {
    uni.showToast({ title: "加载阅读材料失败", icon: "none" });
  } finally {
    loading.value = false;
  }
});

onUnload(() => {
  resetVoiceState();
  stopVoiceRecording();
});
</script>

<style lang="scss" scoped>
.reading-page {
  min-height: 100vh;
  padding: 34rpx 26rpx 60rpx;
  background:
    radial-gradient(circle at top right, rgba(180, 83, 9, 0.14), transparent 30%),
    linear-gradient(180deg, #f7f1ea 0%, #f9f6f1 100%);
}

.hero,
.passage-card,
.question-card,
.answer-card,
.coach-card,
.result-card,
.state-card {
  background: rgba(255, 255, 255, 0.88);
  border: 1rpx solid rgba(255, 255, 255, 0.62);
  box-shadow: 0 18rpx 36rpx rgba(62, 39, 17, 0.08);
  backdrop-filter: blur(16rpx);
  border-radius: 28rpx;
}

.hero {
  padding: 8rpx 6rpx 18rpx;
}

.eyebrow {
  display: block;
  font-size: 22rpx;
  letter-spacing: 3rpx;
  text-transform: uppercase;
  color: #936d49;
}

.title {
  display: block;
  margin-top: 12rpx;
  font-size: 42rpx;
  line-height: 1.2;
  font-weight: 800;
  color: #2f241a;
}

.sub {
  display: block;
  margin-top: 14rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: #746554;
}

.state-card,
.passage-card,
.question-card,
.answer-card,
.coach-card,
.result-card {
  padding: 28rpx;
  margin-bottom: 18rpx;
}

.passage-head,
.input-head {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
  align-items: flex-start;
}

.article-label {
  display: inline-block;
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background: rgba(154, 90, 18, 0.1);
  color: #a16207;
  font-size: 20rpx;
  font-weight: 700;
}

.article-title {
  display: block;
  margin-top: 14rpx;
  font-size: 34rpx;
  line-height: 1.3;
  font-weight: 800;
  color: #2f241a;
}

.article-source {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  line-height: 1.6;
  color: #8a6b50;
}

.passage-progress {
  display: block;
  margin-top: 14rpx;
  font-size: 22rpx;
  color: #9a6b3a;
}

.page-pills {
  display: flex;
  gap: 10rpx;
  padding-top: 8rpx;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.page-pill {
  width: 16rpx;
  height: 16rpx;
  border-radius: 999rpx;
  background: #eadccd;
}

.page-pill.active {
  width: 38rpx;
  background: #b76c1c;
}

.passage-stage {
  margin-top: 20rpx;
  padding: 28rpx 26rpx;
  border-radius: 22rpx;
  background: linear-gradient(180deg, #fffdf8 0%, #fcf6ee 100%);
}

.passage-kicker {
  display: block;
  margin-bottom: 16rpx;
  font-size: 22rpx;
  color: #b45309;
  font-weight: 700;
}

.section-title {
  display: block;
  margin-bottom: 16rpx;
  font-size: 28rpx;
  font-weight: 700;
  color: #2f241a;
}

.passage-text,
.question-stem,
.coach-text,
.result-answer,
.result-explanation,
.option-content,
.recognized-text {
  display: block;
  font-size: 26rpx;
  line-height: 1.82;
  color: #42372c;
  white-space: pre-wrap;
  word-break: break-word;
}

.passage-text.featured {
  font-size: 32rpx;
  line-height: 1.95;
  color: #2c241c;
}

.passage-actions,
.btn-row,
.mode-tabs {
  display: flex;
  gap: 14rpx;
}

.passage-actions {
  margin-top: 20rpx;
}

.nav-btn,
.btn,
.mode-tab {
  text-align: center;
  font-size: 25rpx;
  font-weight: 700;
  border-radius: 999rpx;
}

.nav-btn {
  flex: 1;
  padding: 20rpx 0;
  background: #f2e5d4;
  color: #8a5a2b;
}

.nav-btn.primary,
.btn-primary,
.voice-button.recording {
  background: linear-gradient(135deg, #9a5a12 0%, #c67a23 100%);
  color: #fffaf1;
}

.nav-btn.disabled,
.btn.disabled,
.voice-button.disabled {
  opacity: 0.45;
}

.question-focus {
  display: block;
  margin-top: 14rpx;
  font-size: 22rpx;
  color: #a16207;
}

.option-list {
  margin-top: 20rpx;
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.option-item {
  display: flex;
  gap: 18rpx;
  align-items: flex-start;
  padding: 20rpx 22rpx;
  border-radius: 22rpx;
  background: #fcf7f0;
  border: 2rpx solid transparent;
}

.option-item.selected {
  border-color: #b76c1c;
  background: #fff3df;
}

.option-label {
  width: 42rpx;
  height: 42rpx;
  line-height: 42rpx;
  border-radius: 50%;
  text-align: center;
  background: #f0dfc5;
  color: #8a5a2b;
  font-size: 22rpx;
  font-weight: 800;
  flex-shrink: 0;
}

.input-head {
  margin-bottom: 18rpx;
}

.mode-tabs {
  flex-shrink: 0;
}

.mode-tab {
  padding: 14rpx 24rpx;
  background: #f4ede4;
  color: #85644a;
}

.mode-tab.active {
  background: #f2dcc0;
  color: #8a4f14;
}

.answer-box {
  width: 100%;
  min-height: 240rpx;
  padding: 24rpx;
  box-sizing: border-box;
  border-radius: 22rpx;
  background: #fcf8f3;
  font-size: 26rpx;
  line-height: 1.75;
  color: #2f241a;
}

.selection-hint {
  display: inline-flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 16rpx;
  padding: 12rpx 18rpx;
  border-radius: 999rpx;
  background: #f6eadb;
}

.selection-hint-voice {
  margin-bottom: 18rpx;
}

.selection-hint-label {
  font-size: 22rpx;
  color: #8a6b50;
}

.selection-hint-value {
  min-width: 38rpx;
  height: 38rpx;
  line-height: 38rpx;
  border-radius: 50%;
  text-align: center;
  background: #c67a23;
  color: #fffaf1;
  font-size: 22rpx;
  font-weight: 800;
}

.voice-panel {
  padding: 24rpx;
  border-radius: 22rpx;
  background: #fcf8f3;
}

.voice-title {
  display: block;
  font-size: 26rpx;
  font-weight: 700;
  color: #2f241a;
}

.voice-sub,
.recognized-label {
  display: block;
  margin-top: 10rpx;
  font-size: 22rpx;
  color: #8a6b50;
}

.voice-button {
  margin-top: 20rpx;
  padding: 24rpx 20rpx;
  border-radius: 24rpx;
  background: #f2e5d4;
  color: #8a5a2b;
  text-align: center;
}

.voice-button-icon {
  display: block;
  font-size: 38rpx;
  line-height: 1;
}

.voice-button-text {
  display: block;
  margin-top: 10rpx;
  font-size: 26rpx;
  font-weight: 700;
}

.voice-timer {
  margin-top: 18rpx;
}

.voice-timer-bar {
  height: 10rpx;
  border-radius: 999rpx;
  background: #e7ddcf;
  overflow: hidden;
}

.voice-timer-fill {
  height: 100%;
  background: linear-gradient(135deg, #d97706 0%, #f59e0b 100%);
}

.recognized-card {
  margin-top: 20rpx;
  padding: 20rpx 22rpx;
  border-radius: 18rpx;
  background: #fffdf8;
  border: 1rpx solid #efdfcb;
}

.voice-answer-box {
  margin-top: 18rpx;
  min-height: 180rpx;
}

.btn-row {
  margin-top: 20rpx;
}

.btn {
  width: 100%;
  padding: 22rpx 0;
}

.btn-primary {
  color: #fffaf1;
}

.btn-secondary {
  background: #f0e2d1;
  color: #8a5a2b;
}

@media (max-width: 420px) {
  .passage-head,
  .input-head {
    flex-direction: column;
  }

  .mode-tabs,
  .btn-row {
    width: 100%;
  }

  .mode-tab,
  .btn {
    flex: 1;
  }
}
</style>

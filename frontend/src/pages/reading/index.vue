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
          <view class="passage-head-copy">
            <text class="article-label">本篇材料</text>
            <text class="article-title">{{ article.title || "阅读专项" }}</text>
            <text class="article-source">{{ article.source || "阅读教练式训练" }}</text>
          </view>
        </view>

        <view class="passage-stage">
          <view class="selection-head">
            <view>
              <text class="selection-sub">翻译：短按选词 5 次机会，长按单词选短句 3 次机会</text>
            </view>
          </view>

          <view
            class="paragraph-gesture-layer"
            :class="paragraphAnimationClass"
            @touchstart="onParagraphTouchStart"
            @touchend="onParagraphTouchEnd"
          >
            <view class="passage-inline-wrap featured">
              <view
                v-for="(sentence, sentenceIdx) in getSentenceTokens(paragraphs[currentParagraphIndex] || '')"
                :key="`${currentParagraphIndex}-${sentenceIdx}-${sentence.text}`"
                class="inline-sentence-row"
              >
                <view
                  class="inline-sentence"
                  :class="{ selected: activeSelection?.type === 'sentence' && activeSelection?.text === sentence.text }"
                >
                  <view
                    v-for="(token, tokenIdx) in sentence.tokens"
                    :key="`${currentParagraphIndex}-${sentenceIdx}-${tokenIdx}-${token.raw}`"
                    class="inline-fragment"
                    :class="{ selected: token.value && activeSelection?.type === 'word' && activeSelection?.text === token.value }"
                    @tap="token.value && chooseSelection('word', token.value)"
                    @longpress="token.value && chooseSelection('sentence', sentence.text)"
                  >
                    <text>{{ token.raw }} </text>
                  </view>
                </view>
              </view>
            </view>
          </view>

          <view class="reading-meta-row">
            <text class="passage-progress">第 {{ currentParagraphIndex + 1 }} / {{ paragraphs.length || 1 }} 段</text>
            <view class="page-pills">
              <view
                v-for="(_, idx) in paragraphs"
                :key="idx"
                class="page-pill"
                :class="{ active: idx === currentParagraphIndex }"
                @click="jumpParagraph(idx)"
              />
            </view>
          </view>
        </view>

        <view v-if="activeSelection" class="selection-action-bar">
          <view class="selection-preview">
            <text class="selection-preview-tag">{{ activeSelection.type === 'word' ? '单词' : '短句' }}</text>
            <text class="selection-preview-text">{{ activeSelection.text }}</text>
          </view>
          <view class="selection-buttons">
            <view
              class="selection-btn translate"
              :class="{ disabled: !canTranslateSelection || translating }"
              @click="translateSelection"
            >
              <text>{{ translating ? '翻译中...' : '翻译' }}</text>
            </view>
            <view
              class="selection-btn collect"
              :class="{ disabled: addingMistake }"
              @click="addCurrentSelectionToMistake"
            >
              <text>{{ addingMistake ? '加入中...' : '加入误解本' }}</text>
            </view>
          </view>
        </view>

        <view v-if="selectionTranslation" class="translation-result">
          <text class="translation-label">翻译结果</text>
          <text class="translation-text">{{ selectionTranslation }}</text>
        </view>

        <view v-if="mistakeSuccessText" class="success-banner">
          <text class="success-icon">✓</text>
          <text class="success-text">{{ mistakeSuccessText }}</text>
        </view>

        <view class="passage-actions">
          <view class="nav-btn" :class="{ disabled: currentParagraphIndex === 0 }" @click="goPrevParagraph">
            <text>上一段</text>
          </view>
          <view class="nav-btn primary" :class="{ disabled: currentParagraphIndex === paragraphs.length - 1 }" @click="goNextParagraph">
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
            <view class="mode-tab" :class="{ active: inputMode === 'text' }" @click="switchInputMode('text')">
              <text>文字输入</text>
            </view>
            <view class="mode-tab" :class="{ active: inputMode === 'voice' }" @click="switchInputMode('voice')">
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

            <view class="voice-button" :class="{ recording: isRecording, disabled: uploading || submitting }" @click="toggleVoiceRecording">
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
type SelectionType = "word" | "sentence";

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
  readingSessionId: string;
  wordTranslationLimit: number;
  wordTranslationUsed: number;
  wordTranslationRemaining: number;
  sentenceTranslationLimit: number;
  sentenceTranslationUsed: number;
  sentenceTranslationRemaining: number;
  questions: QuestionItem[];
}

interface CoachResponse {
  coachReply: string;
  revealAnswer: boolean;
  answer: string;
  explanation: string;
  turn: number;
}

interface TranslationResponse {
  translatedText: string;
  contentType: SelectionType;
  limit: number;
  usedCount: number;
  remainingCount: number;
}

interface SelectionState {
  type: SelectionType;
  text: string;
}

interface WordToken {
  raw: string;
  value: string;
}

interface SentenceTokenGroup {
  text: string;
  tokens: WordToken[];
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
const translating = ref(false);
const addingMistake = ref(false);
const article = ref<ArticleData>({
  articleId: "",
  source: "",
  title: "",
  passage: "",
  readingSessionId: "",
  wordTranslationLimit: 5,
  wordTranslationUsed: 0,
  wordTranslationRemaining: 5,
  sentenceTranslationLimit: 3,
  sentenceTranslationUsed: 0,
  sentenceTranslationRemaining: 3,
  questions: [],
});
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
const activeSelection = ref<SelectionState | null>(null);
const selectionTranslation = ref("");
const mistakeSuccessText = ref("");
const paragraphTouchStartX = ref(0);
const paragraphAnimationClass = ref("");

const paragraphs = computed(() => {
  if (!article.value.passage) return [];
  return article.value.passage
    .split(/\n\s*\n/)
    .map((item) => item.replace(/\s*\n\s*/g, " ").replace(/\s+/g, " ").trim())
    .filter(Boolean);
});

const currentQuestion = computed<QuestionItem>(() => {
  return article.value.questions[currentIdx.value] || emptyQuestion;
});

const voiceProgress = computed(() => (voiceCountdown.value / VOICE_LIMIT_SECONDS) * 100);

const canSubmit = computed(() => {
  if (revealed.value) return true;
  return (!!answerText.value.trim() || !!selectedOption.value) && !submitting.value && !uploading.value;
});

const canTranslateSelection = computed(() => {
  if (!activeSelection.value) return false;
  return activeSelection.value.type === "sentence"
    ? article.value.sentenceTranslationRemaining > 0
    : article.value.wordTranslationRemaining > 0;
});

function splitParagraphIntoSentences(paragraph: string) {
  return (paragraph.match(/[^.!?]+[.!?]+["']?|[^.!?]+$/g) || [])
    .map((item) => item.trim())
    .filter(Boolean);
}

function getSentenceTokens(paragraph: string): SentenceTokenGroup[] {
  return splitParagraphIntoSentences(paragraph).map((sentence) => ({
    text: sentence,
    tokens: tokenizeParagraph(sentence),
  }));
}

function tokenizeParagraph(paragraph: string): WordToken[] {
  return paragraph
    .split(/\s+/)
    .map((item) => item.trim())
    .filter(Boolean)
    .map((item) => ({
      raw: item,
      value: item.replace(/^[^A-Za-z]+|[^A-Za-z'-]+$/g, ""),
    }));
}

function resetSelection() {
  activeSelection.value = null;
  selectionTranslation.value = "";
}

function clearMistakeSuccess() {
  mistakeSuccessText.value = "";
}

function playParagraphAnimation(direction: "next" | "prev") {
  paragraphAnimationClass.value = direction === "next" ? "slide-next" : "slide-prev";
  setTimeout(() => {
    paragraphAnimationClass.value = "";
  }, 220);
}

async function loadArticle() {
  if (!userId.value) return;
  const res = await get<ArticleData>("/reading/article", { userId: userId.value });
  if (res.data) {
    article.value = res.data;
    currentParagraphIndex.value = 0;
    resetSelection();
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

function chooseSelection(type: SelectionType, text: string) {
  clearMistakeSuccess();
  if (activeSelection.value?.type === type && activeSelection.value.text === text) {
    activeSelection.value = null;
    selectionTranslation.value = "";
    return;
  }
  activeSelection.value = { type, text };
  selectionTranslation.value = "";
}

async function translateSelection() {
  if (!activeSelection.value || !userId.value) return;
  if (!canTranslateSelection.value) {
    uni.showToast({
      title: activeSelection.value.type === "sentence" ? "每篇短句翻译最多3次哦～" : "每篇最多5次翻译机会哦～",
      icon: "none",
    });
    return;
  }
  translating.value = true;
  try {
    const res = await post<TranslationResponse>("/reading/translate", {
      userId: userId.value,
      articleId: article.value.articleId,
      readingSessionId: article.value.readingSessionId,
      contentType: activeSelection.value.type,
      sourceText: activeSelection.value.text,
    });
    if (res.data) {
      selectionTranslation.value = res.data.translatedText;
      if (res.data.contentType === "sentence") {
        article.value.sentenceTranslationLimit = res.data.limit;
        article.value.sentenceTranslationUsed = res.data.usedCount;
        article.value.sentenceTranslationRemaining = res.data.remainingCount;
      } else {
        article.value.wordTranslationLimit = res.data.limit;
        article.value.wordTranslationUsed = res.data.usedCount;
        article.value.wordTranslationRemaining = res.data.remainingCount;
      }
    }
  } finally {
    translating.value = false;
  }
}

async function addCurrentSelectionToMistake() {
  if (!activeSelection.value || !userId.value) return;
  addingMistake.value = true;
  try {
    await post("/mistakes", {
      userId: userId.value,
      type: activeSelection.value.type,
      sourceText: activeSelection.value.text,
      translation: selectionTranslation.value || undefined,
      sourceModule: "阅读",
      articleId: article.value.articleId,
    });
    mistakeSuccessText.value = `${activeSelection.value.type === "word" ? "单词" : "短句"}已经加入误解本`;
  } finally {
    addingMistake.value = false;
  }
}

function onParagraphTouchStart(event: any) {
  paragraphTouchStartX.value = event.changedTouches?.[0]?.clientX || 0;
}

function onParagraphTouchEnd(event: any) {
  const endX = event.changedTouches?.[0]?.clientX || 0;
  const deltaX = endX - paragraphTouchStartX.value;
  if (deltaX <= -50) {
    goNextParagraph();
    return;
  }
  if (deltaX >= 50) {
    goPrevParagraph();
  }
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
      selectedOption: selectedOption.value || undefined,
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
  resetSelection();
  syncRecentTraining();
}

function jumpParagraph(idx: number) {
  if (idx === currentParagraphIndex.value) return;
  playParagraphAnimation(idx > currentParagraphIndex.value ? "next" : "prev");
  currentParagraphIndex.value = idx;
  resetSelection();
  clearMistakeSuccess();
}

function goPrevParagraph() {
  if (currentParagraphIndex.value === 0) return;
  playParagraphAnimation("prev");
  currentParagraphIndex.value -= 1;
  resetSelection();
  clearMistakeSuccess();
}

function goNextParagraph() {
  if (currentParagraphIndex.value >= paragraphs.value.length - 1) return;
  playParagraphAnimation("next");
  currentParagraphIndex.value += 1;
  resetSelection();
  clearMistakeSuccess();
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
.input-head,
.selection-head,
.selection-action-bar {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
  align-items: flex-start;
}

.passage-head-copy {
  flex: 1;
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

.section-title {
  display: block;
  margin-top: 14rpx;
  margin-bottom: 16rpx;
  font-size: 28rpx;
  font-weight: 700;
  color: #2f241a;
}

.passage-progress {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #9a6b3a;
}

.page-pills {
  display: flex;
  gap: 10rpx;
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

.translation-label,
.selection-title {
  font-size: 22rpx;
  color: #b45309;
  font-weight: 700;
}

.passage-text,
.question-stem,
.coach-text,
.result-answer,
.result-explanation,
.option-content,
.recognized-text,
.selection-preview-text,
.translation-text {
  display: block;
  font-size: 26rpx;
  line-height: 1.82;
  color: #42372c;
  white-space: pre-wrap;
  word-break: break-word;
}

.passage-text.featured,
.passage-inline-wrap.featured {
  margin-top: 16rpx;
  font-size: 32rpx;
  line-height: 1.95;
  color: #2c241c;
}

.selection-sub {
  display: block;
  font-size: 22rpx;
  color: #8a6b50;
}

.mode-tabs,
.passage-actions,
.btn-row,
.selection-buttons {
  display: flex;
  gap: 14rpx;
}

.mode-tab,
.nav-btn,
.btn,
.selection-btn {
  text-align: center;
  font-size: 24rpx;
  font-weight: 700;
  border-radius: 999rpx;
}

.mode-tab {
  padding: 14rpx 22rpx;
  background: #f4ede4;
  color: #85644a;
}

.mode-tab.active {
  background: #f2dcc0;
  color: #8a4f14;
}

.passage-inline-wrap {
  display: block;
  margin-top: 18rpx;
  width: 100%;
}

.paragraph-gesture-layer {
  margin-top: 18rpx;
  width: 100%;
}

.paragraph-gesture-layer.slide-next {
  animation: paragraph-slide-next 220ms ease;
}

.paragraph-gesture-layer.slide-prev {
  animation: paragraph-slide-prev 220ms ease;
}

@keyframes paragraph-slide-next {
  from {
    opacity: 0.28;
    transform: translateX(28rpx);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes paragraph-slide-prev {
  from {
    opacity: 0.28;
    transform: translateX(-28rpx);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.inline-sentence-row {
  display: block;
  width: 100%;
  margin-bottom: 12rpx;
}

.inline-fragment {
  display: inline-flex;
  max-width: 100%;
  padding: 2rpx 4rpx;
  border-radius: 10rpx;
  box-sizing: border-box;
}

.inline-sentence {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  width: 100%;
  padding: 4rpx 2rpx;
  border-radius: 12rpx;
  box-sizing: border-box;
  font-size: 32rpx;
  line-height: 1.95;
  color: #2c241c;
}

.inline-fragment.selected,
.inline-sentence.selected {
  background: rgba(198, 122, 35, 0.18);
  color: #8a4f14;
}

.selection-action-bar {
  margin-top: 18rpx;
  padding: 18rpx 20rpx;
  border-radius: 20rpx;
  background: #fff4e7;
  align-items: center;
}

.selection-preview {
  flex: 1;
}

.selection-preview-tag {
  display: inline-block;
  padding: 6rpx 12rpx;
  border-radius: 999rpx;
  background: #f0dfc5;
  color: #8a5a2b;
  font-size: 20rpx;
  font-weight: 700;
}

.selection-preview-text {
  margin-top: 10rpx;
}

.selection-btn {
  min-width: 180rpx;
  padding: 20rpx 0;
}

.selection-btn.translate {
  background: linear-gradient(135deg, #9a5a12 0%, #c67a23 100%);
  color: #fffaf1;
}

.selection-btn.collect {
  background: #ead8c0;
  color: #7b4e24;
}

.selection-btn.disabled,
.nav-btn.disabled,
.btn.disabled,
.voice-button.disabled {
  opacity: 0.45;
}

.translation-result {
  margin-top: 18rpx;
  padding: 18rpx 20rpx;
  border-radius: 20rpx;
  background: #fffdf8;
  border: 1rpx solid #efdfcb;
}

.reading-meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-top: 18rpx;
}

.success-banner {
  margin-top: 18rpx;
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding: 18rpx 20rpx;
  border-radius: 20rpx;
  background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%);
  border: 1rpx solid rgba(34, 197, 94, 0.22);
}

.success-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40rpx;
  height: 40rpx;
  border-radius: 999rpx;
  background: #16a34a;
  color: #ffffff;
  font-size: 24rpx;
  font-weight: 800;
}

.success-text {
  font-size: 24rpx;
  color: #166534;
  font-weight: 700;
}

.passage-actions {
  margin-top: 20rpx;
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

.option-label,
.selection-hint-value {
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

.selection-hint-label,
.voice-sub,
.recognized-label {
  font-size: 22rpx;
  color: #8a6b50;
}

.selection-hint-value {
  background: #c67a23;
  color: #fffaf1;
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

@media (max-width: 420px) {
  .passage-head,
  .input-head,
  .selection-head,
  .selection-action-bar {
    flex-direction: column;
  }

  .mode-tabs,
  .btn-row,
  .selection-buttons {
    width: 100%;
  }

  .mode-tab,
  .selection-btn,
  .btn {
    flex: 1;
  }
}
</style>

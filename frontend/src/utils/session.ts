/**
 * 本地会话管理
 */
const KEYS = {
  TOKEN: "kaoyan_token",
  USER_ID: "kaoyan_user_id",
  ONBOARDING_DONE: "kaoyan_onboarding_done",
};

export function saveSession(userId: number, token?: string, onboardingDone = true) {
  uni.setStorageSync(KEYS.USER_ID, userId);
  if (token) {
    uni.setStorageSync(KEYS.TOKEN, token);
  }
  uni.setStorageSync(KEYS.ONBOARDING_DONE, onboardingDone);
}

export function saveToken(token: string) {
  uni.setStorageSync(KEYS.TOKEN, token);
}

export function getToken(): string {
  return uni.getStorageSync(KEYS.TOKEN) || "";
}

export function getUserId(): number | null {
  return uni.getStorageSync(KEYS.USER_ID) || null;
}

export function isOnboardingDone(): boolean {
  return !!uni.getStorageSync(KEYS.ONBOARDING_DONE);
}

export function clearSession() {
  uni.removeStorageSync(KEYS.TOKEN);
  uni.removeStorageSync(KEYS.USER_ID);
  uni.removeStorageSync(KEYS.ONBOARDING_DONE);
}

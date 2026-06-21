/**
 * 本地会话管理（M5 微信登录前临时方案）
 */
const KEYS = {
  USER_ID: "kaoyan_user_id",
  ONBOARDING_DONE: "kaoyan_onboarding_done",
};

export function saveSession(userId: number) {
  uni.setStorageSync(KEYS.USER_ID, userId);
  uni.setStorageSync(KEYS.ONBOARDING_DONE, true);
}

export function getUserId(): number | null {
  return uni.getStorageSync(KEYS.USER_ID) || null;
}

export function isOnboardingDone(): boolean {
  return !!uni.getStorageSync(KEYS.ONBOARDING_DONE);
}

export function clearSession() {
  uni.removeStorageSync(KEYS.USER_ID);
  uni.removeStorageSync(KEYS.ONBOARDING_DONE);
}

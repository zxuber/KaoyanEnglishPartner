import { post } from "@/utils/request";
import { getToken, saveSession } from "@/utils/session";

interface LoginResponse {
  token: string;
  userId: number;
  onboardingDone: boolean;
}

let pendingLogin: Promise<LoginResponse> | null = null;

function uniLogin(): Promise<string> {
  return new Promise((resolve, reject) => {
    uni.login({
      provider: "weixin",
      success: (res) => {
        if (res.code) {
          resolve(res.code);
          return;
        }
        reject(new Error("未获取到微信登录 code"));
      },
      fail: reject,
    });
  });
}

export async function ensureAuthed(force = false): Promise<LoginResponse> {
  if (!force && getToken()) {
    return {
      token: getToken(),
      userId: uni.getStorageSync("kaoyan_user_id"),
      onboardingDone: !!uni.getStorageSync("kaoyan_onboarding_done"),
    };
  }

  if (!pendingLogin) {
    pendingLogin = (async () => {
      const code = await uniLogin();
      const res = await post<LoginResponse>("/auth/wx-login", { code });
      if (!res.data) {
        throw new Error("登录失败");
      }
      saveSession(res.data.userId, res.data.token, res.data.onboardingDone);
      return res.data;
    })().finally(() => {
      pendingLogin = null;
    });
  }

  return pendingLogin;
}

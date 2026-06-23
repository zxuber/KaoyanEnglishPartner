import { post } from "@/utils/request";
import { getApiRuntimeSummary } from "@/config/api";
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
        console.log("[AUTH] uni.login success", {
          runtime: getApiRuntimeSummary(),
          hasCode: !!res.code,
          codePreview: res.code ? `${res.code.slice(0, 8)}...` : "",
        });
        if (res.code) {
          resolve(res.code);
          return;
        }
        reject(new Error("未获取到微信登录 code"));
      },
      fail: (error) => {
        console.log("[AUTH] uni.login fail", {
          runtime: getApiRuntimeSummary(),
          error,
        });
        reject(error);
      },
    });
  });
}

export async function ensureAuthed(force = false): Promise<LoginResponse> {
  console.log("[AUTH] ensureAuthed enter", {
    force,
    hasToken: !!getToken(),
    runtime: getApiRuntimeSummary(),
  });
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
      console.log("[AUTH] calling /auth/wx-login", {
        runtime: getApiRuntimeSummary(),
      });
      const res = await post<LoginResponse>("/auth/wx-login", { code });
      console.log("[AUTH] /auth/wx-login response", {
        runtime: getApiRuntimeSummary(),
        response: res,
      });
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

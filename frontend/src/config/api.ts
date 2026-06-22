type ApiMode = "local" | "lan" | "tunnel";

const API_MODES: ApiMode[] = ["local", "lan", "tunnel"];
const STORAGE_KEY = "apiBaseUrlOverride";

function normalizeBaseUrl(url: string): string {
  return url.replace(/\/+$/, "");
}

function getMode(): ApiMode {
  const rawMode = (import.meta.env.VITE_API_MODE || "local").toLowerCase() as ApiMode;
  return API_MODES.includes(rawMode) ? rawMode : "local";
}

function getConfiguredBaseUrl(mode: ApiMode): string {
  const envMap: Record<ApiMode, string | undefined> = {
    local: import.meta.env.VITE_API_BASE_URL_LOCAL,
    lan: import.meta.env.VITE_API_BASE_URL_LAN,
    tunnel: import.meta.env.VITE_API_BASE_URL_TUNNEL,
  };
  const rawUrl = envMap[mode] || envMap.local || "http://localhost:8080/api/v1";
  return normalizeBaseUrl(rawUrl);
}

export function getApiBaseUrl(): string {
  const override = uni.getStorageSync(STORAGE_KEY);
  if (override && typeof override === "string") {
    return normalizeBaseUrl(override);
  }
  return getConfiguredBaseUrl(getMode());
}

export function getSpeechApiUrl(): string {
  return `${getApiBaseUrl()}/speech/recognize`;
}

export function setApiBaseUrlOverride(url: string) {
  uni.setStorageSync(STORAGE_KEY, normalizeBaseUrl(url));
}

export function clearApiBaseUrlOverride() {
  uni.removeStorageSync(STORAGE_KEY);
}

export function getApiRuntimeSummary() {
  return {
    mode: getMode(),
    baseUrl: getApiBaseUrl(),
  };
}

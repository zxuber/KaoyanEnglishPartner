type ApiMode = "local" | "lan" | "tunnel";

const API_MODES: ApiMode[] = ["local", "lan", "tunnel"];
const STORAGE_KEY = "apiBaseUrlOverride";
const ALLOW_OVERRIDE = import.meta.env.VITE_API_ALLOW_OVERRIDE === "true";
const BUILD_MARKER = "LAN_BUILD_2026_06_23_2035";

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
  console.log("[API_CONFIG]", {
    marker: BUILD_MARKER,
    envMode: import.meta.env.VITE_API_MODE,
    allowOverride: ALLOW_OVERRIDE,
    override,
    local: import.meta.env.VITE_API_BASE_URL_LOCAL,
    lan: import.meta.env.VITE_API_BASE_URL_LAN,
    tunnel: import.meta.env.VITE_API_BASE_URL_TUNNEL,
  });
  if (!ALLOW_OVERRIDE && override) {
    console.log("[API_CONFIG] clearing stale override", { marker: BUILD_MARKER, override });
    uni.removeStorageSync(STORAGE_KEY);
  }
  if (ALLOW_OVERRIDE && override && typeof override === "string") {
    const finalUrl = normalizeBaseUrl(override);
    console.log("[API_CONFIG] using override url", { marker: BUILD_MARKER, finalUrl });
    return finalUrl;
  }
  const mode = getMode();
  const finalUrl = getConfiguredBaseUrl(mode);
  console.log("[API_CONFIG] using env url", { marker: BUILD_MARKER, mode, finalUrl });
  return finalUrl;
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
    marker: BUILD_MARKER,
    mode: getMode(),
    baseUrl: getApiBaseUrl(),
  };
}

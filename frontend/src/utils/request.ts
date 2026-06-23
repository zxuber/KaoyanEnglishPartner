/**
 * 网络请求封装 - 基于 uni.request
 * 统一管理 token、错误处理、超时
 */

import { getApiBaseUrl } from "@/config/api";
import { getToken, clearSession } from "@/utils/session";

const TIMEOUT = 15000;

interface RequestOptions {
  url: string;
  method?: "GET" | "POST" | "PUT" | "DELETE";
  data?: Record<string, unknown>;
  header?: Record<string, string>;
}

interface ApiResponse<T = unknown> {
  code: number;
  message: string;
  data: T;
}

function request<T = unknown>(options: RequestOptions): Promise<ApiResponse<T>> {
  const token = getToken();
  const finalUrl = getApiBaseUrl() + options.url;

  console.log("[API_REQUEST:start]", {
    url: finalUrl,
    path: options.url,
    method: options.method || "GET",
    hasToken: !!token,
  });

  return new Promise((resolve, reject) => {
    uni.request({
      url: finalUrl,
      method: options.method || "GET",
      data: options.data,
      timeout: TIMEOUT,
      header: {
        "Content-Type": "application/json",
        Authorization: token ? `Bearer ${token}` : "",
        ...options.header,
      },
      success(res) {
        const { statusCode, data } = res;
        console.log("[API_REQUEST:success]", {
          url: finalUrl,
          statusCode,
          data,
        });
        if (statusCode === 200) {
          resolve(data as ApiResponse<T>);
        } else if (statusCode === 401) {
          clearSession();
          uni.reLaunch({ url: "/pages/home/index" });
          reject(new Error("登录已过期"));
        } else {
          const msg = (data as ApiResponse).message || "请求失败";
          uni.showToast({ title: msg, icon: "none" });
          reject(new Error(msg));
        }
      },
      fail(err) {
        console.log("[API_REQUEST:fail]", {
          url: finalUrl,
          error: err,
        });
        uni.showToast({ title: "网络开小差了，请重试", icon: "none" });
        reject(err);
      },
    });
  });
}

export function get<T = unknown>(url: string, data?: Record<string, unknown>) {
  return request<T>({ url, method: "GET", data });
}

export function post<T = unknown>(url: string, data?: Record<string, unknown>) {
  return request<T>({ url, method: "POST", data });
}

export function put<T = unknown>(url: string, data?: Record<string, unknown>) {
  return request<T>({ url, method: "PUT", data });
}

export function patch<T = unknown>(url: string, data?: Record<string, unknown>) {
  return request<T>({
    url,
    method: "POST",
    data: { ...data, _method: "PATCH" },
    header: { "X-HTTP-Method-Override": "PATCH" },
  });
}

export function del<T = unknown>(url: string, data?: Record<string, unknown>) {
  return request<T>({ url, method: "DELETE", data });
}

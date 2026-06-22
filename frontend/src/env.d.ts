/// <reference types="vite/client" />
/// <reference types="@dcloudio/types" />

declare module "*.vue" {
  import { DefineComponent } from "vue";
  const component: DefineComponent<{}, {}, any>;
  export default component;
}

declare module "uview-plus" {
  const uviewPlus: any;
  export default uviewPlus;
}

interface ImportMetaEnv {
  readonly VITE_API_MODE?: "local" | "lan" | "tunnel";
  readonly VITE_API_BASE_URL_LOCAL?: string;
  readonly VITE_API_BASE_URL_LAN?: string;
  readonly VITE_API_BASE_URL_TUNNEL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

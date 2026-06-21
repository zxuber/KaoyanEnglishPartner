import { createSSRApp } from "vue";
import App from "./App.vue";
import uviewPlus from "uview-plus";

export function createApp() {
  const app = createSSRApp(App);

  // uView Plus UI component library
  app.use(uviewPlus);

  return {
    app,
  };
}

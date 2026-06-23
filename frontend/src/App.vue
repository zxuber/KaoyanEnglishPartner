<script setup lang="ts">
import { onLaunch } from "@dcloudio/uni-app";
import { getApiRuntimeSummary } from "@/config/api";

onLaunch(() => {
  console.log("[APP_BOOT]", {
    marker: "LAN_BUILD_2026_06_23_2035",
    runtime: getApiRuntimeSummary(),
  });
  // 不做任何跳页操作，交给各页面自己判断
});
</script>

<style lang="scss">
@import "uview-plus/index.scss";

page {
  background-color: #f5f5f5;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}
</style>

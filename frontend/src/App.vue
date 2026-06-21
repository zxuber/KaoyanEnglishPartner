<script setup lang="ts">
import { onLaunch } from "@dcloudio/uni-app";

onLaunch(() => {
  console.log("考研英语陪跑 App launched");
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

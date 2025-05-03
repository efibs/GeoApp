<template>
  <div class="section-header q-my-sm">
    <div class="line" />
    <div class="label q-px-sm text-caption text-weight-medium text-grey-5">
      {{ label }}
    </div>
    <div class="line" />
  </div>
</template>

<script setup lang="ts">
defineProps({ label: String });
</script>

<style scoped>
.section-header {
  display: flex;
  align-items: center;
  margin: 32px 0 0 16px;
}
.line {
  flex: 1;
  height: 1px;
  background-color: #aaa;
  opacity: 0.3;
}
</style>

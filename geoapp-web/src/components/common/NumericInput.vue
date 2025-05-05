<template>
  <div class="q-pa-md container">
    <q-badge>{{ label }}: {{ value }} {{ unit ?? '' }}</q-badge>
    <q-slider
      class="slider"
      v-model="value"
      snap
      :min="min"
      :max="max"
      :step="step"
      label
      marker-labels
      markers
    />
  </div>
</template>
<script lang="ts" setup>
const value = defineModel<number>();
const { label, min, max, step, unit } = defineProps<{
  label: string;
  unit?: string;
  min: number;
  max: number;
  step: number;
}>();
</script>
<style scoped>
.container {
  padding: 0;
}

.slider {
  max-width: 100%;
  margin: 0;
  padding: 0 8px 0 8px;
}
</style>

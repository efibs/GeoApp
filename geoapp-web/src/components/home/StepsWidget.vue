<template>
  <q-card>
    <q-card-section>
      <div class="text-h6">Steps</div>
    </q-card-section>
    <q-card-section>
      <VueApexCharts
        :width="400"
        :type="'line'"
        :options="chartOptions"
        :series="chartSeries"
      ></VueApexCharts>
    </q-card-section>
  </q-card>
</template>
<script lang="ts" setup>
import VueApexCharts from 'vue3-apexcharts';
import { type Datapoint } from '../models';
import { computed } from 'vue';

const { stepData } = defineProps<{ stepData: Datapoint[] }>();

const differences = computed(() => {
  const result = [];
  for (let i = 1; i < stepData.length; i++) {
    result.push((stepData[i]?.steps ?? 0) - (stepData[i - 1]?.steps ?? 0));
  }
  return result;
});

const chartSeries = computed(() => [
  {
    name: 'steps',
    data: differences.value,
  },
]);

const chartOptions = computed(() => {
  return {
    chart: {
      id: 'steps-chart',
    },
    xaxis: {
      categories: stepData.map((d) => new Date(d.timestamp).toLocaleTimeString()),
    },
    tooltip: {
      theme: 'dark',
    },
    stroke: {
      width: 2,
      lineCap: 'round',
      curve: 'monotoneCubic',
    },
    noData: {
      text: 'No Data Available',
      style: {
        color: 'red',
      },
    },
  };
});
</script>
<style scoped>
:deep(.apexcharts-menu) {
  background: #2c2c2c !important;
  color: #ffffff !important;
}

:deep(.apexcharts-menu-item:hover) {
  background-color: #444 !important;
}
</style>

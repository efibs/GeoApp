<template>
  <q-card>
    <q-card-section>
      <div class="text-h6">Total steps</div>
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

const chartSeries = computed(() => [
  {
    name: 'total-steps',
    data: stepData.map((d) => d.steps),
  },
]);

const chartOptions = computed(() => {
  return {
    chart: {
      id: 'total-steps-chart',
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

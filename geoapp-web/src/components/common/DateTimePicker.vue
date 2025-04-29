<template>
  <div class="q-pa-md" style="max-width: 300px">
    <q-input filled v-model="dateStr" :label="label">
      <template v-slot:prepend>
        <q-icon name="event" class="cursor-pointer">
          <q-popup-proxy cover transition-show="scale" transition-hide="scale">
            <q-date v-model="dateStr" mask="YYYY-MM-DD HH:mm" minimal>
              <div class="row items-center justify-end">
                <q-btn v-close-popup label="Close" color="primary" flat />
              </div>
            </q-date>
          </q-popup-proxy>
        </q-icon>
      </template>

      <template v-slot:append>
        <q-icon name="access_time" class="cursor-pointer">
          <q-popup-proxy cover transition-show="scale" transition-hide="scale">
            <q-time v-model="dateStr" mask="YYYY-MM-DD HH:mm" format24h>
              <div class="row items-center justify-end">
                <q-btn v-close-popup label="Close" color="primary" flat />
              </div>
            </q-time>
          </q-popup-proxy>
        </q-icon>
      </template>
    </q-input>
  </div>
</template>
<script lang="ts" setup>
import { date } from 'quasar';
import { computed } from 'vue';

const dateModel = defineModel<Date | undefined>();

const { label } = defineProps<{ label: string }>();

const dateStr = computed({
  get() {
    if (!dateModel.value) {
      return null;
    }

    return date.formatDate(dateModel.value, 'YYYY-MM-DD HH:mm');
  },
  set(newValue) {
    if (!newValue) {
      return;
    }

    dateModel.value = new Date(newValue);
  },
});
</script>

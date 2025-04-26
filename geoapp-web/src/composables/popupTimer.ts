import { useTimeoutFn } from '@vueuse/core';
import { ref } from 'vue';

export function usePopupTimer(duration: number) {
  const popupShowing = ref(false);

  const { start, isPending } = useTimeoutFn(
    () => {
      popupShowing.value = false;
    },
    duration,
    { immediate: false },
  );

  const showPopup = () => {
    if (isPending.value) {
      return;
    }

    popupShowing.value = true;

    start();
  };

  return { popupShowing, showPopup };
}

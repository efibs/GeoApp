<template>
  <div style="height: 100vh">
    <q-card class="register-card" flat bordered>
      <q-card-section>
        <div class="text-h6">Register</div>
      </q-card-section>

      <q-card-section>
        <q-form @submit="register">
          <q-input label="username" v-model="username" :rules="usernameRules"></q-input>

          <q-input
            v-model="password"
            :type="isPwd ? 'password' : 'text'"
            label="password"
            :rules="passwordRules"
          >
            <template v-slot:append>
              <q-icon
                :name="isPwd ? 'visibility_off' : 'visibility'"
                class="cursor-pointer"
                @click="isPwd = !isPwd"
              />
            </template>
          </q-input>

          <q-input
            v-model="passwordRepeat"
            :type="isPwd ? 'password' : 'text'"
            label="repeat password"
            :rules="passwordRepeatRules"
            :lazy-rules="'ondemand'"
          >
          </q-input>

          <q-btn color="primary" type="submit" class="full-width register-button">Register</q-btn>
        </q-form>
      </q-card-section>
    </q-card>

    <div class="q-pa-md q-gutter-sm">
      <q-banner v-if="errorShowing" class="text-white bg-red">
        Register failed: {{ errorMessage }}
      </q-banner>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { type ValidationRule } from 'quasar';
import type { ValidationErorr } from 'src/components/models';
import { type Register } from 'src/components/models';
import { ref } from 'vue';
import { useAuthStore } from 'src/stores/authStore';
import { usePopupTimer } from 'src/composables/popupTimer';
import { useRouter } from 'vue-router';
import { AxiosError } from 'axios';

const router = useRouter();
const authStore = useAuthStore();
const { popupShowing: errorShowing, showPopup: showError } = usePopupTimer(10000);

const username = ref();
const password = ref();
const passwordRepeat = ref();
const isPwd = ref(true);
const errorMessage = ref<string>();

const usernameRules: ValidationRule[] = [
  (val) => (val && val.length > 0) || 'Username cannot be empty.',
];
const passwordRules: ValidationRule[] = [
  (val) => (val && val.length > 0) || 'Password cannot be empty.',
];
const passwordRepeatRules: ValidationRule[] = [
  (val) => (val && val.length > 0) || 'Password cannot be empty.',
  () => password.value == passwordRepeat.value || 'Password not the same.',
];

const register = async () => {
  const register: Register = {
    username: username.value,
    password: password.value,
  };

  try {
    await authStore.registerAsync(register);
    await router.push('/');
  } catch (error) {
    console.error(error);
    if (error instanceof AxiosError && error.response?.status == 400) {
      const valErrors: ValidationErorr[] = error.response?.data;

      errorMessage.value = valErrors.map((e) => e.description).join(' ');
    } else {
      errorMessage.value = 'Unknown error';
    }
    showError();
  }
};
</script>

<style scoped>
.full-width {
  width: 100%;
}

.register-card {
  width: 300px;
  margin: 0;
  position: absolute;
  top: 50%;
  left: 50%;
  -ms-transform: translate(-50%, -50%);
  transform: translate(-50%, -50%);
  filter: drop-shadow(7px 7px 5px rgba(0, 0, 0, 0.6));
}

.register-button {
  margin-top: 40px;
}
</style>

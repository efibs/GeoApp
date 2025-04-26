<template>
  <div style="height: 100vh">
    <q-card class="login-card" flat bordered>
      <q-card-section>
        <div class="text-h6">Login</div>
      </q-card-section>

      <q-card-section>
        <q-form @submit="login">
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

          <q-btn color="primary" type="submit" class="full-width login-button">Login</q-btn>
        </q-form>
      </q-card-section>
    </q-card>

    <div class="q-pa-md q-gutter-sm">
      <q-banner v-if="!noError" class="text-white bg-red"> Login failed </q-banner>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { type ValidationRule } from 'quasar';
import { type Login } from 'src/components/models';
import { ref } from 'vue';
import { useAuthStore } from 'src/stores/authStore';
import { useTimeout } from '@vueuse/core';
import { api } from 'boot/axios';
import { useRouter } from 'vue-router';

const router = useRouter();
const authStore = useAuthStore();
const { ready: noError, start } = useTimeout(3000, { controls: true });

const username = ref();
const password = ref();
const isPwd = ref(true);

const usernameRules: ValidationRule[] = [
  (val) => (val && val.length > 0) || 'Username cannot be empty.',
];
const passwordRules: ValidationRule[] = [
  (val) => (val && val.length > 0) || 'Password cannot be empty.',
];

const login = async () => {
  const login: Login = {
    username: username.value,
    password: password.value,
  };

  try {
    const response = await api.post('/users/login', login);

    authStore.tokenString = response.data.token;

    await router.push('/');
  } catch (error) {
    console.error(error);
    if (noError) {
      start();
    }
  }
};
</script>

<style scoped>
.full-width {
  width: 100%;
}

.login-card {
  width: 300px;
  height: 350px;
  margin: 0;
  position: absolute;
  top: 50%;
  left: 50%;
  -ms-transform: translate(-50%, -50%);
  transform: translate(-50%, -50%);
}

.login-button {
  margin-top: 40px;
}
</style>

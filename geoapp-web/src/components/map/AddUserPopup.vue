<template>
  <q-dialog v-model="popupShowing">
    <q-card>
      <q-card-section>
        <div class="text-h6">Add user data</div>
      </q-card-section>

      <q-card-section class="q-pt-none">
        <q-input
          outlined
          bottom-slots
          v-model="newUserToken"
          label="Token"
          :dense="true"
          type="password"
        />
      </q-card-section>

      <q-card-actions>
        <q-btn flat label="CANCEL" @click="closePopup" />
        <q-btn flat label="OK" color="primary" @click="addUser" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>
<script lang="ts" setup>
import { ref } from 'vue';
import { type JwtToken, type OtherUserDataAllowance, PermissionReadData } from '../models';
import { useQuasar } from 'quasar';
import { getRandomHexColor } from 'src/util/color';
import { jwtDecode } from 'jwt-decode';

const quasar = useQuasar();

const emit = defineEmits<{
  (event: 'userAdded', value: OtherUserDataAllowance): void;
}>();

const popupShowing = ref(false);
const newUserToken = ref<string>();

const showAddUserPopup = () => {
  popupShowing.value = true;
};

const addUser = () => {
  // If the token is invalid
  if (!newUserToken.value) {
    quasar.notify({
      type: 'negative',
      message: 'Invalid token',
      timeout: 3000,
      position: 'top',
    });
    return;
  }

  try {
    const newUserJwt = jwtDecode<JwtToken>(newUserToken.value);

    // If the token does not have read permissions
    if (!(newUserJwt.Permissions?.includes(PermissionReadData) == true)) {
      quasar.notify({
        type: 'negative',
        message: 'Token does not have read permission.',
        timeout: 3000,
        position: 'top',
      });
      return;
    }

    // If the token is already expired
    if (new Date((newUserJwt.exp ?? 0) * 1000) < new Date()) {
      quasar.notify({
        type: 'negative',
        message: 'Token is already expired.',
        timeout: 3000,
        position: 'top',
      });
      return;
    }

    // Get the necessary values from the token
    const userId =
      newUserJwt['http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier'];
    const username = newUserJwt['http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name'];
    const userReadToken = newUserToken.value;
    const color = getRandomHexColor();

    emit('userAdded', { userId, username, userReadToken, color });

    closePopup();
  } catch {
    quasar.notify({
      type: 'negative',
      message: `Unknown error`,
      timeout: 3000,
      position: 'top',
    });
  }
};

const closePopup = () => {
  popupShowing.value = false;
  newUserToken.value = '';
};

defineExpose({ showAddUserPopup });
</script>

<script setup>
import { computed } from 'vue';

import DefaultIcon from '@/assets/icons/question-gray-16.svg';
import CorrectIcon from '@/assets/icons/mypage/check-green-16.svg';
import WrongIcon from '@/assets/icons/mypage/x-red-16.svg';

import BaseInput from '../common/BaseInput.vue';

defineOptions({
  inheritAttrs: false,
});

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
  id: {
    type: String,
    required: true,
  },
  label: {
    type: String,
    required: true,
  },
  type: {
    type: String,
    default: 'text',
  },
  message: {
    type: String,
    default: '',
  },
  status: {
    type: String,
    default: 'default',
    validator: (value) => ['default', 'correct', 'wrong'].includes(value),
  },
});

const emit = defineEmits(['update:modelValue', 'focus', 'blur']);

const statusIcon = computed(() => {
  const iconMap = {
    default: DefaultIcon,
    correct: CorrectIcon,
    wrong: WrongIcon,
  };

  return iconMap[props.status];
});
</script>

<template>
  <div class="auth-input w-100 d-flex flex-column gap-2">
    <label :for="id" class="auth-input__label form-label fw-semibold mb-0">
      {{ label }}
    </label>
    <BaseInput
      v-bind="$attrs"
      :id="id"
      :model-value="modelValue"
      :type="type"
      @update:model-value="emit('update:modelValue', $event)"
      @focus="emit('focus', $event)"
      @blur="emit('blur', $event)"
    />
    <div class="d-flex gap-1">
      <img
        :src="statusIcon"
        class="auth-input__status-icon flex-shrink-0"
        alt=""
      />
      <p
        class="auth-input__status-message mb-0"
        :class="{
          'auth-input__status-message-default': status === 'default',
          'auth-input__status-message-correct': status === 'correct',
          'auth-input__status-message-wrong': status === 'wrong',
        }"
      >
        {{ message }}
      </p>
    </div>
  </div>
</template>

<style scoped>
.auth-input__label {
  color: var(--black-900);
  font-size: 0.875rem;
  line-height: 1.4;
}

.auth-input__status-icon {
  width: 16px;
  height: 16px;
  object-fit: contain;
}

.auth-input__status-message {
  min-height: 1.05rem;
  font-size: 0.75rem;
  line-height: 1.4;
}

.auth-input__status-message-default {
  color: var(--black-500);
}

.auth-input__status-message-correct {
  color: var(--green-500);
}

.auth-input__status-message-wrong {
  color: var(--red-500);
}
</style>

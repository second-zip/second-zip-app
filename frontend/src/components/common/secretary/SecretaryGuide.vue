<script setup>
import { onBeforeUnmount, ref, watch } from 'vue';

import SecretaryCharacter from './SecretaryCharacter.vue';
import SecretaryBubble from './SecretaryBubble.vue';

const props = defineProps({
  text: {
    type: String,
    required: true,
  },
  floating: {
    type: Boolean,
    default: false,
  },
  changeBtn: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(['change']);

const isExpanded = ref(true);

let collapseTimer;

const clearCollapseTimer = () => {
  clearTimeout(collapseTimer);
};

const resetGuide = () => {
  clearCollapseTimer();
  isExpanded.value = true;

  if (props.floating) {
    collapseTimer = setTimeout(() => {
      isExpanded.value = false;
    }, 4000);
  }
};

const openGuide = () => {
  if (!props.floating || isExpanded.value) return;

  resetGuide();
};

const handleChange = () => {
  emit('change');
};

watch(() => [props.text, props.floating], resetGuide, { immediate: true });

onBeforeUnmount(clearCollapseTimer);
</script>

<template>
  <div
    class="secretary-guide position-relative w-100"
    :class="{
      'secretary-guide--floating position-fixed start-50 translate-middle-x':
        props.floating,
    }"
  >
    <button
      v-if="props.changeBtn"
      type="button"
      class="secretary-guide__change-btn position-absolute bg-white fw-semibold"
      @click="handleChange"
    >
      AI 비서 변경하기
    </button>

    <div class="secretary-guide__content d-flex align-items-end gap-2 w-100">
      <SecretaryCharacter :collapsed="!isExpanded" @click="openGuide" />

      <div
        class="secretary-guide__message flex-grow-1"
        :class="{
          'secretary-guide__message--with-change-btn': props.changeBtn,
        }"
      >
        <Transition name="bubble">
          <SecretaryBubble v-if="isExpanded" :text="props.text" />
        </Transition>
      </div>
    </div>
  </div>
</template>

<style scoped>
.secretary-guide {
  height: fit-content;
  padding: 12px;
}

.secretary-guide__content,
.secretary-guide__message {
  min-width: 0;
}

.secretary-guide__message--with-change-btn {
  padding-top: 40px;
}

.secretary-guide__change-btn {
  top: 8px;
  right: 12px;
  height: 32px;
  padding: 0 16px;
  z-index: 1;

  color: var(--blue-700);
  border: 1px solid var(--blue-500);
  border-radius: 999px;

  font-size: 0.75rem;
  white-space: nowrap;
}

.secretary-guide__change-btn:hover {
  background-color: var(--blue-100) !important;
}

/* 하단 네비게이션 64px 바로 위 */
.secretary-guide--floating {
  bottom: 64px;
  max-width: 402px;
  z-index: 1090;
  pointer-events: none;
}

.secretary-guide--floating > * {
  pointer-events: auto;
}

.bubble-enter-active,
.bubble-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.bubble-enter-from,
.bubble-leave-to {
  opacity: 0;
  transform: translateX(-6px);
}

@media (max-width: 767.98px) {
  .secretary-guide--floating {
    max-width: none;
  }
}
</style>

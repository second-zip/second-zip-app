<script setup>
import { onBeforeUnmount, ref, watch } from 'vue';

import SecretaryCharacter from './SecretaryCharacter.vue';
import SecretaryBubble from './SecretaryBubble.vue';

const props = defineProps({
  text: {
    type: String,
    required: true,
  },

  /*
   * true  : 일반 콘텐츠 흐름에 표시
   * false : 하단 네비게이션 위에 fixed로 표시
   */
  floating: {
    type: Boolean,
    default: false,
  },
});

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

watch(() => [props.text, props.floating], resetGuide, { immediate: true });

onBeforeUnmount(clearCollapseTimer);
</script>

<template>
  <div
    class="secretary-guide d-flex align-items-center gap-2 w-100"
    :class="{
      'secretary-guide--floating position-fixed start-50 translate-middle-x':
        floating,
    }"
  >
    <SecretaryCharacter :collapsed="!isExpanded" @click="openGuide" />

    <Transition name="bubble">
      <SecretaryBubble v-if="isExpanded" :text="text" />
    </Transition>
  </div>
</template>

<style scoped>
.secretary-guide {
  height: fit-content;
  padding: 12px;
}

/* 하단 네비게이션 64px 바로 위 */
.secretary-guide--floating {
  bottom: 64px;
  max-width: 402px;
  z-index: 1090;
  pointer-events: none;
}

/* fixed 부모의 빈 영역이 클릭을 막지 않도록 실제 UI만 클릭 허용 */
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

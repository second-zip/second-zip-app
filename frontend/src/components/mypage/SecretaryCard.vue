<script setup>
import NextSecretaryImage from '@/assets/images/next.png';

const props = defineProps({
  bouncing: { type: Boolean, default: false },
  option: { type: Object, default: null },
  preparing: { type: Boolean, default: false },
  selected: { type: Boolean, default: false },
});

defineEmits(['select']);
</script>

<template>
  <button
    type="button"
    class="secretary-card"
    :class="[
      props.preparing ? 'is-ready' : `is-${props.option.tone}`,
      { selected: props.selected },
    ]"
    :data-dictionary-character="props.option?.dictionaryCharacter"
    :aria-pressed="props.preparing ? undefined : props.selected"
    @click="$emit('select')"
  >
    <template v-if="props.preparing">
      <strong>준비 중</strong>
      <img
        :src="NextSecretaryImage"
        class="secretary-card__next"
        alt="다음 AI 비서 준비 중"
      />
      <span>기다려 주세요~</span>
    </template>
    <template v-else>
      <strong>{{ props.option.label }}</strong>
      <img
        :src="props.option.image"
        :alt="props.option.label"
        :class="{ 'is-bouncing': props.bouncing }"
      />
      <span>“{{ props.option.message }}”</span>
    </template>
  </button>
</template>

<style scoped>
.secretary-card { display: flex; min-width: 0; min-height: 0; flex-direction: column; align-items: center; justify-content: space-between; overflow: hidden; padding: 10px 7px 7px; color: #fff; border: 3px solid transparent; border-radius: 15px; }
.secretary-card.selected { border-color: #176cf3; box-shadow: 0 0 0 2px #fff inset; }
.secretary-card strong { flex: 0 0 auto; font-size: clamp(13px, 4vw, 17px); line-height: 1.15; }
.secretary-card img { width: 100%; min-height: 0; flex: 1 1 auto; object-fit: contain; object-position: center; }
.secretary-card img.is-bouncing { animation: character-bounce 320ms ease-out; }
.secretary-card span { width: 100%; flex: 0 0 auto; overflow: hidden; padding: 6px 2px; border: 1px solid rgba(255,255,255,.2); border-radius: 7px; background: rgba(255,255,255,.2); font-size: clamp(7px, 2.2vw, 9px); text-align: center; text-overflow: ellipsis; white-space: nowrap; }
.is-pink { background: #e671a1; }.is-green { background: #08b875; }.is-purple { background: #8665e5; }.is-ready { background: #92939c; }
.secretary-card .secretary-card__next { width: min(100%, 240px); margin: 5px 0; }

@keyframes character-bounce {
  0%, 100% { transform: translateY(0) scale(1); }
  48% { transform: translateY(-5px) scale(1.025); }
}

@media (prefers-reduced-motion: reduce) {
  .secretary-card img.is-bouncing { animation: none; }
}
</style>

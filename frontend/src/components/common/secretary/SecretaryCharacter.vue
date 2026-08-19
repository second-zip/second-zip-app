<script setup>
import catImage from '@/assets/images/cat-main.png';
import manImage from '@/assets/images/man-main.png';
import womanImage from '@/assets/images/woman-main.png';

const CHARACTER_IMAGES = {
  CAT: catImage,
  MAN: manImage,
  WOMAN: womanImage,
};

const props = defineProps({
  collapsed: {
    type: Boolean,
    default: false,
  },
  characterType: {
    type: String,
    default: 'CAT',
  },
});

defineEmits(['click']);
</script>

<template>
  <button
    type="button"
    class="secretary-character flex-shrink-0 rounded-circle border-0 p-0"
    :class="{ 'secretary-character--collapsed': collapsed }"
    :aria-label="collapsed ? '안내 메시지 열기' : '안내 캐릭터'"
    @click="$emit('click')"
  >
    <img
      :src="CHARACTER_IMAGES[props.characterType] || CHARACTER_IMAGES.CAT"
      alt=""
      class="d-block w-100 h-100"
    />
  </button>
</template>

<style scoped>
.secretary-character {
  width: 100px;
  height: 100px;
  overflow: hidden;
  background-color: var(--mint-500);
  box-shadow: 0 1px 8px rgb(123 216 176 / 25%);
  transition:
    width 0.25s ease,
    height 0.25s ease;
}

.secretary-character--collapsed {
  width: 48px;
  height: 48px;
  cursor: pointer;
}

.secretary-character img {
  object-fit: contain;
  transform: scale(1.06);
}
</style>

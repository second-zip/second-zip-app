<script setup>
defineProps({ type: { type: Object, required: true } });
defineEmits(["play"]);

// 고해상도 썸네일 로딩 실패 시 유튜브 기본 썸네일로 교체합니다.
const handleThumbnailError = ({ currentTarget }, fallbackSrc) => {
  if (!fallbackSrc || currentTarget.dataset.fallbackApplied) return;

  currentTarget.dataset.fallbackApplied = "true";
  currentTarget.src = fallbackSrc;
};
</script>
<template>
  <article class="fraud-card">
    <header>
      <span>{{ type.number }}</span>
      <h2>{{ type.title }}</h2>
    </header>
    <ul class="fraud-card__hashtags" :aria-label="`${type.title} 관련 항목`">
      <li v-for="hashtag in type.hashtags" :key="hashtag">#{{ hashtag }}</li>
    </ul>
    <div class="fraud-card__body">
      <button
        type="button"
        :aria-label="`${type.title} 영상 보기`"
        @click="$emit('play', type.id)"
      >
        <img
          v-if="type.thumbnailSrc"
          :src="type.thumbnailSrc"
          :alt="`${type.title} 영상 썸네일`"
          @error="handleThumbnailError($event, type.thumbnailFallbackSrc)"
        />
        <i aria-hidden="true"></i>
      </button>
      <p>{{ type.description }}</p>
    </div>
  </article>
</template>
<style scoped>
.fraud-card {
  padding: 14px 15px 16px;
  border: 1px solid var(--black-100);
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 2px 7px rgb(17 17 24 / 7%);
}
header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
header span {
  display: grid;
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  place-items: center;
  border: 1px solid var(--blue-900);
  border-radius: 9px;
  color: var(--blue-900);
  font-size: 16px;
  font-weight: 700;
}
.fraud-card__hashtags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin: 0 0 12px;
  padding: 0;
  list-style: none;
}
.fraud-card__hashtags li {
  padding: 5px 7px;
  border-radius: 5px;
  background: var(--blue-900);
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  line-height: 1;
  letter-spacing: 0;
  white-space: nowrap;
}
h2 {
  margin: 0;
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 0;
}
.fraud-card__body {
  display: grid;
  grid-template-columns: 80px minmax(0, 1fr);
  gap: 12px;
  align-items: start;
}
button {
  position: relative;
  width: 80px;
  aspect-ratio: 9/16;
  border: 0;
  background: #111118;
  cursor: pointer;
  overflow: hidden;
}
button img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
button::before {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 30px;
  height: 30px;
  content: "";
  transform: translate(-50%, -50%);
  border: 3px solid #f2a6ab;
  border-radius: 50%;
  background: rgb(17 17 24 / 68%);
  box-shadow: 0 2px 8px rgb(0 0 0 / 35%);
}
button i {
  position: absolute;
  top: 50%;
  left: 51%;
  width: 0;
  height: 0;
  transform: translate(-35%, -50%);
  border-top: 6px solid transparent;
  border-bottom: 6px solid transparent;
  border-left: 9px solid #f2a6ab;
}
button:hover {
  filter: brightness(0.88);
}
button:focus-visible {
  outline: 3px solid var(--blue-300);
  outline-offset: 2px;
}
p {
  margin: 0;
  color: var(--black-500);
  font-size: 11px;
  line-height: 1.75;
  word-break: keep-all;
}
</style>

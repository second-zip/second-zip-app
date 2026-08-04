<script setup>
import arrowBlue from "@/assets/icons/dict/arrow-blue-26.svg";
import arrowPurple from "@/assets/icons/dict/arrow-blue-26-1.svg";
import arrowPink from "@/assets/icons/dict/arrow-red-26.svg";
import arrowGreen from "@/assets/icons/dict/arrow-green-26.svg";
defineProps({ card: { type: Object, required: true } });
defineEmits(["select"]);
const arrowByTone = {
  blue: arrowBlue,
  purple: arrowPurple,
  pink: arrowPink,
  green: arrowGreen,
};
</script>
<template>
  <button
    class="menu-card"
    :class="`menu-card--${card.tone}`"
    type="button"
    @click="$emit('select', card.id)"
  >
    <span class="menu-card__visual"><img :src="card.image" alt="" /></span>
    <span class="menu-card__copy"
      ><strong>{{ card.title }}</strong
      ><small>{{ card.description }}</small></span
    >
    <img class="menu-card__arrow" :src="arrowByTone[card.tone]" alt="" />
  </button>
</template>
<style scoped>
.menu-card {
  position: relative;
  display: flex;
  width: 100%;
  min-width: 0;
  height: 192px;
  flex-direction: column;
  overflow: hidden;
  padding: 8px 14px 16px;
  text-align: left;
  border: 1px solid;
  border-radius: 24px;
  background: white;
  color: var(--black-900);
  transition:
    transform 0.16s ease,
    box-shadow 0.16s ease;
}
.menu-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 18px rgb(17 17 24 / 8%);
}
.menu-card:focus-visible {
  outline: 3px solid rgb(7 94 241 / 25%);
  outline-offset: 2px;
}
.menu-card--blue {
  border-color: #4f8dff;
  background: #eef5ff;
}
.menu-card--purple {
  border-color: #9868ff;
  background: #f5efff;
}
.menu-card--pink {
  border-color: #f073a7;
  background: #fff1f6;
}
.menu-card--green {
  border-color: #12b76a;
  background: #edfbf7;
}
.menu-card__visual {
  display: flex;
  height: 126px;
  align-items: center;
  justify-content: center;
}
.menu-card__visual img {
  width: 128px;
  height: 122px;
  object-fit: contain;
}
.menu-card__copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
  padding-right: 28px;
}
.menu-card__copy strong {
  font-size: 15px;
  line-height: 1.2;
  font-weight: 800;
  letter-spacing: 0;
}
.menu-card__copy small {
  overflow: hidden;
  color: var(--black-500);
  font-size: 10px;
  line-height: 1.3;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.menu-card__arrow {
  position: absolute;
  right: 12px;
  bottom: 22px;
  width: 26px;
  height: 26px;
}
</style>

<script setup>
defineOptions({
  inheritAttrs: false,
});

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
  type: {
    type: String,
    default: 'text',
  },
});

const emit = defineEmits(['update:modelValue']);

const handleInput = (event) => {
  emit('update:modelValue', event.target.value);
};
</script>

<template>
  <input
    v-bind="$attrs"
    :type="props.type"
    :value="props.modelValue"
    class="base-input form-control w-100 fw-semibold"
    @input="handleInput"
  />
</template>

<style scoped>
.base-input {
  height: 52px;
  padding: 0 1rem;

  color: var(--black-900);
  background-color: #f8faff;

  border: 1px solid var(--black-100);
  border-radius: 14px;
  font-size: 1rem;
}

.base-input::placeholder {
  color: var(--black-300);
  font-weight: 400;
}

.base-input:focus {
  color: var(--black-900);
  background-color: #f8faff;
  border-color: var(--blue-700);
  box-shadow: none;
}

.base-input:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}
</style>

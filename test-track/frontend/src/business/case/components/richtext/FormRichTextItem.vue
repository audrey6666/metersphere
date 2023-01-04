<template>
  <el-form-item
    :disable="true"
    :label="title"
    :prop="prop"
    :label-width="labelWidth"
  >
    <ms-mark-down-text
      :prop="prop"
      :data="data"
      :placeholder="placeholder"
      :disabled="disabled"
      :default-open="defaultOpen"
    />
  </el-form-item>
</template>

<script>
import MsMarkDownText from "./MsMarkDownText";
export default {
  name: "FormRichTextItem",
  components: { MsMarkDownText },
  props: {
    data: Object,
    prop: String,
    disabled: Boolean,
    title: String,
    placeholder: String,
    defaultOpen: {
      type: String,
      default() {
        return "preview";
      },
    },
    labelWidth: [String, Number],
  },
};
</script>

<style scoped>
:deep(.v-note-wrapper) {
  position: initial;
}
:deep(.v-note-op) {
  background-color: #f8f9fa !important;
  border-bottom: 1px solid #bbbfc4;
}
:deep(.v-note-wrapper) {
  box-sizing: border-box;
  border: 1px solid #bbbfc4 !important;
  border-radius: 4px;
  box-shadow: none !important;
}
:deep(.v-note-show) {
  min-height: 65px;
}
:deep(.v-left-item) {
  flex: none !important;
}
</style>

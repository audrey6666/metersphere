<template>
    <span>
      <el-tag v-if="value === 'Prepare'" type="info" plain class="status-label">
        {{ $t('test_track.review.prepare') }}
      </el-tag>
      <el-tag v-else-if="value === 'Pass'" type="success" plain class="status-label">
        {{ $t('test_track.review.pass') }}
      </el-tag>
      <el-tag v-else-if="value === 'UnPass'" type="danger" plain class="status-label">
        {{ $t('test_track.review.un_pass') }}
      </el-tag>
      <el-tag v-else-if="value === 'Underway'" class="status-label underway">
        {{ $t('test_track.review.underway') }}
      </el-tag>
      <el-tag v-else-if="value === 'Again'" class="status-label again">
        {{ $t('test_track.review.again') }}
      </el-tag>
      <el-tag v-else class="status-label" type="info" plain>
        {{ value }}
      </el-tag>
    </span>
</template>

<script>
import MsTag from "metersphere-frontend/src/components/MsTag";

export default {
  name: "ReviewStatus",
  components: {MsTag},
  props: {
    value: {
      type: String
    }
  }
}
</script>

<style scoped>
.status-label {
  height: 24px;
  font-weight: 400;
  font-size: 14px;
  line-height: 22px;
  padding: 1px 6px;
  border-radius: 2px;
  border-color: transparent;
}

.underway {
  color: #783887;
  background: rgba(120, 56, 135, 0.2);;
}
</style>

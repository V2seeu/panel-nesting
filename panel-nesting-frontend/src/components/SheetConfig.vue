<template>
  <el-card header="板材参数">
    <el-form :model="sheet" label-width="80px" size="small">
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="宽度(mm)">
            <el-input-number v-model="sheet.width" :min="100" :max="5000" :step="10" style="width:100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="高度(mm)">
            <el-input-number v-model="sheet.height" :min="100" :max="5000" :step="10" style="width:100%" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="留白(mm)">
            <el-input-number v-model="sheet.margin" :min="0" :max="100" :step="1" style="width:100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="间距(mm)">
            <el-input-number v-model="sheet.gap" :min="0" :max="50" :step="0.5" style="width:100%" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-divider content-position="left">避让区域（定位孔）</el-divider>
      <div class="zone-header">
        <span class="zone-header-cell">X(mm)</span>
        <span class="zone-header-cell">Y(mm)</span>
        <span class="zone-header-cell">半径(mm)</span>
        <span class="zone-header-cell zone-header-cell--action"></span>
      </div>
      <div v-for="(zone, idx) in sheet.avoidZones" :key="idx" class="zone-row">
        <el-input-number v-model="zone.cx" :min="0" size="small" class="zone-field" />
        <el-input-number v-model="zone.cy" :min="0" size="small" class="zone-field" />
        <el-input-number v-model="zone.radius" :min="1" size="small" class="zone-field" />
        <el-button type="danger" :icon="Delete" circle size="small" @click="sheet.avoidZones.splice(idx, 1)" />
      </div>
      <el-button type="primary" link size="small" @click="sheet.avoidZones.push({ cx: 0, cy: 0, radius: 8 })">
        + 添加避让区域
      </el-button>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { Delete } from '@element-plus/icons-vue'
import type { Sheet } from '../types'

const sheet = defineModel<Sheet>({ required: true })
</script>

<style scoped>
.zone-header {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 6px;
  padding-bottom: 4px;
  border-bottom: 1px solid #ebeef5;
}
.zone-header-cell {
  flex: 1;
  font-size: 11px;
  color: #909399;
  text-align: center;
}
.zone-header-cell--action {
  flex: none;
  width: 24px;
}
.zone-row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.zone-field {
  flex: 1;
}
.zone-field :deep(.el-input__wrapper) {
  padding-left: 8px;
  padding-right: 8px;
}
</style>

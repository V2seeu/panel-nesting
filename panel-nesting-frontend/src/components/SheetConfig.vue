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
      <div v-for="(zone, idx) in sheet.avoidZones" :key="idx" style="display:flex;gap:8px;margin-bottom:8px;align-items:center">
        <el-input-number v-model="zone.cx" :min="0" placeholder="X" size="small" style="width:25%" />
        <el-input-number v-model="zone.cy" :min="0" placeholder="Y" size="small" style="width:25%" />
        <el-input-number v-model="zone.radius" :min="1" placeholder="半径" size="small" style="width:25%" />
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

<template>
  <el-card header="零件列表">
    <div v-for="(part, idx) in parts" :key="idx" class="part-item">
      <div class="part-color-bar" :style="{ backgroundColor: colors[idx % colors.length] }"></div>
      <div class="part-fields">
        <el-row :gutter="8">
          <el-col :span="6">
            <el-input v-model="part.name" placeholder="名称" size="small" />
          </el-col>
          <el-col :span="5">
            <el-input-number v-model="part.width" :min="1" placeholder="宽" size="small" style="width:100%" />
          </el-col>
          <el-col :span="5">
            <el-input-number v-model="part.height" :min="1" placeholder="高" size="small" style="width:100%" />
          </el-col>
          <el-col :span="4">
            <el-input-number v-model="part.quantity" :min="1" placeholder="数量" size="small" style="width:100%" />
          </el-col>
          <el-col :span="2">
            <el-checkbox v-model="part.rotatable" size="small">旋转</el-checkbox>
          </el-col>
          <el-col :span="2">
            <el-button type="danger" :icon="Delete" circle size="small" @click="parts.splice(idx, 1)" />
          </el-col>
        </el-row>
      </div>
    </div>
    <el-button type="primary" link @click="addPart">+ 添加零件</el-button>
  </el-card>
</template>

<script setup lang="ts">
import { Delete } from '@element-plus/icons-vue'
import type { Part } from '../types'

const parts = defineModel<Part[]>({ required: true })

defineProps<{
  hoveredPartName: string | null
}>()

defineEmits<{
  hoverPart: [name: string | null]
}>()

const colors = [
  '#4FC3F7', '#FF8A65', '#81C784', '#BA68C8', '#FFD54F',
  '#4DB6AC', '#F06292', '#A1887F', '#90A4AE', '#FF7043'
]

function addPart() {
  const idx = parts.value.length + 1
  parts.value.push({
    id: 'p' + idx,
    name: 'P' + idx,
    width: 100,
    height: 100,
    quantity: 1,
    rotatable: true
  })
}
</script>

<style scoped>
.part-item {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  gap: 8px;
}
.part-color-bar {
  width: 6px;
  min-height: 32px;
  border-radius: 3px;
  flex-shrink: 0;
}
.part-fields {
  flex: 1;
}
.part-item--highlighted {
  background-color: #ecf5ff;
  border-radius: 4px;
}
.part-item--highlighted .part-color-bar {
  box-shadow: 0 0 6px 2px currentColor;
}
</style>

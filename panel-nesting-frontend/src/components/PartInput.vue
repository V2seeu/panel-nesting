<template>
  <el-card header="零件列表">
    <div class="header-row">
      <span class="header-spacer"></span>
      <div class="header-fields">
        <span class="header-cell header-cell--name">名称</span>
        <span class="header-cell header-cell--num">宽(mm)</span>
        <span class="header-cell header-cell--num">高(mm)</span>
        <span class="header-cell header-cell--qty">数量</span>
        <span class="header-cell header-cell--checkbox">旋转</span>
        <span class="header-cell header-cell--delete"></span>
      </div>
    </div>
    <div v-for="(part, idx) in parts" :key="idx" class="part-item"
         :class="{ 'part-item--highlighted': hoveredPartName === part.name }"
         @mouseenter="emit('hoverPart', part.name)"
         @mouseleave="emit('hoverPart', null)">
      <div class="part-color-bar" :style="{ backgroundColor: colors[idx % colors.length] }"></div>
      <div class="part-fields">
        <div class="part-row">
          <el-input v-model="part.name" size="small" class="field-name" />
          <el-input-number v-model="part.width" :min="1" size="small" controls-position="right" class="field-num" />
          <el-input-number v-model="part.height" :min="1" size="small" controls-position="right" class="field-num" />
          <el-input-number v-model="part.quantity" :min="1" size="small" controls-position="right" class="field-qty" />
          <el-checkbox v-model="part.rotatable" size="small" />
          <el-button type="danger" :icon="Delete" circle size="small" @click="parts.splice(idx, 1)" />
        </div>
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

const emit = defineEmits<{
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
  min-width: 0;
}
.part-row {
  display: flex;
  align-items: center;
  gap: 4px;
}
.header-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  padding-bottom: 4px;
  border-bottom: 1px solid #ebeef5;
}
.header-spacer {
  width: 6px;
  flex-shrink: 0;
}
.header-fields {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 4px;
}
.header-cell {
  font-size: 11px;
  color: #909399;
  text-align: center;
}
.header-cell--name {
  flex: 3;
  min-width: 50px;
}
.header-cell--num {
  flex: 4;
  min-width: 60px;
}
.header-cell--qty {
  flex: 3;
  min-width: 40px;
}
.header-cell--checkbox {
  width: 40px;
  flex-shrink: 0;
  text-align: center;
}
.header-cell--delete {
  flex-shrink: 0;
  width: 24px;
}
.field-name {
  flex: 3;
  min-width: 50px;
}
.field-num {
  flex: 4;
  min-width: 60px;
}
.field-qty {
  flex: 3;
  min-width: 40px;
}
.part-item--highlighted {
  background-color: #ecf5ff;
  border-radius: 4px;
}
.part-item--highlighted .part-color-bar {
  box-shadow: 0 0 6px 2px currentColor;
}
</style>

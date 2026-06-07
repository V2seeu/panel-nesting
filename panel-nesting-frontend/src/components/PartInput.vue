<template>
  <el-card header="零件列表">
    <div v-for="(part, idx) in parts" :key="idx" class="part-item"
         :class="{ 'part-item--highlighted': hoveredPartName === part.name }"
         @mouseenter="emit('hoverPart', part.name)"
         @mouseleave="emit('hoverPart', null)">
      <div class="part-color-bar" :style="{ backgroundColor: colors[idx % colors.length] }"></div>
      <div class="part-fields">
        <div class="part-row">
          <div class="field-group">
            <span v-if="idx === 0" class="field-label">名称</span>
            <el-input v-model="part.name" size="small" class="field-name" />
          </div>
          <div class="field-group">
            <span v-if="idx === 0" class="field-label">宽(mm)</span>
            <el-input-number v-model="part.width" :min="1" size="small" controls-position="right" class="field-num" />
          </div>
          <div class="field-group">
            <span v-if="idx === 0" class="field-label">高(mm)</span>
            <el-input-number v-model="part.height" :min="1" size="small" controls-position="right" class="field-num" />
          </div>
          <div class="field-group">
            <span v-if="idx === 0" class="field-label">数量</span>
            <el-input-number v-model="part.quantity" :min="1" size="small" controls-position="right" class="field-qty" />
          </div>
          <div class="field-group field-group--checkbox">
            <span v-if="idx === 0" class="field-label">旋转</span>
            <el-checkbox v-model="part.rotatable" size="small" />
          </div>
          <div class="field-group field-group--delete">
            <span v-if="idx === 0" class="field-label">&nbsp;</span>
            <el-button type="danger" :icon="Delete" circle size="small" @click="parts.splice(idx, 1)" />
          </div>
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
  align-items: flex-end;
  gap: 4px;
}
.field-group {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.field-label {
  font-size: 11px;
  color: #909399;
  padding-left: 2px;
}
.field-group--checkbox {
  align-items: center;
}
.field-group--delete {
  align-items: center;
}
.field-name {
  width: 70px;
  flex-shrink: 0;
}
.field-num {
  width: 80px;
  flex-shrink: 0;
}
.field-qty {
  width: 60px;
  flex-shrink: 0;
}
.part-item--highlighted {
  background-color: #ecf5ff;
  border-radius: 4px;
}
.part-item--highlighted .part-color-bar {
  box-shadow: 0 0 6px 2px currentColor;
}
</style>

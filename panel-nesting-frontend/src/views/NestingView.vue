<template>
  <div class="nesting-page">
    <h1 class="page-title">CNC 拼板排样系统</h1>
    <div class="main-layout">
      <div class="left-panel">
        <SheetConfig v-model="sheet" />
        <PartInput v-model="parts" :hoveredPartName="hoveredPartName" @hoverPart="onCanvasHover" />
        <el-button type="primary" size="large" class="compute-btn"
                   :loading="loading" @click="compute">
          开始排样
        </el-button>
      </div>
      <div class="right-panel">
        <NestingCanvas :result="result" :sheet="sheet" :parts="parts"
                       :hoveredPartName="hoveredPartName"
                       @hoverPart="onCanvasHover" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import SheetConfig from '../components/SheetConfig.vue'
import PartInput from '../components/PartInput.vue'
import NestingCanvas from '../components/NestingCanvas.vue'
import { computeNesting } from '../api/nesting'
import type { Sheet, Part, NestingResult } from '../types'

const sheet = ref<Sheet>({
  width: 1220,
  height: 2440,
  margin: 10,
  gap: 3,
  avoidZones: [
    { cx: 50, cy: 50, radius: 8 },
    { cx: 1170, cy: 50, radius: 8 },
    { cx: 50, cy: 2390, radius: 8 },
    { cx: 1170, cy: 2390, radius: 8 },
    { cx: 610, cy: 1220, radius: 8 }
  ]
})

const parts = ref<Part[]>([
  { id: 'p1', name: 'P1', width: 200, height: 300, quantity: 10, rotatable: true },
  { id: 'p2', name: 'P2', width: 150, height: 150, quantity: 8, rotatable: true }
])

const result = ref<NestingResult | null>(null)
const loading = ref(false)
const hoveredPartName = ref<string | null>(null)

async function compute() {
  loading.value = true
  try {
    result.value = await computeNesting({ sheet: sheet.value, parts: parts.value })
  } catch (e) {
    console.error('排样计算失败', e)
    alert('排样计算失败，请检查参数')
  } finally {
    loading.value = false
  }
}

function onCanvasHover(name: string | null) {
  hoveredPartName.value = name
}
</script>

<style scoped>
.nesting-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 16px;
  box-sizing: border-box;
}
.page-title {
  margin: 0 0 16px;
  font-size: 22px;
  color: #333;
}
.main-layout {
  flex: 1;
  display: flex;
  gap: 16px;
  min-height: 0;
}
.left-panel {
  width: 380px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
}
.right-panel {
  flex: 1;
  min-width: 0;
}
.compute-btn {
  width: 100%;
}
</style>

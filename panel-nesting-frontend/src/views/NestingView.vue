<template>
  <div class="nesting-page">
    <h1 class="page-title">CNC 拼板排样系统</h1>
    <div class="main-layout" ref="layoutRef">
      <div class="left-panel" :style="{ width: leftWidth + 'px' }">
        <SheetConfig v-model="sheet" v-model:placingAvoidZone="placingAvoidZone" />
        <PartInput v-model="parts" :hoveredPartName="hoveredPartName" @hoverPart="onCanvasHover" />
        <el-card>
          <template #header>优化策略</template>
          <el-select v-model="strategy" style="width: 100%">
            <el-option label="利用率优先" value="UTILIZATION" />
            <el-option label="板材数优先" value="SHEET_COUNT" />
            <el-option label="切缝最短" value="MIN_SEAM" />
            <el-option label="齐边优先" value="ALIGN_EDGE" />
          </el-select>
        </el-card>
        <el-button type="primary" size="large" class="compute-btn"
                   :loading="loading" @click="compute">
          开始排样
        </el-button>
      </div>
      <div class="resize-handle" @mousedown="onResizeStart">
        <template v-if="dragging">
          <span class="resize-percent">{{ dragPercent }}%</span>
        </template>
        <template v-else>
          <div class="resize-grip">
            <span></span><span></span><span></span>
          </div>
        </template>
      </div>
      <div class="right-panel">
        <NestingCanvas :result="result" :sheet="sheet" :parts="parts"
                       :hoveredPartName="hoveredPartName"
                       :placingAvoidZone="placingAvoidZone"
                       @hoverPart="onCanvasHover"
                       @placeAvoidZone="onPlaceAvoidZone" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import SheetConfig from '../components/SheetConfig.vue'
import PartInput from '../components/PartInput.vue'
import NestingCanvas from '../components/NestingCanvas.vue'
import { computeNesting } from '../api/nesting'
import type { Sheet, Part, NestingResult, OptimizationStrategy } from '../types'

const layoutRef = ref<HTMLDivElement>()
const leftWidth = ref(420)
const dragging = ref(false)
const dragPercent = ref(33)

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
const placingAvoidZone = ref(false)
const strategy = ref<OptimizationStrategy>('UTILIZATION')

async function compute() {
  loading.value = true
  try {
    result.value = await computeNesting({ sheet: sheet.value, parts: parts.value, strategy: strategy.value })
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

function onPlaceAvoidZone(cx: number, cy: number) {
  sheet.value.avoidZones.push({ cx, cy, radius: 8 })
  placingAvoidZone.value = false
}

// --- Resize drag logic ---
function onResizeStart(e: MouseEvent) {
  e.preventDefault()
  const startX = e.clientX
  const startWidth = leftWidth.value
  const layoutEl = layoutRef.value
  if (!layoutEl) return
  const layoutWidth = layoutEl.clientWidth

  function onMouseMove(ev: MouseEvent) {
    const dx = ev.clientX - startX
    const newWidth = startWidth + dx
    const minW = 280
    const maxW = layoutWidth - 300
    leftWidth.value = Math.max(minW, Math.min(maxW, newWidth))
    dragPercent.value = Math.round(leftWidth.value / layoutWidth * 100)
  }

  function onMouseUp() {
    dragging.value = false
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
  }

  dragging.value = true
  dragPercent.value = Math.round(leftWidth.value / layoutWidth * 100)
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
}

onMounted(() => {
  if (layoutRef.value) {
    leftWidth.value = Math.max(280, Math.floor(layoutRef.value.clientWidth * 0.22))
  }
})
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
  min-height: 0;
}
.left-panel {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
  min-width: 0;
}
.resize-handle {
  width: 14px;
  cursor: col-resize;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-left: 1px solid #e4e7ed;
  border-right: 1px solid #e4e7ed;
  transition: background 0.2s;
}
.resize-handle:hover {
  background: #ecf5ff;
  border-color: #409eff;
}
.resize-handle:hover .resize-grip span {
  background: #409eff;
}
.resize-grip {
  display: flex;
  flex-direction: column;
  gap: 3px;
  align-items: center;
}
.resize-grip span {
  display: block;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #c0c4cc;
  transition: background 0.2s;
}
.resize-percent {
  font-size: 11px;
  color: #409eff;
  font-weight: 600;
  writing-mode: vertical-lr;
  letter-spacing: 1px;
  user-select: none;
}
.right-panel {
  flex: 1;
  min-width: 0;
}
.compute-btn {
  width: 100%;
}
</style>

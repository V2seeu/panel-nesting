<template>
  <div class="nesting-page">
    <h1 class="page-title">CNC 拼板排样系统</h1>
    <div class="main-layout" ref="layoutRef">
      <div class="left-panel" :style="{ width: leftWidth + 'px' }">
        <SheetConfig v-model="sheet" />
        <PartInput v-model="parts" :hoveredPartName="hoveredPartName" @hoverPart="onCanvasHover" />
        <el-button type="primary" size="large" class="compute-btn"
                   :loading="loading" @click="compute">
          开始排样
        </el-button>
      </div>
      <div class="resize-handle" @mousedown="onResizeStart">
        <div class="resize-grip">
          <span></span><span></span><span></span>
        </div>
        <div class="resize-tooltip" v-if="dragging">{{ dragPercent }}%</div>
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
import { ref, onMounted, onUnmounted } from 'vue'
import SheetConfig from '../components/SheetConfig.vue'
import PartInput from '../components/PartInput.vue'
import NestingCanvas from '../components/NestingCanvas.vue'
import { computeNesting } from '../api/nesting'
import type { Sheet, Part, NestingResult } from '../types'

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
  // Default to 1/3 of layout width
  if (layoutRef.value) {
    leftWidth.value = Math.max(280, Math.floor(layoutRef.value.clientWidth / 3))
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
.resize-tooltip {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%) translateX(24px);
  background: #303133;
  color: #fff;
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 4px;
  white-space: nowrap;
  pointer-events: none;
  z-index: 10;
}
.resize-tooltip::before {
  content: '';
  position: absolute;
  top: 50%;
  right: 100%;
  transform: translateY(-50%);
  border: 4px solid transparent;
  border-right-color: #303133;
}
.right-panel {
  flex: 1;
  min-width: 0;
}
.compute-btn {
  width: 100%;
}
</style>

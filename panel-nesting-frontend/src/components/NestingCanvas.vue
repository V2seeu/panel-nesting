<template>
  <div class="canvas-container" ref="containerRef">
    <div class="canvas-toolbar" v-if="result">
      <el-button-group>
        <el-button size="small" :disabled="currentSheet <= 0" @click="currentSheet--">上一块</el-button>
        <el-button size="small" disabled>板 {{ currentSheet + 1 }} / {{ result.totalSheets }}</el-button>
        <el-button size="small" :disabled="currentSheet >= result.totalSheets - 1" @click="currentSheet++">下一块</el-button>
      </el-button-group>
      <span class="utilization-text">材料利用率: {{ (result.utilization * 100).toFixed(1) }}%</span>
    </div>
    <canvas ref="canvasRef" @mousemove="onMouseMove" @mouseleave="onMouseLeave" @click="onCanvasClick"></canvas>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import type { NestingResult, Sheet, Part, Placement } from '../types'

const props = defineProps<{
  result: NestingResult | null
  sheet: Sheet
  parts: Part[]
  hoveredPartName: string | null
  placingAvoidZone: boolean
}>()

const emit = defineEmits<{
  hoverPart: [name: string | null]
  placeAvoidZone: [cx: number, cy: number]
}>()

const containerRef = ref<HTMLDivElement>()
const canvasRef = ref<HTMLCanvasElement>()
const currentSheet = ref(0)
const mouseSheetPos = ref<{ x: number; y: number } | null>(null)

const COLORS = [
  '#4FC3F7', '#FF8A65', '#81C784', '#BA68C8', '#FFD54F',
  '#4DB6AC', '#F06292', '#A1887F', '#90A4AE', '#FF7043'
]

function getPartColor(partName: string): string {
  const names = [...new Set(props.parts.map(p => p.name))]
  const idx = names.indexOf(partName)
  return COLORS[idx % COLORS.length]
}

function draw() {
  const canvas = canvasRef.value
  const container = containerRef.value
  if (!canvas || !container) return

  const ctx = canvas.getContext('2d')!
  const dpr = window.devicePixelRatio || 1
  const w = container.clientWidth
  const h = container.clientHeight
  canvas.width = w * dpr
  canvas.height = h * dpr
  canvas.style.width = w + 'px'
  canvas.style.height = h + 'px'
  ctx.scale(dpr, dpr)
  ctx.clearRect(0, 0, w, h)

  const sheet = props.sheet
  const padding = 40

  // Calculate scale to fit
  const scaleX = (w - 2 * padding) / sheet.width
  const scaleY = (h - 2 * padding) / sheet.height
  const scale = Math.min(scaleX, scaleY)
  const offsetX = (w - sheet.width * scale) / 2
  const offsetY = (h - sheet.height * scale) / 2

  // Draw sheet outline
  ctx.strokeStyle = '#999'
  ctx.lineWidth = 2
  ctx.strokeRect(offsetX, offsetY, sheet.width * scale, sheet.height * scale)

  // Draw margin area (dashed)
  ctx.strokeStyle = '#ccc'
  ctx.lineWidth = 1
  ctx.setLineDash([4, 4])
  const m = sheet.margin * scale
  ctx.strokeRect(offsetX + m, offsetY + m, sheet.width * scale - 2 * m, sheet.height * scale - 2 * m)
  ctx.setLineDash([])

  // Draw avoid zones
  for (const zone of sheet.avoidZones) {
    ctx.beginPath()
    ctx.arc(offsetX + zone.cx * scale, offsetY + zone.cy * scale, zone.radius * scale, 0, Math.PI * 2)
    ctx.fillStyle = 'rgba(255, 0, 0, 0.15)'
    ctx.fill()
    ctx.strokeStyle = 'rgba(255, 0, 0, 0.5)'
    ctx.lineWidth = 1
    ctx.stroke()
  }

  // Draw sheet dimensions label
  ctx.fillStyle = '#999'
  ctx.font = '12px sans-serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'top'
  ctx.fillText(`${sheet.width} × ${sheet.height} mm`, offsetX + sheet.width * scale / 2, offsetY + sheet.height * scale + 8)

  // Draw ghost avoid zone when in placing mode
  if (props.placingAvoidZone && mouseSheetPos.value) {
    const ghostR = 8 * scale
    ctx.beginPath()
    ctx.arc(offsetX + mouseSheetPos.value.x * scale, offsetY + mouseSheetPos.value.y * scale, ghostR, 0, Math.PI * 2)
    ctx.fillStyle = 'rgba(64, 158, 255, 0.2)'
    ctx.fill()
    ctx.strokeStyle = '#409eff'
    ctx.lineWidth = 1.5
    ctx.setLineDash([3, 3])
    ctx.stroke()
    ctx.setLineDash([])
  }

  // Draw placements only when result exists
  if (!props.result) return

  const sheetResult = props.result.sheets[currentSheet.value]
  if (!sheetResult) return

  for (const p of sheetResult.placements) {
    const isHovered = props.hoveredPartName === p.partName
    const color = getPartColor(p.partName)

    const px = offsetX + p.x * scale
    const py = offsetY + p.y * scale
    const pw = p.width * scale
    const ph = p.height * scale

    // Fill
    ctx.fillStyle = isHovered ? color : color + 'CC'
    ctx.fillRect(px, py, pw, ph)

    // Border
    ctx.strokeStyle = isHovered ? '#000' : color
    ctx.lineWidth = isHovered ? 3 : 1.5
    ctx.strokeRect(px, py, pw, ph)

    // Label
    const fontSize = Math.max(10, Math.min(pw, ph) * 0.25)
    ctx.font = `${fontSize}px sans-serif`
    ctx.fillStyle = '#333'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    const label = p.partName + (p.rotated ? ' (转)' : '')
    if (pw > 20 && ph > 15) {
      ctx.fillText(label, px + pw / 2, py + ph / 2)
    } else {
      ctx.font = '10px sans-serif'
      ctx.fillText(label, px + pw / 2, py - 8)
    }
  }
}

function onMouseMove(e: MouseEvent) {
  const canvas = canvasRef.value
  if (!canvas) return

  const rect = canvas.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top

  // Track mouse sheet position for ghost avoid zone
  if (props.placingAvoidZone) {
    const pos = canvasToSheet(x, y)
    if (pos) {
      mouseSheetPos.value = pos
    } else {
      mouseSheetPos.value = null
    }
    return
  }

  if (!props.result) return

  const sheet = props.sheet
  const padding = 40
  const scaleX = (canvas.clientWidth - 2 * padding) / sheet.width
  const scaleY = (canvas.clientHeight - 2 * padding) / sheet.height
  const scale = Math.min(scaleX, scaleY)
  const offsetX = (canvas.clientWidth - sheet.width * scale) / 2
  const offsetY = (canvas.clientHeight - sheet.height * scale) / 2

  const sheetResult = props.result.sheets[currentSheet.value]
  if (!sheetResult) return

  for (const p of sheetResult.placements) {
    const px = offsetX + p.x * scale
    const py = offsetY + p.y * scale
    const pw = p.width * scale
    const ph = p.height * scale
    if (x >= px && x <= px + pw && y >= py && y <= py + ph) {
      emit('hoverPart', p.partName)
      return
    }
  }
  emit('hoverPart', null)
}

function onMouseLeave() {
  emit('hoverPart', null)
  mouseSheetPos.value = null
}

function canvasToSheet(canvasX: number, canvasY: number): { x: number; y: number } | null {
  const canvas = canvasRef.value
  if (!canvas) return null
  const sheet = props.sheet
  const padding = 40
  const scaleX = (canvas.clientWidth - 2 * padding) / sheet.width
  const scaleY = (canvas.clientHeight - 2 * padding) / sheet.height
  const scale = Math.min(scaleX, scaleY)
  const offsetX = (canvas.clientWidth - sheet.width * scale) / 2
  const offsetY = (canvas.clientHeight - sheet.height * scale) / 2

  const sx = (canvasX - offsetX) / scale
  const sy = (canvasY - offsetY) / scale
  if (sx < 0 || sx > sheet.width || sy < 0 || sy > sheet.height) return null
  return { x: Math.round(sx), y: Math.round(sy) }
}

function onCanvasClick(e: MouseEvent) {
  if (!props.placingAvoidZone) return
  const canvas = canvasRef.value
  if (!canvas) return
  const rect = canvas.getBoundingClientRect()
  const pos = canvasToSheet(e.clientX - rect.left, e.clientY - rect.top)
  if (pos) {
    emit('placeAvoidZone', pos.x, pos.y)
  }
}

watch(() => [props.sheet, props.result, currentSheet.value, props.hoveredPartName, mouseSheetPos.value], draw, { deep: true })

let resizeObserver: ResizeObserver | null = null
onMounted(() => {
  draw()
  if (containerRef.value) {
    resizeObserver = new ResizeObserver(draw)
    resizeObserver.observe(containerRef.value)
  }
})
onUnmounted(() => resizeObserver?.disconnect())
</script>

<style scoped>
.canvas-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}
.canvas-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 0;
}
.utilization-text {
  font-size: 14px;
  color: #666;
}
canvas {
  flex: 1;
  cursor: v-bind("placingAvoidZone ? 'crosshair' : 'default'");
}
</style>

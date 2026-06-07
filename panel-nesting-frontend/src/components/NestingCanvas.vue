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
    <canvas ref="canvasRef" @mousemove="onMouseMove" @mouseleave="onMouseLeave"></canvas>
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
}>()

const emit = defineEmits<{
  hoverPart: [name: string | null]
}>()

const containerRef = ref<HTMLDivElement>()
const canvasRef = ref<HTMLCanvasElement>()
const currentSheet = ref(0)

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
  if (!canvas || !container || !props.result) return

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

  // Draw placements for current sheet
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
  if (!canvas || !props.result) return

  const rect = canvas.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top

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
}

watch(() => [props.result, currentSheet.value, props.hoveredPartName], draw)

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
  cursor: crosshair;
}
</style>

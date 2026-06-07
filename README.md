# Panel Nesting

**CNC 2D Rectangular Panel Nesting Optimizer** — an open-source nesting system that arranges rectangular parts on sheet material to minimize waste, with real-time visual preview and multiple optimization strategies.

> CNC 拼板排样系统 —— 基于 BL 放置算法 + 遗传算法的二维矩形件排样优化工具

![Java 21](https://img.shields.io/badge/Java-21-blue?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green?logo=springboot)
![Vue 3](https://img.shields.io/badge/Vue-3.5-brightgreen?logo=vuedotjs)
![TypeScript](https://img.shields.io/badge/TypeScript-5.6-blue?logo=typescript)
![License](https://img.shields.io/badge/License-MIT-yellow)

<!-- TODO: Add a screenshot here -->
<!--
![screenshot](docs/screenshot.png)
-->

## Features

- **4 Optimization Strategies** — switchable via UI
  - Utilization-first (maximize material usage)
  - Sheet-count-first (minimize number of sheets)
  - Min-seam (shortest cutting path)
  - Edge-alignment (neat grid-like layout)
- **BL + Genetic Algorithm** — Bottom-Left placement with GA-optimized part ordering (candidate keypoint method for fast placement)
- **Real-time Canvas Preview** — see sheet layout update instantly as you adjust parameters
- **Interactive Avoid Zones** — click-to-place drill hole positions on canvas, with preset templates (corners, corners+center)
- **Bidirectional Hover Highlight** — hover a part in the list to highlight it on canvas, and vice versa
- **Resizable Split Panel** — drag the divider to adjust config/preview ratio
- **No Database Required** — pure computation, deploy and run

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 3.3 |
| Algorithm | BL Placement + Genetic Algorithm (60 population, 200 generations) |
| Frontend | Vue 3, TypeScript, Element Plus, Canvas 2D |
| Build | Maven, Vite |

## Quick Start

### Prerequisites

- Java 21+
- Node.js 18+
- Maven 3.9+

### 1. Start Backend

```bash
cd panel-nesting-backend
mvn spring-boot:run
```

Server starts at `http://localhost:8080`.

### 2. Start Frontend

```bash
cd panel-nesting-frontend
npm install
npm run dev
```

Frontend starts at `http://localhost:5173` and proxies `/api` requests to the backend.

### 3. Open in Browser

Navigate to `http://localhost:5173`, configure your sheet and parts, and click **Start Nesting**.

## Algorithm

### BL Placement (Bottom-Left)

Parts are placed one by one at the lowest, then leftmost available position. A **candidate keypoint method** is used instead of pixel-by-pixel scanning — only the boundary corners of already-placed parts and avoid zones are checked as candidate positions, achieving ~90x speedup.

### Genetic Algorithm Optimization

The GA searches for the optimal part ordering to feed into the BL placer:

- **Population**: 60 individuals (random permutations)
- **Generations**: up to 200 (early stop after 40 stagnant generations)
- **Selection**: Tournament (k=3)
- **Crossover**: Order Crossover (OX)
- **Mutation**: Swap mutation (15% rate)
- **Elitism**: Best individual preserved each generation

### Fitness Functions

| Strategy | Fitness Function |
|----------|-----------------|
| Utilization | `totalPartArea / totalSheetArea` |
| Sheet Count | `-sheetsUsed * 10^6 + utilization` |
| Min Seam | `sharedEdgeLength + utilization * 1000` |
| Edge Alignment | `alignmentScore + utilization * 100` |

## API

### `POST /api/nesting/compute`

**Request:**

```json
{
  "sheet": {
    "width": 1220,
    "height": 2440,
    "margin": 10,
    "gap": 3,
    "avoidZones": [
      { "cx": 50, "cy": 50, "radius": 8 }
    ]
  },
  "parts": [
    { "id": "p1", "name": "Panel-A", "width": 200, "height": 300, "quantity": 10, "rotatable": true }
  ],
  "strategy": "UTILIZATION"
}
```

**Response:**

```json
{
  "totalSheets": 2,
  "utilization": 0.783,
  "sheets": [
    {
      "index": 0,
      "placements": [
        { "partId": "p1_0", "partName": "Panel-A", "sheetIndex": 0, "x": 10.0, "y": 10.0, "rotated": false, "width": 200.0, "height": 300.0 }
      ]
    }
  ]
}
```

**Strategy values:** `UTILIZATION` | `SHEET_COUNT` | `MIN_SEAM` | `ALIGN_EDGE`

## Project Structure

```
panel-nesting/
├── panel-nesting-backend/          # Spring Boot backend
│   └── src/main/java/com/nesting/
│       ├── algorithm/
│       │   ├── BLPlacer.java       # BL placement algorithm
│       │   ├── GeneticOptimizer.java  # GA with multi-strategy fitness
│       │   └── NestingEngine.java  # Orchestrator
│       ├── controller/
│       │   └── NestingController.java
│       └── model/                  # Data records
├── panel-nesting-frontend/         # Vue 3 frontend
│   └── src/
│       ├── api/
│       ├── components/
│       │   ├── SheetConfig.vue     # Sheet parameters + avoid zones
│       │   ├── PartInput.vue       # Part list with color coding
│       │   └── NestingCanvas.vue   # Canvas 2D renderer
│       ├── views/
│       │   └── NestingView.vue     # Main page layout
│       └── types/
└── README.md
```

## Roadmap

- [ ] DXF file import/export
- [ ] Irregular polygon support
- [ ] Multi-sheet batch processing
- [ ] G-code generation for CNC machines
- [ ] Docker deployment

## License

[MIT](LICENSE)

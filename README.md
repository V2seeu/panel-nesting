# Panel Nesting

CNC 二维矩形件排样优化系统 —— 基于 BL 放置算法 + 遗传算法，支持实时可视化预览与多种优化策略。

![Java 21](https://img.shields.io/badge/Java-21-blue?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green?logo=springboot)
![Vue 3](https://img.shields.io/badge/Vue-3.5-brightgreen?logo=vuedotjs)
![TypeScript](https://img.shields.io/badge/TypeScript-5.6-blue?logo=typescript)
![License](https://img.shields.io/badge/License-MIT-yellow)

<!-- TODO: 添加截图 -->
<!--
![截图](docs/screenshot.png)
-->

## 功能特性

- **4 种优化策略** —— 界面一键切换
  - 利用率优先（最大化材料利用率）
  - 板材数优先（使用最少板材）
  - 切缝最短（减少切割路径）
  - 齐边优先（整齐排列，便于加工）
- **BL + 遗传算法** —— 基于候选关键点的 BL 放置算法，搭配 GA 优化零件排列顺序，较逐像素扫描提速约 90 倍
- **实时画布预览** —— 修改板材参数即时生效，所见即所得
- **交互式避让区域** —— 画布点击放置定位孔，支持四角/四角+中心预设模板
- **双向悬停高亮** —— 零件列表与画布联动，快速定位
- **可拖拽分栏** —— 自由调整配置区与预览区比例
- **无需数据库** —— 纯计算服务，开箱即用

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 21, Spring Boot 3.3 |
| 算法 | BL 放置算法 + 遗传算法（种群 60，最大迭代 200 代） |
| 前端 | Vue 3, TypeScript, Element Plus, Canvas 2D |
| 构建 | Maven, Vite |

## 快速开始

### 环境要求

- Java 21+
- Node.js 18+
- Maven 3.9+

### 1. 启动后端

```bash
cd panel-nesting-backend
mvn spring-boot:run
```

服务启动于 `http://localhost:8080`。

### 2. 启动前端

```bash
cd panel-nesting-frontend
npm install
npm run dev
```

前端启动于 `http://localhost:5173`，自动代理 `/api` 请求到后端。

### 3. 浏览器访问

打开 `http://localhost:5173`，配置板材和零件参数，点击 **开始排样**。

## 算法说明

### BL 放置算法（Bottom-Left）

逐个将零件放置到最低、最左的可用位置。采用 **候选关键点法** 替代逐像素扫描 —— 仅检查已放置零件的边界角点和避让区域边界作为候选位置，性能提升约 90 倍。

### 遗传算法优化

GA 搜索最优零件排列顺序，输入 BL 放置器：

- **种群规模**：60 个个体（随机排列）
- **迭代上限**：200 代（连续 40 代无改善则提前终止）
- **选择**：锦标赛选择（k=3）
- **交叉**：顺序交叉（OX），交叉率 0.8
- **变异**：交换变异，变异率 0.15
- **精英保留**：每代保留最优个体

### 适应度函数

| 策略 | 适应度函数 |
|------|-----------|
| 利用率优先 | `零件总面积 / 板材总面积` |
| 板材数优先 | `-板材数 * 10^6 + 利用率` |
| 切缝最短 | `共享边长度 + 利用率 * 1000` |
| 齐边优先 | `对齐得分 + 利用率 * 100` |

## API 文档

### `POST /api/nesting/compute`

**请求示例：**

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

**响应示例：**

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

**策略可选值：** `UTILIZATION` | `SHEET_COUNT` | `MIN_SEAM` | `ALIGN_EDGE`

## 项目结构

```
panel-nesting/
├── panel-nesting-backend/             # Spring Boot 后端
│   └── src/main/java/com/nesting/
│       ├── algorithm/
│       │   ├── BLPlacer.java          # BL 放置算法
│       │   ├── GeneticOptimizer.java  # 遗传算法（多策略适应度）
│       │   └── NestingEngine.java     # 排样引擎入口
│       ├── controller/
│       │   └── NestingController.java # REST 接口
│       └── model/                     # 数据模型
├── panel-nesting-frontend/            # Vue 3 前端
│   └── src/
│       ├── api/
│       ├── components/
│       │   ├── SheetConfig.vue        # 板材参数 + 避让区域配置
│       │   ├── PartInput.vue          # 零件列表（颜色编码）
│       │   └── NestingCanvas.vue      # Canvas 2D 排样渲染
│       ├── views/
│       │   └── NestingView.vue        # 主页面布局
│       └── types/
└── README.md
```

## 开发计划

- [ ] 不规则多边形零件支持
- [ ] 多板材批量处理
- [ ] Docker 一键部署

## 许可证

[MIT](LICENSE)

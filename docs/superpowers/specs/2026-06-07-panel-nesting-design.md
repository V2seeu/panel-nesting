# CNC 拼板排样系统 - 设计文档

## 概述

CNC 拼板排样系统用于在 CNC 加工生产中，将多个零件高效排列在一块大板上，最大化材料利用率，降低反复上下料的成本。

技术栈：Spring Boot + Vue 3，暂不引入数据库，先跑通算法和预览。

## 需求总结

- **零件形状：** 先实现矩形排样，架构预留多边形扩展空间
- **约束条件：** 固定尺寸板材、零件间距、零件方向约束、边缘留白（夹持区）、避让定位孔（圆形区域）
- **优化目标：** 最大化材料利用率
- **前端交互：** 参数输入 + 2D 静态预览
- **项目结构：** 单体 Spring Boot 应用

## 算法方案

采用 **BL（Bottom-Left）+ 遗传算法** 方案：

- BL 算法负责根据给定顺序将零件逐个放置到最低最左的位置
- 遗传算法负责搜索最优的零件排列顺序
- 两者组合在实现复杂度和结果质量之间取得平衡

## 项目结构

```
panel-nesting/
├── panel-nesting-backend/          # Spring Boot 单体应用
│   ├── src/main/java/com/nesting/
│   │   ├── controller/             # REST API
│   │   │   └── NestingController   # 接收排样请求，返回结果
│   │   ├── model/                  # 数据模型（纯内存，无DB）
│   │   │   ├── Sheet               # 板材（尺寸、边缘留白、避让孔）
│   │   │   ├── Part                # 零件（尺寸、方向约束、数量）
│   │   │   ├── NestingRequest      # 请求体
│   │   │   └── NestingResult       # 排样结果（含利用率、放置坐标）
│   │   ├── algorithm/              # 核心算法模块（纯Java，不依赖Spring）
│   │   │   ├── BLPlacer            # Bottom-Left 放置算法
│   │   │   ├── GeneticOptimizer    # 遗传算法优化排列顺序
│   │   │   └── NestingEngine       # 算法调度引擎（组合BL + GA）
│   │   └── config/                 # 跨域、Swagger等配置
│   └── pom.xml
│
├── panel-nesting-frontend/         # Vue 3 前端
│   ├── src/
│   │   ├── views/
│   │   │   └── NestingView.vue     # 主页面：参数输入 + Canvas预览
│   │   ├── components/
│   │   │   ├── PartInput.vue       # 零件列表输入组件
│   │   │   ├── SheetConfig.vue     # 板材参数配置组件
│   │   │   └── NestingCanvas.vue   # Canvas 2D 排样结果渲染
│   │   └── api/
│   │       └── nesting.ts          # API 调用封装
│   └── package.json
```

## 数据模型

### Sheet（板材）

| 字段 | 类型 | 说明 |
|------|------|------|
| width | double | 板材宽度 (mm) |
| height | double | 板材高度 (mm) |
| margin | double | 边缘留白 (mm) |
| gap | double | 零件间距 (mm) |
| avoidZones | List<AvoidZone> | 避让区域列表 |

AvoidZone: `{ cx, cy, radius }` — 圆形避让区（定位孔）

### Part（零件）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 标识 |
| name | String | 名称 |
| width | double | 宽度 (mm) |
| height | double | 高度 (mm) |
| quantity | int | 数量 |
| rotatable | boolean | 是否允许旋转90° |

### Placement（放置结果）

| 字段 | 类型 | 说明 |
|------|------|------|
| partId | String | 零件ID |
| partName | String | 零件名称 |
| sheetIndex | int | 放在第几块板上（0-based） |
| x | double | 左下角X坐标 |
| y | double | 左下角Y坐标 |
| rotated | boolean | 是否旋转 |
| width | double | 实际占位宽度 |
| height | double | 实际占位高度 |

## 算法设计

### BL（Bottom-Left）放置算法

**输入：** 一块板材 + 一组按顺序排列的零件

**流程：**
1. 将板材可用区域初始化为矩形（扣除 margin 和避让区）
2. 对每个零件，从左下角开始，沿 X 轴逐步右移，碰到障碍则上移一行，重复扫描
3. 找到第一个不与已放置零件、避让区重叠的最低位置
4. 放置零件，标记占用区域

**碰撞检测：** 矩形-矩形相交判断 + 矩形-圆形相交判断（避让孔）

### 遗传算法优化

**目标：** 找到最优的零件排列顺序，使 BL 放置后的材料利用率最高

**编码：** 零件顺序的排列（permutation），展开 quantity 得到实际列表

**遗传操作：**
- 种群大小：50-100
- 选择：锦标赛选择（tournament selection, k=3）
- 交叉：顺序交叉 OX（Order Crossover），保证子代是合法排列
- 变异：交换变异（swap mutation），随机交换两个位置
- 适应度：已用面积 / 总板材面积（越高越好）
- 终止条件：达到最大代数（200代）或连续N代无改善

### 算法引擎调度

```
NestingEngine.nest(request):
  1. 展开零件列表（按 quantity 展开）
  2. 初始化遗传算法种群（随机排列）
  3. 循环 GA 代数:
     a. 对每个个体（排列顺序）执行 BL 放置
     b. 计算适应度（利用率）
     c. 选择 -> 交叉 -> 变异 -> 产生新一代
  4. 返回最优个体的排样结果
```

## API 设计

```
POST /api/nesting/compute
Content-Type: application/json

Request:
{
  "sheet": {
    "width": 1220,
    "height": 2440,
    "margin": 10,
    "gap": 3,
    "avoidZones": [
      {"cx": 50, "cy": 50, "radius": 8},
      {"cx": 1170, "cy": 50, "radius": 8},
      {"cx": 50, "cy": 2390, "radius": 8},
      {"cx": 1170, "cy": 2390, "radius": 8},
      {"cx": 610, "cy": 1220, "radius": 8}
    ]
  },
  "parts": [
    {"name": "P1", "width": 100, "height": 200, "quantity": 10, "rotatable": true},
    {"name": "P2", "width": 150, "height": 150, "quantity": 5, "rotatable": false}
  ]
}

Response:
{
  "totalSheets": 2,
  "utilization": 0.87,
  "sheets": [
    {
      "index": 0,
      "placements": [
        {"partName": "P1", "x": 10, "y": 10, "width": 100, "height": 200, "rotated": false}
      ]
    }
  ]
}
```

## 前端设计

### 技术选型

| 层面 | 选型 | 理由 |
|------|------|------|
| 框架 | Vue 3 + TypeScript | 用户指定 |
| UI库 | Element Plus | Vue 3 生态最成熟 |
| 渲染 | HTML Canvas 2D | 排样结果绘制，性能好 |
| 构建 | Vite | 开发体验好 |
| HTTP | Axios | 标准选择 |

### 页面布局

左右分栏布局：左侧为参数输入区（板材参数 + 零件列表 + 排样按钮），右侧为 Canvas 2D 预览区。

### Canvas 渲染内容

- 板材轮廓（灰色边框 + margin 虚线）
- 避让区域（圆形，半透明红色）
- 已放置零件（矩形色块，标注零件名）
- 零件间距示意（相邻零件间的 gap 线）
- 多板切换（上一块 / 下一块按钮）
- 自动缩放适配

### 零件定位机制

三重机制确保零件列表与 Canvas 中的零件容易对应：

1. **颜色编码：** 每个零件类型分配固定颜色（预定义色板），列表中显示颜色条，Canvas 中同色填充
2. **文字标注：** 每个矩形内部居中绘制零件名称（零件太小时在旁边标注）
3. **联动高亮：** 悬停列表项 → Canvas 中同色实例边框加粗；悬停 Canvas 矩形 → 列表对应项高亮

### 交互流程

1. 用户填写板材参数（默认值：1220x2440mm，margin 10mm，gap 3mm）
2. 用户添加零件列表（名称、尺寸、数量、是否可旋转）
3. 点击"开始排样" → 调用后端 API
4. 返回结果后 Canvas 渲染排样图，显示利用率和板数
5. 可切换查看不同板材的排样结果

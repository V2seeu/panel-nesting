# CNC 拼板排样系统 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现基于 BL+遗传算法的矩形零件排样系统，提供 REST API 和 Canvas 2D 可视化预览。

**Architecture:** Spring Boot 后端提供排样计算 REST API，Vue 3 前端提供参数输入和 Canvas 渲染预览。算法模块纯 Java 实现，不依赖 Spring 框架。前后端通过 JSON API 通信。

**Tech Stack:** Java 21, Spring Boot 3.x, Maven 3.9, Vue 3 + TypeScript, Element Plus, Vite, HTML Canvas 2D, Axios

**Maven path:** 执行 mvn 命令前需要 `export PATH="$PATH:/d/soft/apache-maven-3.9.16/bin"`

---

## File Structure

```
panel-nesting/
├── panel-nesting-backend/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/nesting/
│       │   ├── NestingApplication.java          # Spring Boot 启动类
│       │   ├── config/
│       │   │   └── WebConfig.java               # CORS 跨域配置
│       │   ├── controller/
│       │   │   └── NestingController.java       # REST API 控制器
│       │   ├── model/
│       │   │   ├── AvoidZone.java               # 避让区域
│       │   │   ├── Sheet.java                   # 板材
│       │   │   ├── Part.java                    # 零件
│       │   │   ├── NestingRequest.java          # 排样请求
│       │   │   ├── Placement.java               # 放置结果
│       │   │   ├── SheetResult.java             # 单块板材结果
│       │   │   └── NestingResult.java           # 排样总结果
│       │   └── algorithm/
│       │       ├── BLPlacer.java                # BL 放置算法
│       │       ├── GeneticOptimizer.java        # 遗传算法优化器
│       │       └── NestingEngine.java           # 排样引擎（调度）
│       └── test/java/com/nesting/algorithm/
│           ├── BLPlacerTest.java                # BL 算法单元测试
│           └── NestingEngineTest.java           # 排样引擎集成测试
│
├── panel-nesting-frontend/
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   ├── index.html
│   └── src/
│       ├── main.ts                              # Vue 入口
│       ├── App.vue                              # 根组件
│       ├── api/
│       │   └── nesting.ts                       # API 调用封装
│       ├── types/
│       │   └── index.ts                         # TypeScript 类型定义
│       ├── views/
│       │   └── NestingView.vue                  # 主页面
│       └── components/
│           ├── SheetConfig.vue                  # 板材参数配置
│           ├── PartInput.vue                    # 零件列表输入
│           └── NestingCanvas.vue                # Canvas 排样渲染
│
└── docs/
```

---

### Task 1: 初始化 Spring Boot 后端项目

**Files:**
- Create: `panel-nesting-backend/pom.xml`
- Create: `panel-nesting-backend/src/main/java/com/nesting/NestingApplication.java`
- Create: `panel-nesting-backend/src/main/java/com/nesting/config/WebConfig.java`

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.6</version>
        <relativePath/>
    </parent>
    <groupId>com.nesting</groupId>
    <artifactId>panel-nesting-backend</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>panel-nesting-backend</name>
    <description>CNC Panel Nesting System</description>
    <properties>
        <java.version>21</java.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建 Spring Boot 启动类**

```java
package com.nesting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NestingApplication {
    public static void main(String[] args) {
        SpringApplication.run(NestingApplication.class, args);
    }
}
```

- [ ] **Step 3: 创建 CORS 跨域配置**

```java
package com.nesting.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class WebConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

- [ ] **Step 4: 验证后端能启动**

Run: `cd D:/code/panel-nesting/panel-nesting-backend && export PATH="$PATH:/d/soft/apache-maven-3.9.16/bin" && mvn spring-boot:run`

Expected: 看到 `Started NestingApplication` 日志，启动成功后 Ctrl+C 停止。

- [ ] **Step 5: Commit**

```bash
git add panel-nesting-backend/
git commit -m "feat: initialize Spring Boot backend project"
```

---

### Task 2: 创建后端数据模型

**Files:**
- Create: `panel-nesting-backend/src/main/java/com/nesting/model/AvoidZone.java`
- Create: `panel-nesting-backend/src/main/java/com/nesting/model/Sheet.java`
- Create: `panel-nesting-backend/src/main/java/com/nesting/model/Part.java`
- Create: `panel-nesting-backend/src/main/java/com/nesting/model/Placement.java`
- Create: `panel-nesting-backend/src/main/java/com/nesting/model/SheetResult.java`
- Create: `panel-nesting-backend/src/main/java/com/nesting/model/NestingRequest.java`
- Create: `panel-nesting-backend/src/main/java/com/nesting/model/NestingResult.java`

- [ ] **Step 1: 创建 AvoidZone**

```java
package com.nesting.model;

public record AvoidZone(double cx, double cy, double radius) {}
```

- [ ] **Step 2: 创建 Sheet**

```java
package com.nesting.model;

import java.util.List;

public record Sheet(double width, double height, double margin, double gap, List<AvoidZone> avoidZones) {
    public Sheet {
        if (avoidZones == null) avoidZones = List.of();
    }
}
```

- [ ] **Step 3: 创建 Part**

```java
package com.nesting.model;

public record Part(String id, String name, double width, double height, int quantity, boolean rotatable) {}
```

- [ ] **Step 4: 创建 Placement**

```java
package com.nesting.model;

public record Placement(String partId, String partName, int sheetIndex, double x, double y, boolean rotated, double width, double height) {}
```

- [ ] **Step 5: 创建 SheetResult**

```java
package com.nesting.model;

import java.util.List;

public record SheetResult(int index, List<Placement> placements) {}
```

- [ ] **Step 6: 创建 NestingRequest**

```java
package com.nesting.model;

import java.util.List;

public record NestingRequest(Sheet sheet, List<Part> parts) {}
```

- [ ] **Step 7: 创建 NestingResult**

```java
package com.nesting.model;

import java.util.List;

public record NestingResult(int totalSheets, double utilization, List<SheetResult> sheets) {}
```

- [ ] **Step 8: 验证编译**

Run: `cd D:/code/panel-nesting/panel-nesting-backend && export PATH="$PATH:/d/soft/apache-maven-3.9.16/bin" && mvn compile`

Expected: BUILD SUCCESS

- [ ] **Step 9: Commit**

```bash
git add panel-nesting-backend/src/main/java/com/nesting/model/
git commit -m "feat: add data models for nesting request/response"
```

---

### Task 3: 实现 BL 放置算法

**Files:**
- Create: `panel-nesting-backend/src/main/java/com/nesting/algorithm/BLPlacer.java`
- Create: `panel-nesting-backend/src/test/java/com/nesting/algorithm/BLPlacerTest.java`

- [ ] **Step 1: 编写 BLPlacer 单元测试**

```java
package com.nesting.algorithm;

import com.nesting.model.AvoidZone;
import com.nesting.model.Part;
import com.nesting.model.Placement;
import com.nesting.model.Sheet;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BLPlacerTest {

    private final Sheet standardSheet = new Sheet(1000, 1000, 10, 3, List.of());

    @Test
    void placeSinglePart() {
        Sheet sheet = standardSheet;
        Part part = new Part("p1", "P1", 100, 200, 1, false);
        List<Part> expanded = List.of(part);
        List<Placement> placed = new ArrayList<>();

        BLPlacer.PlacementResult result = BLPlacer.place(sheet, expanded, placed);

        assertEquals(1, result.placements().size());
        Placement p = result.placements().get(0);
        assertEquals("p1", p.partId());
        assertEquals(10, p.x()); // starts at margin
        assertEquals(10, p.y()); // starts at margin
    }

    @Test
    void placeTwoPartsSideBySide() {
        Sheet sheet = standardSheet;
        Part part = new Part("p1", "P1", 100, 100, 2, false);
        List<Part> expanded = List.of(part, new Part("p2", "P2", 100, 100, 1, false));
        List<Placement> placed = new ArrayList<>();

        BLPlacer.PlacementResult result = BLPlacer.place(sheet, expanded, placed);

        assertEquals(2, result.placements().size());
        // Second part should be to the right of first, with gap
        assertTrue(result.placements().get(1).x() > result.placements().get(0).x());
    }

    @Test
    void partThatExceedsSheetGoesToNewSheet() {
        Sheet sheet = new Sheet(200, 200, 10, 3, List.of());
        Part bigPart = new Part("p1", "P1", 150, 150, 3, false);
        List<Part> expanded = List.of(bigPart, bigPart, bigPart);
        List<Placement> placed = new ArrayList<>();

        BLPlacer.PlacementResult result = BLPlacer.place(sheet, expanded, placed);

        // Should need at least 2 sheets for 3 big parts on a small sheet
        assertTrue(result.placements().stream().mapToInt(Placement::sheetIndex).max().orElse(0) >= 1);
    }

    @Test
    void rotatablePartGetsRotatedWhenFitsBetter() {
        Sheet sheet = new Sheet(200, 100, 10, 3, List.of());
        Part part = new Part("p1", "P1", 80, 50, 1, true);
        List<Part> expanded = List.of(part);
        List<Placement> placed = new ArrayList<>();

        BLPlacer.PlacementResult result = BLPlacer.place(sheet, expanded, placed);

        assertEquals(1, result.placements().size());
        // Should fit on the sheet
        assertEquals(0, result.placements().get(0).sheetIndex());
    }

    @Test
    void avoidZoneBlocksPlacement() {
        Sheet sheet = new Sheet(500, 500, 10, 3,
                List.of(new AvoidZone(100, 100, 50)));
        Part part = new Part("p1", "P1", 80, 80, 1, false);
        List<Part> expanded = List.of(part);
        List<Placement> placed = new ArrayList<>();

        BLPlacer.PlacementResult result = BLPlacer.place(sheet, expanded, placed);

        assertEquals(1, result.placements().size());
        // Part should NOT overlap with the avoid zone at (100,100,r=50)
        Placement p = result.placements().get(0);
        boolean overlaps = !(p.x() + p.width() <= 50 || p.x() >= 150 ||
                             p.y() + p.height() <= 50 || p.y() >= 150);
        assertFalse(overlaps, "Part should not overlap with avoid zone");
    }

    @Test
    void gapBetweenParts() {
        Sheet sheet = new Sheet(500, 500, 10, 5, List.of());
        Part part1 = new Part("p1", "P1", 100, 100, 1, false);
        Part part2 = new Part("p2", "P2", 100, 100, 1, false);
        List<Part> expanded = List.of(part1, part2);
        List<Placement> placed = new ArrayList<>();

        BLPlacer.PlacementResult result = BLPlacer.place(sheet, expanded, placed);

        assertEquals(2, result.placements().size());
        Placement a = result.placements().get(0);
        Placement b = result.placements().get(1);
        // If side by side, gap should be >= 5
        if (a.y() == b.y()) {
            assertTrue(Math.abs(b.x() - (a.x() + a.width())) >= 5 ||
                       Math.abs(a.x() - (b.x() + b.width())) >= 5);
        }
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd D:/code/panel-nesting/panel-nesting-backend && export PATH="$PATH:/d/soft/apache-maven-3.9.16/bin" && mvn test -pl . -Dtest=BLPlacerTest`

Expected: 编译失败，BLPlacer 类不存在。

- [ ] **Step 3: 实现 BLPlacer**

```java
package com.nesting.algorithm;

import com.nesting.model.*;

import java.util.ArrayList;
import java.util.List;

public class BLPlacer {

    public record PlacementResult(List<Placement> placements, int sheetsUsed) {}

    /**
     * BL（Bottom-Left）放置算法。
     * 按给定顺序逐个将零件放置到最低最左的可用位置。
     */
    public static PlacementResult place(Sheet sheet, List<Part> parts, List<Placement> existing) {
        List<Placement> placements = new ArrayList<>(existing);
        int sheetsUsed = existing.isEmpty() ? 0 : existing.stream().mapToInt(Placement::sheetIndex).max().orElse(0) + 1;

        for (Part part : parts) {
            boolean placed = false;
            for (int sheetIdx = 0; sheetIdx <= sheetsUsed && !placed; sheetIdx++) {
                if (tryPlaceOnSheet(sheet, part, sheetIdx, placements)) {
                    placed = true;
                }
            }
            if (!placed) {
                sheetsUsed++;
                tryPlaceOnSheet(sheet, part, sheetsUsed - 1, placements);
            }
            sheetsUsed = Math.max(sheetsUsed, placements.stream().mapToInt(Placement::sheetIndex).max().orElse(0) + 1);
        }

        return new PlacementResult(placements, sheetsUsed);
    }

    private static boolean tryPlaceOnSheet(Sheet sheet, Part part, int sheetIdx, List<Placement> placements) {
        double margin = sheet.margin();
        double gap = sheet.gap();
        double innerW = sheet.width() - 2 * margin;
        double innerH = sheet.height() - 2 * margin;

        // Try original orientation first, then rotated if allowed
        boolean[] rotations = part.rotatable() ? new boolean[]{false, true} : new boolean[]{false};

        for (boolean rotated : rotations) {
            double pw = rotated ? part.height() : part.width();
            double ph = rotated ? part.width() : part.height();

            if (pw > innerW || ph > innerH) continue;

            double bestX = Double.MAX_VALUE;
            double bestY = Double.MAX_VALUE;
            boolean found = false;

            // Scan from bottom-left, row by row
            for (double y = margin; y + ph <= sheet.height() - margin; y += 1) {
                for (double x = margin; x + pw <= sheet.width() - margin; x += 1) {
                    Placement candidate = new Placement(part.id(), part.name(), sheetIdx, x, y, rotated, pw, ph);
                    if (!collides(candidate, placements, gap) && !collidesAvoidZone(candidate, sheet.avoidZones(), gap)) {
                        if (y < bestY || (y == bestY && x < bestX)) {
                            bestX = x;
                            bestY = y;
                            found = true;
                        }
                        break; // Found leftmost in this row, no need to scan further right
                    }
                }
                if (found) break; // Found the lowest row, stop scanning up
            }

            if (found) {
                placements.add(new Placement(part.id(), part.name(), sheetIdx, bestX, bestY, rotated, pw, ph));
                return true;
            }
        }
        return false;
    }

    static boolean collides(Placement candidate, List<Placement> placed, double gap) {
        for (Placement p : placed) {
            if (p.sheetIndex() != candidate.sheetIndex()) continue;
            if (rectsOverlap(candidate.x(), candidate.y(), candidate.width(), candidate.height(),
                             p.x(), p.y(), p.width(), p.height(), gap)) {
                return true;
            }
        }
        return false;
    }

    static boolean collidesAvoidZone(Placement candidate, List<AvoidZone> zones, double gap) {
        for (AvoidZone z : zones) {
            if (rectCircleOverlap(candidate.x(), candidate.y(), candidate.width(), candidate.height(),
                                  z.cx(), z.cy(), z.radius(), gap)) {
                return true;
            }
        }
        return false;
    }

    static boolean rectsOverlap(double x1, double y1, double w1, double h1,
                                double x2, double y2, double w2, double h2, double gap) {
        return !(x1 + w1 + gap <= x2 || x2 + w2 + gap <= x1 ||
                 y1 + h1 + gap <= y2 || y2 + h2 + gap <= y1);
    }

    static boolean rectCircleOverlap(double rx, double ry, double rw, double rh,
                                     double cx, double cy, double cr, double gap) {
        // Expand rect by gap, then check circle overlap
        double ex = rx - gap;
        double ey = ry - gap;
        double ew = rw + 2 * gap;
        double eh = rh + 2 * gap;
        double nearestX = Math.max(ex, Math.min(cx, ex + ew));
        double nearestY = Math.max(ey, Math.min(cy, ey + eh));
        double dx = cx - nearestX;
        double dy = cy - nearestY;
        return (dx * dx + dy * dy) <= (cr * cr);
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd D:/code/panel-nesting/panel-nesting-backend && export PATH="$PATH:/d/soft/apache-maven-3.9.16/bin" && mvn test -Dtest=BLPlacerTest`

Expected: 6 tests PASS

- [ ] **Step 5: Commit**

```bash
git add panel-nesting-backend/src/main/java/com/nesting/algorithm/BLPlacer.java panel-nesting-backend/src/test/java/com/nesting/algorithm/BLPlacerTest.java
git commit -m "feat: implement BL placement algorithm with tests"
```

---

### Task 4: 实现遗传算法优化器

**Files:**
- Create: `panel-nesting-backend/src/main/java/com/nesting/algorithm/GeneticOptimizer.java`

- [ ] **Step 1: 实现 GeneticOptimizer**

```java
package com.nesting.algorithm;

import com.nesting.model.Part;
import com.nesting.model.Sheet;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GeneticOptimizer {

    private static final int POPULATION_SIZE = 60;
    private static final int MAX_GENERATIONS = 200;
    private static final double CROSSOVER_RATE = 0.8;
    private static final double MUTATION_RATE = 0.15;
    private static final int TOURNAMENT_K = 3;
    private static final int STAGNATION_LIMIT = 40;

    private final Random random;
    private final Sheet sheet;
    private final List<Part> expandedParts;

    public GeneticOptimizer(Sheet sheet, List<Part> expandedParts) {
        this.sheet = sheet;
        this.expandedParts = expandedParts;
        this.random = new Random(42);
    }

    public record OptimizationResult(int[] bestOrder, double bestFitness, BLPlacer.PlacementResult placementResult) {}

    public OptimizationResult optimize() {
        int n = expandedParts.size();
        List<int[]> population = initializePopulation(n);

        double bestFitness = -1;
        int[] bestOrder = null;
        BLPlacer.PlacementResult bestResult = null;
        int stagnationCount = 0;

        for (int gen = 0; gen < MAX_GENERATIONS; gen++) {
            List<Double> fitnesses = new ArrayList<>();
            double genBest = -1;
            int[] genBestOrder = null;
            BLPlacer.PlacementResult genBestResult = null;

            for (int[] individual : population) {
                List<Part> ordered = reorder(individual);
                BLPlacer.PlacementResult result = BLPlacer.place(sheet, ordered, List.of());
                double fitness = calcFitness(result);
                fitnesses.add(fitness);

                if (fitness > genBest) {
                    genBest = fitness;
                    genBestOrder = individual.clone();
                    genBestResult = result;
                }
            }

            if (genBest > bestFitness) {
                bestFitness = genBest;
                bestOrder = genBestOrder;
                bestResult = genBestResult;
                stagnationCount = 0;
            } else {
                stagnationCount++;
            }

            if (stagnationCount >= STAGNATION_LIMIT) break;

            population = nextGeneration(population, fitnesses);
        }

        if (bestResult == null) {
            // Fallback: evaluate initial random order
            List<Part> ordered = reorder(IntStream.range(0, n).toArray());
            bestResult = BLPlacer.place(sheet, ordered, List.of());
            bestFitness = calcFitness(bestResult);
            bestOrder = IntStream.range(0, n).toArray();
        }

        return new OptimizationResult(bestOrder, bestFitness, bestResult);
    }

    private List<int[]> initializePopulation(int n) {
        List<int[]> pop = new ArrayList<>();
        for (int i = 0; i < POPULATION_SIZE; i++) {
            int[] perm = IntStream.range(0, n).toArray();
            shuffleArray(perm);
            pop.add(perm);
        }
        return pop;
    }

    private void shuffleArray(int[] arr) {
        for (int i = arr.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
        }
    }

    private double calcFitness(BLPlacer.PlacementResult result) {
        if (result.sheetsUsed() == 0) return 0;
        double totalPartArea = expandedParts.stream()
                .mapToDouble(p -> p.width() * p.height())
                .sum();
        double totalSheetArea = sheet.width() * sheet.height() * result.sheetsUsed();
        return totalPartArea / totalSheetArea;
    }

    private List<int[]> nextGeneration(List<int[]> population, List<Double> fitnesses) {
        List<int[]> next = new ArrayList<>();

        // Elitism: keep the best individual
        int bestIdx = 0;
        for (int i = 1; i < fitnesses.size(); i++) {
            if (fitnesses.get(i) > fitnesses.get(bestIdx)) bestIdx = i;
        }
        next.add(population.get(bestIdx).clone());

        while (next.size() < POPULATION_SIZE) {
            int[] parent1 = tournamentSelect(population, fitnesses);
            int[] parent2 = tournamentSelect(population, fitnesses);
            int[] child;
            if (random.nextDouble() < CROSSOVER_RATE) {
                child = orderCrossover(parent1, parent2);
            } else {
                child = parent1.clone();
            }
            if (random.nextDouble() < MUTATION_RATE) {
                swapMutate(child);
            }
            next.add(child);
        }
        return next;
    }

    private int[] tournamentSelect(List<int[]> population, List<Double> fitnesses) {
        int best = random.nextInt(population.size());
        for (int i = 1; i < TOURNAMENT_K; i++) {
            int candidate = random.nextInt(population.size());
            if (fitnesses.get(candidate) > fitnesses.get(best)) best = candidate;
        }
        return population.get(best);
    }

    private int[] orderCrossover(int[] p1, int[] p2) {
        int n = p1.length;
        int[] child = new int[n];
        Arrays.fill(child, -1);

        int start = random.nextInt(n);
        int end = start + random.nextInt(n - start);

        Set<Integer> used = new HashSet<>();
        for (int i = start; i <= end; i++) {
            child[i] = p1[i];
            used.add(p1[i]);
        }

        int pos = (end + 1) % n;
        for (int i = 0; i < n; i++) {
            int idx = (end + 1 + i) % n;
            if (!used.contains(p2[idx])) {
                child[pos] = p2[idx];
                used.add(p2[idx]);
                pos = (pos + 1) % n;
            }
        }
        return child;
    }

    private void swapMutate(int[] individual) {
        int i = random.nextInt(individual.length);
        int j = random.nextInt(individual.length);
        int tmp = individual[i]; individual[i] = individual[j]; individual[j] = tmp;
    }

    private List<Part> reorder(int[] order) {
        return Arrays.stream(order)
                .mapToObj(i -> expandedParts.get(i))
                .collect(Collectors.toList());
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `cd D:/code/panel-nesting/panel-nesting-backend && export PATH="$PATH:/d/soft/apache-maven-3.9.16/bin" && mvn compile`

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add panel-nesting-backend/src/main/java/com/nesting/algorithm/GeneticOptimizer.java
git commit -m "feat: implement genetic algorithm optimizer"
```

---

### Task 5: 实现排样引擎 + 集成测试

**Files:**
- Create: `panel-nesting-backend/src/main/java/com/nesting/algorithm/NestingEngine.java`
- Create: `panel-nesting-backend/src/test/java/com/nesting/algorithm/NestingEngineTest.java`

- [ ] **Step 1: 编写 NestingEngine 集成测试**

```java
package com.nesting.algorithm;

import com.nesting.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NestingEngineTest {

    @Test
    void nestMultiplePartsOnStandardSheet() {
        Sheet sheet = new Sheet(1220, 2440, 10, 3, List.of());
        List<Part> parts = List.of(
            new Part("p1", "P1", 100, 200, 10, true),
            new Part("p2", "P2", 150, 150, 5, false)
        );
        NestingRequest request = new NestingRequest(sheet, parts);

        NestingResult result = NestingEngine.nest(request);

        assertNotNull(result);
        assertTrue(result.totalSheets() >= 1);
        assertTrue(result.utilization() > 0);
        assertEquals(15, result.sheets().stream().mapToInt(s -> s.placements().size()).sum());
    }

    @Test
    void nestWithAvoidZones() {
        Sheet sheet = new Sheet(500, 500, 10, 3,
                List.of(new AvoidZone(250, 250, 20)));
        List<Part> parts = List.of(
            new Part("p1", "P1", 100, 100, 4, false)
        );
        NestingRequest request = new NestingRequest(sheet, parts);

        NestingResult result = NestingEngine.nest(request);

        for (SheetResult sr : result.sheets()) {
            for (Placement p : sr.placements()) {
                boolean overlaps = !(p.x() + p.width() <= 230 || p.x() >= 270 ||
                                     p.y() + p.height() <= 230 || p.y() >= 270);
                assertFalse(overlaps, "Placement should not overlap avoid zone");
            }
        }
    }

    @Test
    void utilizationReasonable() {
        // 5 parts of 200x200 on a 1220x2440 sheet should fit on 1 sheet with decent utilization
        Sheet sheet = new Sheet(1220, 2440, 10, 3, List.of());
        List<Part> parts = List.of(
            new Part("p1", "P1", 200, 200, 5, false)
        );
        NestingRequest request = new NestingRequest(sheet, parts);

        NestingResult result = NestingEngine.nest(request);

        assertEquals(1, result.totalSheets());
        // 5 * 200*200 / (1220*2440) ≈ 0.067, very low but should still be correct
        assertTrue(result.utilization() > 0);
        assertEquals(5, result.sheets().get(0).placements().size());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd D:/code/panel-nesting/panel-nesting-backend && export PATH="$PATH:/d/soft/apache-maven-3.9.16/bin" && mvn test -Dtest=NestingEngineTest`

Expected: 编译失败，NestingEngine 类不存在。

- [ ] **Step 3: 实现 NestingEngine**

```java
package com.nesting.algorithm;

import com.nesting.model.*;

import java.util.ArrayList;
import java.util.List;

public class NestingEngine {

    public static NestingResult nest(NestingRequest request) {
        Sheet sheet = request.sheet();
        List<Part> parts = request.parts();

        // Expand parts by quantity
        List<Part> expanded = new ArrayList<>();
        for (Part p : parts) {
            for (int i = 0; i < p.quantity(); i++) {
                expanded.add(new Part(p.id() + "_" + i, p.name(), p.width(), p.height(), 1, p.rotatable()));
            }
        }

        if (expanded.isEmpty()) {
            return new NestingResult(0, 0, List.of());
        }

        // Use genetic algorithm to find good ordering
        GeneticOptimizer optimizer = new GeneticOptimizer(sheet, expanded);
        GeneticOptimizer.OptimizationResult optResult = optimizer.optimize();

        // Build NestingResult from the best placement
        List<Placement> allPlacements = optResult.placementResult().placements();
        int sheetsUsed = optResult.placementResult().sheetsUsed();

        // Group by sheet index
        List<SheetResult> sheetResults = new ArrayList<>();
        for (int i = 0; i < sheetsUsed; i++) {
            int idx = i;
            List<Placement> sheetPlacements = allPlacements.stream()
                    .filter(p -> p.sheetIndex() == idx)
                    .toList();
            sheetResults.add(new SheetResult(idx, sheetPlacements));
        }

        // Calculate utilization
        double totalPartArea = expanded.stream()
                .mapToDouble(p -> p.width() * p.height())
                .sum();
        double totalSheetArea = sheet.width() * sheet.height() * sheetsUsed;
        double utilization = totalSheetArea > 0 ? totalPartArea / totalSheetArea : 0;

        return new NestingResult(sheetsUsed, utilization, sheetResults);
    }
}
```

- [ ] **Step 4: 运行全部测试**

Run: `cd D:/code/panel-nesting/panel-nesting-backend && export PATH="$PATH:/d/soft/apache-maven-3.9.16/bin" && mvn test`

Expected: All tests PASS

- [ ] **Step 5: Commit**

```bash
git add panel-nesting-backend/src/main/java/com/nesting/algorithm/NestingEngine.java panel-nesting-backend/src/test/java/com/nesting/algorithm/NestingEngineTest.java
git commit -m "feat: implement nesting engine with integration tests"
```

---

### Task 6: 实现 REST API 控制器

**Files:**
- Create: `panel-nesting-backend/src/main/java/com/nesting/controller/NestingController.java`

- [ ] **Step 1: 实现 NestingController**

```java
package com.nesting.controller;

import com.nesting.algorithm.NestingEngine;
import com.nesting.model.NestingRequest;
import com.nesting.model.NestingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/nesting")
public class NestingController {

    @PostMapping("/compute")
    public NestingResult compute(@RequestBody NestingRequest request) {
        return NestingEngine.nest(request);
    }
}
```

- [ ] **Step 2: 启动后端并手动测试 API**

Run: `cd D:/code/panel-nesting/panel-nesting-backend && export PATH="$PATH:/d/soft/apache-maven-3.9.16/bin" && mvn spring-boot:run`

然后在新终端测试：

```bash
curl -X POST http://localhost:8080/api/nesting/compute \
  -H "Content-Type: application/json" \
  -d '{"sheet":{"width":1220,"height":2440,"margin":10,"gap":3,"avoidZones":[]},"parts":[{"id":"p1","name":"P1","width":100,"height":200,"quantity":3,"rotatable":true}]}'
```

Expected: 返回 JSON 格式的排样结果，包含 totalSheets、utilization、sheets 等字段。

- [ ] **Step 3: Commit**

```bash
git add panel-nesting-backend/src/main/java/com/nesting/controller/NestingController.java
git commit -m "feat: add REST API endpoint for nesting computation"
```

---

### Task 7: 初始化 Vue 3 前端项目

**Files:**
- Create: `panel-nesting-frontend/` (via create-vue scaffold)

- [ ] **Step 1: 用 npm create vue 脚手架初始化**

Run:
```bash
cd D:/code/panel-nesting && npm create vue@latest panel-nesting-frontend -- --typescript --router=false --pinia=false --vitest=false --e2e=false --eslint=false --prettier=false
```

当交互提示时选择默认值。如果脚手架命令需要交互式输入，则改用以下方式：

```bash
cd D:/code/panel-nesting && mkdir -p panel-nesting-frontend/src/{views,components,api,types} && mkdir -p panel-nesting-frontend/public
```

然后手动创建 `package.json`：

```json
{
  "name": "panel-nesting-frontend",
  "version": "0.0.1",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc && vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "vue": "^3.5.13",
    "element-plus": "^2.9.1",
    "axios": "^1.7.9"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.2.1",
    "typescript": "~5.6.3",
    "vite": "^6.0.5",
    "vue-tsc": "^2.2.0"
  }
}
```

- [ ] **Step 2: 创建 vite.config.ts**

```typescript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

- [ ] **Step 3: 创建 tsconfig.json**

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "module": "ESNext",
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "isolatedModules": true,
    "moduleDetection": "force",
    "noEmit": true,
    "jsx": "preserve",
    "strict": true,
    "noUnusedLocals": false,
    "noUnusedParameters": false,
    "noFallthroughCasesInSwitch": true,
    "paths": {
      "@/*": ["./src/*"]
    }
  },
  "include": ["src/**/*.ts", "src/**/*.tsx", "src/**/*.vue", "env.d.ts"]
}
```

- [ ] **Step 4: 创建 env.d.ts**

```typescript
/// <reference types="vite/client" />
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}
```

- [ ] **Step 5: 创建 index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>CNC 拼板排样系统</title>
</head>
<body>
  <div id="app"></div>
  <script type="module" src="/src/main.ts"></script>
</body>
</html>
```

- [ ] **Step 6: 创建 src/main.ts**

```typescript
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'

const app = createApp(App)
app.use(ElementPlus)
app.mount('#app')
```

- [ ] **Step 7: 创建 src/App.vue**

```vue
<template>
  <NestingView />
</template>

<script setup lang="ts">
import NestingView from './views/NestingView.vue'
</script>

<style>
body {
  margin: 0;
  padding: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}
</style>
```

- [ ] **Step 8: 安装依赖并验证启动**

Run: `cd D:/code/panel-nesting/panel-nesting-frontend && npm install && npm run dev`

Expected: Vite dev server 启动在 http://localhost:5173，页面显示空白（因为 NestingView 还未创建）。

Ctrl+C 停止。

- [ ] **Step 9: Commit**

```bash
git add panel-nesting-frontend/
git commit -m "feat: initialize Vue 3 frontend project"
```

---

### Task 8: 实现前端类型定义和 API 封装

**Files:**
- Create: `panel-nesting-frontend/src/types/index.ts`
- Create: `panel-nesting-frontend/src/api/nesting.ts`

- [ ] **Step 1: 创建类型定义**

```typescript
// types/index.ts
export interface AvoidZone {
  cx: number
  cy: number
  radius: number
}

export interface Sheet {
  width: number
  height: number
  margin: number
  gap: number
  avoidZones: AvoidZone[]
}

export interface Part {
  id: string
  name: string
  width: number
  height: number
  quantity: number
  rotatable: boolean
}

export interface Placement {
  partId: string
  partName: string
  sheetIndex: number
  x: number
  y: number
  rotated: boolean
  width: number
  height: number
}

export interface SheetResult {
  index: number
  placements: Placement[]
}

export interface NestingResult {
  totalSheets: number
  utilization: number
  sheets: SheetResult[]
}

export interface NestingRequest {
  sheet: Sheet
  parts: Part[]
}
```

- [ ] **Step 2: 创建 API 封装**

```typescript
// api/nesting.ts
import axios from 'axios'
import type { NestingRequest, NestingResult } from '../types'

export async function computeNesting(request: NestingRequest): Promise<NestingResult> {
  const response = await axios.post<NestingResult>('/api/nesting/compute', request)
  return response.data
}
```

- [ ] **Step 3: Commit**

```bash
git add panel-nesting-frontend/src/types/ panel-nesting-frontend/src/api/
git commit -m "feat: add frontend types and API client"
```

---

### Task 9: 实现板材参数配置组件 (SheetConfig)

**Files:**
- Create: `panel-nesting-frontend/src/components/SheetConfig.vue`

- [ ] **Step 1: 实现 SheetConfig 组件**

```vue
<template>
  <el-card header="板材参数">
    <el-form :model="sheet" label-width="80px" size="small">
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="宽度(mm)">
            <el-input-number v-model="sheet.width" :min="100" :max="5000" :step="10" style="width:100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="高度(mm)">
            <el-input-number v-model="sheet.height" :min="100" :max="5000" :step="10" style="width:100%" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="留白(mm)">
            <el-input-number v-model="sheet.margin" :min="0" :max="100" :step="1" style="width:100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="间距(mm)">
            <el-input-number v-model="sheet.gap" :min="0" :max="50" :step="0.5" style="width:100%" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-divider content-position="left">避让区域（定位孔）</el-divider>
      <div v-for="(zone, idx) in sheet.avoidZones" :key="idx" style="display:flex;gap:8px;margin-bottom:8px;align-items:center">
        <el-input-number v-model="zone.cx" :min="0" placeholder="X" size="small" style="width:25%" />
        <el-input-number v-model="zone.cy" :min="0" placeholder="Y" size="small" style="width:25%" />
        <el-input-number v-model="zone.radius" :min="1" placeholder="半径" size="small" style="width:25%" />
        <el-button type="danger" :icon="Delete" circle size="small" @click="sheet.avoidZones.splice(idx, 1)" />
      </div>
      <el-button type="primary" link size="small" @click="sheet.avoidZones.push({ cx: 0, cy: 0, radius: 8 })">
        + 添加避让区域
      </el-button>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { Delete } from '@element-plus/icons-vue'
import type { Sheet } from '../types'

const sheet = defineModel<Sheet>({ required: true })
</script>
```

- [ ] **Step 2: Commit**

```bash
git add panel-nesting-frontend/src/components/SheetConfig.vue
git commit -m "feat: add SheetConfig component"
```

---

### Task 10: 实现零件列表输入组件 (PartInput)

**Files:**
- Create: `panel-nesting-frontend/src/components/PartInput.vue`

- [ ] **Step 1: 实现 PartInput 组件**

```vue
<template>
  <el-card header="零件列表">
    <div v-for="(part, idx) in parts" :key="idx" class="part-item">
      <div class="part-color-bar" :style="{ backgroundColor: colors[idx % colors.length] }"></div>
      <div class="part-fields">
        <el-row :gutter="8">
          <el-col :span="6">
            <el-input v-model="part.name" placeholder="名称" size="small" />
          </el-col>
          <el-col :span="5">
            <el-input-number v-model="part.width" :min="1" placeholder="宽" size="small" style="width:100%" />
          </el-col>
          <el-col :span="5">
            <el-input-number v-model="part.height" :min="1" placeholder="高" size="small" style="width:100%" />
          </el-col>
          <el-col :span="4">
            <el-input-number v-model="part.quantity" :min="1" placeholder="数量" size="small" style="width:100%" />
          </el-col>
          <el-col :span="2">
            <el-checkbox v-model="part.rotatable" size="small">旋转</el-checkbox>
          </el-col>
          <el-col :span="2">
            <el-button type="danger" :icon="Delete" circle size="small" @click="parts.splice(idx, 1)" />
          </el-col>
        </el-row>
      </div>
    </div>
    <el-button type="primary" link @click="addPart">+ 添加零件</el-button>
  </el-card>
</template>

<script setup lang="ts">
import { Delete } from '@element-plus/icons-vue'
import type { Part } from '../types'

const parts = defineModel<Part[]>({ required: true })

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
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add panel-nesting-frontend/src/components/PartInput.vue
git commit -m "feat: add PartInput component with color coding"
```

---

### Task 11: 实现 Canvas 排样渲染组件 (NestingCanvas)

**Files:**
- Create: `panel-nesting-frontend/src/components/NestingCanvas.vue`

- [ ] **Step 1: 实现 NestingCanvas 组件**

```vue
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
```

- [ ] **Step 2: Commit**

```bash
git add panel-nesting-frontend/src/components/NestingCanvas.vue
git commit -m "feat: add NestingCanvas component with hover interaction"
```

---

### Task 12: 组装主页面 (NestingView)

**Files:**
- Create: `panel-nesting-frontend/src/views/NestingView.vue`

- [ ] **Step 1: 实现 NestingView 主页面**

```vue
<template>
  <div class="nesting-page">
    <h1 class="page-title">CNC 拼板排样系统</h1>
    <div class="main-layout">
      <div class="left-panel">
        <SheetConfig v-model="sheet" />
        <PartInput v-model="parts" />
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
```

- [ ] **Step 2: 启动前后端联调验证**

终端 1 - 启动后端：
```bash
cd D:/code/panel-nesting/panel-nesting-backend && export PATH="$PATH:/d/soft/apache-maven-3.9.16/bin" && mvn spring-boot:run
```

终端 2 - 启动前端：
```bash
cd D:/code/panel-nesting/panel-nesting-frontend && npm run dev
```

浏览器打开 http://localhost:5173：
1. 确认页面布局正确（左侧参数，右侧空白 Canvas）
2. 点击"开始排样"
3. 确认 Canvas 显示排样结果，零件带颜色编码和名称标注
4. 确认鼠标悬停零件能高亮

- [ ] **Step 3: Commit**

```bash
git add panel-nesting-frontend/src/views/NestingView.vue
git commit -m "feat: add NestingView main page with full interaction"
```

---

### Task 13: 添加零件列表悬停联动高亮

**Files:**
- Modify: `panel-nesting-frontend/src/components/PartInput.vue`
- Modify: `panel-nesting-frontend/src/views/NestingView.vue`

- [ ] **Step 1: 给 PartInput 添加悬停事件**

在 PartInput.vue 中，将 `<div class="part-item">` 改为响应悬停：

```vue
<div class="part-item"
     :class="{ 'part-item--highlighted': hoveredPartName === part.name }"
     @mouseenter="emit('hoverPart', part.name)"
     @mouseleave="emit('hoverPart', null)">
```

在 `<script setup>` 中添加：

```typescript
defineProps<{
  hoveredPartName: string | null
}>()
const emit = defineEmits<{
  hoverPart: [name: string | null]
}>()
```

在 `<style scoped>` 中添加：

```css
.part-item--highlighted {
  background-color: #ecf5ff;
  border-radius: 4px;
}
.part-item--highlighted .part-color-bar {
  box-shadow: 0 0 6px 2px currentColor;
}
```

- [ ] **Step 2: 在 NestingView 中传递联动状态**

更新 NestingView.vue 的 PartInput 引用：

```vue
<PartInput v-model="parts" :hoveredPartName="hoveredPartName" @hoverPart="onCanvasHover" />
```

- [ ] **Step 3: 启动验证联动**

前后端都启动后：
1. 鼠标悬停左侧零件列表的 P1 → Canvas 中所有 P1 边框加粗
2. 鼠标悬停 Canvas 中的零件 → 左侧列表对应项高亮

- [ ] **Step 4: Commit**

```bash
git add panel-nesting-frontend/src/components/PartInput.vue panel-nesting-frontend/src/views/NestingView.vue
git commit -m "feat: add bidirectional hover highlight between part list and canvas"
```

---

## Self-Review

**Spec coverage:**
- 矩形排样: Task 3 (BLPlacer), Task 4 (GeneticOptimizer), Task 5 (NestingEngine)
- 板材约束（尺寸/留白/间距/避让孔）: Task 3 (BLPlacer 碰撞检测)
- 零件方向约束: Task 3 (rotatable 参数处理)
- 材料利用率最大化: Task 4 (GA 适应度函数)
- REST API: Task 6
- Vue 前端: Task 7-8
- Canvas 预览: Task 11
- 颜色编码: Task 10, 11
- 文字标注: Task 11 (draw 函数中)
- 联动高亮: Task 13

**Placeholder scan:** 无 TBD/TODO。

**Type consistency:** 所有接口字段名在 model/ 和 types/ 中保持一致（sheetIndex, partName, partId 等）。

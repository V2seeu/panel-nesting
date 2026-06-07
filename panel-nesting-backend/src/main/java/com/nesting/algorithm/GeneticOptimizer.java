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

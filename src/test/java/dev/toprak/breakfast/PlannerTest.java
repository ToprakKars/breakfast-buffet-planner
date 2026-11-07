package dev.toprak.breakfast;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlannerTest {

    private final MenuItem eggs = new MenuItem("Scrambled eggs", Category.HOT, "kg", 0.1, 0.05, 2.5);
    private final MenuItem rolls = new MenuItem("Bread rolls", Category.BAKERY, "pcs", 1.5, 1, 60);
    private final Planner planner = new Planner(0.1);

    @Test
    @DisplayName("needed amount = guests × portion + safety buffer")
    void calculatesNeededAmount() {
        PlanLine line = planner.planItem(eggs, 100, 20, 0);
        // (100 × 0.1 + 20 × 0.05) × 1.1 = 12.1 kg
        assertEquals(12.1, line.needed(), 0.001);
        assertEquals(12.1, line.toPrepare(), 0.001);
    }

    @Test
    @DisplayName("stock is subtracted and packs are rounded up")
    void usesStockAndRoundsPacksUp() {
        PlanLine line = planner.planItem(rolls, 100, 20, 60);
        // needed = (150 + 20) × 1.1 = 187, prepare 127 → 3 packs of 60
        assertEquals(187, line.needed(), 0.001);
        assertEquals(127, line.toPrepare(), 0.001);
        assertEquals(3, line.packsToOrder());
    }

    @Test
    @DisplayName("more stock than needed means nothing to prepare")
    void neverNegative() {
        PlanLine line = planner.planItem(eggs, 10, 0, 50);
        assertEquals(0, line.toPrepare());
        assertEquals(0, line.packsToOrder());
    }

    @Test
    @DisplayName("exact pack sizes don't order an extra pack")
    void exactPack() {
        Planner noBuffer = new Planner(0);
        PlanLine line = noBuffer.planItem(rolls, 40, 0, 0); // 60 rolls = exactly 1 pack
        assertEquals(1, line.packsToOrder());
    }

    @Test
    @DisplayName("items counted in pieces are rounded up to whole numbers")
    void piecesAreWholeNumbers() {
        PlanLine line = planner.planItem(rolls, 3, 1, 0); // (4.5 + 1) × 1.1 = 6.05 → 7
        assertEquals(7, line.needed());
    }

    @Test
    @DisplayName("plan() uses stock per item name")
    void planWholeMenu() {
        List<PlanLine> lines = planner.plan(List.of(eggs, rolls), 50, 0, Map.of("Bread rolls", 20.0));
        assertEquals(2, lines.size());
        assertEquals(0, lines.get(0).inStock());
        assertEquals(20, lines.get(1).inStock());
    }

    @Test
    @DisplayName("refill schedule adds up to the total and peaks around 08:00")
    void refillSchedule() {
        Map<LocalTime, Double> schedule = Planner.refillSchedule(12.0, LocalTime.of(6, 30), LocalTime.of(10, 30));
        assertEquals(8, schedule.size());
        double sum = schedule.values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(12.0, sum, 0.001);

        LocalTime busiest = schedule.entrySet().stream()
                .max(Map.Entry.comparingByValue()).orElseThrow().getKey();
        assertTrue(busiest.equals(LocalTime.of(8, 0)) || busiest.equals(LocalTime.of(7, 30)),
                "busiest slot was " + busiest);
        assertTrue(schedule.get(LocalTime.of(6, 30)) < schedule.get(LocalTime.of(8, 0)));
    }

    @Test
    @DisplayName("invalid input is rejected")
    void validation() {
        assertThrows(IllegalArgumentException.class, () -> new Planner(1.5));
        assertThrows(IllegalArgumentException.class, () -> planner.planItem(eggs, -1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new MenuItem("X", Category.HOT, "kg", 1, 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> Planner.refillSchedule(1, LocalTime.of(10, 0), LocalTime.of(9, 0)));
    }

    @Test
    @DisplayName("CSV menu is parsed and bad lines report their line number")
    void csv() {
        List<MenuItem> menu = CsvLoader.parseMenu(List.of(
                "name,category,unit,perAdult,perChild,packSize",
                "Bacon,hot,kg,0.04,0.02,1",
                "# comment",
                ""));
        assertEquals(1, menu.size());
        assertEquals(Category.HOT, menu.get(0).category());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                CsvLoader.parseMenu(List.of("header", "Bacon,HOT,kg,abc,0.02,1")));
        assertTrue(e.getMessage().contains("Line 2"));
    }
}

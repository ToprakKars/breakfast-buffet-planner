package dev.toprak.breakfast;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calculates how much food a breakfast buffet needs and when hot items should be refilled.
 */
public class Planner {

    /** Extra amount on top of the average, so the buffet doesn't run empty at the end. */
    private final double safetyBuffer;

    public Planner(double safetyBuffer) {
        if (safetyBuffer < 0 || safetyBuffer > 1) {
            throw new IllegalArgumentException("safetyBuffer must be between 0 and 1");
        }
        this.safetyBuffer = safetyBuffer;
    }

    public PlanLine planItem(MenuItem item, int adults, int children, double inStock) {
        if (adults < 0 || children < 0) throw new IllegalArgumentException("guest numbers must not be negative");
        double needed = round((adults * item.perAdult() + children * item.perChild()) * (1 + safetyBuffer));
        if (item.isCounted()) needed = Math.ceil(needed); // you can't serve 0.8 croissants
        double toPrepare = round(Math.max(0, needed - inStock));
        int packs = (int) Math.ceil(toPrepare / item.packSize() - 1e-9);
        return new PlanLine(item, needed, inStock, toPrepare, packs);
    }

    public List<PlanLine> plan(List<MenuItem> menu, int adults, int children, Map<String, Double> stock) {
        List<PlanLine> lines = new ArrayList<>();
        for (MenuItem item : menu) {
            lines.add(planItem(item, adults, children, stock.getOrDefault(item.name(), 0.0)));
        }
        return lines;
    }

    /**
     * Splits the total amount of an item over the service in 30-minute slots,
     * following a typical hotel breakfast curve (quiet start, rush between 07:30 and 09:00).
     * The amounts always add up to the total.
     */
    public static Map<LocalTime, Double> refillSchedule(double total, LocalTime open, LocalTime close) {
        if (!close.isAfter(open)) throw new IllegalArgumentException("close must be after open");
        List<LocalTime> slots = new ArrayList<>();
        for (LocalTime t = open; t.isBefore(close); t = t.plusMinutes(30)) slots.add(t);

        double[] weights = new double[slots.size()];
        double sum = 0;
        for (int i = 0; i < slots.size(); i++) {
            weights[i] = demandWeight(slots.get(i));
            sum += weights[i];
        }

        Map<LocalTime, Double> schedule = new LinkedHashMap<>();
        double assigned = 0;
        for (int i = 0; i < slots.size(); i++) {
            double amount = (i == slots.size() - 1) ? round(total - assigned) : round(total * weights[i] / sum);
            assigned += amount;
            schedule.put(slots.get(i), amount);
        }
        return schedule;
    }

    /** Relative number of guests arriving around a given time. */
    static double demandWeight(LocalTime time) {
        double hour = time.getHour() + time.getMinute() / 60.0 + 0.25; // middle of the 30-min slot
        double peak = 8.25;                                              // 08:15
        double spread = 0.9;
        return Math.exp(-Math.pow(hour - peak, 2) / (2 * spread * spread));
    }

    static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

package dev.toprak.breakfast;

import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Command-line entry point.
 *
 * <pre>
 * java -jar target/breakfast-buffet-planner-1.0.0.jar --adults 180 --children 25 \
 *      --menu menu.csv --stock stock.csv --open 06:30 --close 10:30
 * </pre>
 */
public class App {

    public static void main(String[] args) throws Exception {
        Map<String, String> opts = parseArgs(args);
        int adults = Integer.parseInt(opts.getOrDefault("adults", "150"));
        int children = Integer.parseInt(opts.getOrDefault("children", "20"));
        double buffer = Double.parseDouble(opts.getOrDefault("buffer", "0.1"));
        LocalTime open = LocalTime.parse(opts.getOrDefault("open", "06:30"));
        LocalTime close = LocalTime.parse(opts.getOrDefault("close", "10:30"));

        List<MenuItem> menu = CsvLoader.parseMenu(CsvLoader.read(Path.of(opts.getOrDefault("menu", "menu.csv"))));
        Map<String, Double> stock = opts.containsKey("stock")
                ? CsvLoader.parseStock(CsvLoader.read(Path.of(opts.get("stock"))))
                : Map.of();

        Planner planner = new Planner(buffer);
        List<PlanLine> lines = planner.plan(menu, adults, children, stock);

        System.out.printf(Locale.ROOT, "%nBreakfast plan for %d adults + %d children (buffer %.0f%%)%n%n",
                adults, children, buffer * 100);
        System.out.printf("%-16s %-7s %12s %12s %12s %6s%n", "Item", "Cat.", "Needed", "In stock", "Prepare", "Packs");
        System.out.println("-".repeat(70));
        for (PlanLine l : lines) {
            System.out.printf(Locale.ROOT, "%-16s %-7s %12s %12s %12s %6d%n",
                    l.item().name(), l.item().category(),
                    amount(l.needed(), l.item()), amount(l.inStock(), l.item()),
                    amount(l.toPrepare(), l.item()), l.packsToOrder());
        }

        System.out.printf("%nRefill plan for hot food and bakery (%s - %s)%n%n", open, close);
        System.out.printf("%-16s", "");
        Planner.refillSchedule(1, open, close).keySet().forEach(t -> System.out.printf(" %6s", t));
        System.out.println();
        for (PlanLine l : lines) {
            if (!l.item().isRefilledInBatches()) continue;
            System.out.printf("%-16s", l.item().name());
            Planner.refillSchedule(l.needed(), open, close).values().forEach(v ->
                    System.out.printf(Locale.ROOT, l.item().isCounted() ? " %6.0f" : " %6.1f", v));
            System.out.println("  " + l.item().unit());
        }
    }

    static String amount(double value, MenuItem item) {
        String number = item.isCounted()
                ? String.format(Locale.ROOT, "%.0f", value)
                : String.format(Locale.ROOT, "%.2f", value);
        return number + " " + item.unit();
    }

    static Map<String, String> parseArgs(String[] args) {
        Map<String, String> opts = new java.util.HashMap<>();
        for (int i = 0; i < args.length - 1; i += 2) {
            if (!args[i].startsWith("--")) throw new IllegalArgumentException("Unexpected argument: " + args[i]);
            opts.put(args[i].substring(2), args[i + 1]);
        }
        return opts;
    }
}

package dev.toprak.breakfast;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Reads the menu and the current stock from simple CSV files. */
public final class CsvLoader {

    private CsvLoader() {
    }

    /** Format: name,category,unit,perAdult,perChild,packSize (first line is a header). */
    public static List<MenuItem> parseMenu(List<String> lines) {
        List<MenuItem> menu = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] p = line.split(",");
            if (p.length != 6) throw new IllegalArgumentException("Line " + (i + 1) + ": expected 6 columns");
            try {
                menu.add(new MenuItem(p[0].strip(), Category.parse(p[1]), p[2].strip(),
                        Double.parseDouble(p[3]), Double.parseDouble(p[4]), Double.parseDouble(p[5])));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Line " + (i + 1) + ": not a number", e);
            }
        }
        return menu;
    }

    /** Format: name,amount (first line is a header). */
    public static Map<String, Double> parseStock(List<String> lines) {
        Map<String, Double> stock = new HashMap<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] p = line.split(",");
            stock.put(p[0].strip(), Double.parseDouble(p[1]));
        }
        return stock;
    }

    public static List<String> read(Path path) throws IOException {
        return Files.readAllLines(path, StandardCharsets.UTF_8);
    }
}

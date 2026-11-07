# Breakfast Buffet Planner ☕🥐

A Java command-line tool that answers the questions every hotel breakfast shift starts with:

- **How much of each item do we need** for tomorrow's guests?
- **What do we still have to prepare or order**, after counting what's already in stock?
- **When should the hot food be refilled**, so it's fresh during the rush and nothing is wasted at the end?

The idea comes from my own job in hotel breakfast service in Berlin.
The portions in `menu.csv` are example values, not real data from any hotel.

## Example

```bash
mvn package
java -jar target/breakfast-buffet-planner-1.0.0.jar --adults 180 --children 25 --menu menu.csv --stock stock.csv
```

```
Breakfast plan for 180 adults + 25 children (buffer 10%)

Item             Cat.          Needed     In stock      Prepare  Packs
----------------------------------------------------------------------
Scrambled eggs   HOT         19.20 kg      0.00 kg     19.20 kg      8
Bacon            HOT          8.47 kg      0.00 kg      8.47 kg      9
Pancakes         HOT          152 pcs        0 pcs      152 pcs      4
Bread rolls      BAKERY       300 pcs       60 pcs      240 pcs      4
Croissants       BAKERY       116 pcs        0 pcs      116 pcs      3
Cheese           COLD         7.34 kg      3.50 kg      3.84 kg      4
...

Refill plan for hot food and bakery (06:30 - 10:30)

                  06:30  07:00  07:30  08:00  08:30  09:00  09:30  10:00
Scrambled eggs      1.1    2.4    3.8    4.4    3.8    2.4    1.1    0.4  kg
Bacon               0.5    1.0    1.7    1.9    1.7    1.0    0.5    0.2  kg
Pancakes              9     19     30     35     30     19      9      3  pcs
Bread rolls          17     37     59     69     59     37     17      6  pcs
```

## How it works

- **Amount needed** = adults × portion per adult + children × portion per child, plus a safety buffer (default 10 %).
  Items counted in pieces are rounded up – you can't put 0.8 croissants on the buffet.
- **To prepare** = needed − stock (never below zero). **Packs** are rounded up to the pack size.
- **Refill plan:** guests don't arrive evenly. The service is split into 30-minute slots and the total is
  spread along a bell curve that peaks around 08:15. The slots always add up exactly to the total.

## Options

| Option | Default | Meaning |
|---|---|---|
| `--adults` | 150 | number of adult guests |
| `--children` | 20 | number of children |
| `--buffer` | 0.1 | safety buffer (0.1 = 10 %) |
| `--menu` | menu.csv | menu file |
| `--stock` | – | current stock (optional) |
| `--open` / `--close` | 06:30 / 10:30 | breakfast service times |

### `menu.csv`

```
name,category,unit,perAdult,perChild,packSize
Scrambled eggs,HOT,kg,0.09,0.05,2.5
Bread rolls,BAKERY,pcs,1.4,0.8,60
```

Categories: `HOT`, `BAKERY`, `COLD`, `FRUIT`, `DRINKS`. Hot food and bakery get a refill plan.

## Tech

- Java 21 (records, enums, streams), no runtime dependencies
- Maven build, runnable JAR
- JUnit 5 – 9 tests (calculation, stock, pack rounding, refill curve, validation, CSV errors with line numbers)
- GitHub Actions runs the tests on every push

```
src/main/java/dev/toprak/breakfast/
  App.java        command-line interface and table output
  Planner.java    calculations and refill schedule
  MenuItem.java   one buffet item (record)
  PlanLine.java   result for one item (record)
  Category.java   buffet categories (enum)
  CsvLoader.java  reads menu and stock files
src/test/java/dev/toprak/breakfast/
  PlannerTest.java
```

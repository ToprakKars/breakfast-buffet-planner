package dev.toprak.breakfast;

/**
 * Result for one item: how much is needed, how much is already in stock,
 * how much still has to be prepared and how many packs to order.
 */
public record PlanLine(MenuItem item, double needed, double inStock, double toPrepare, int packsToOrder) {
}

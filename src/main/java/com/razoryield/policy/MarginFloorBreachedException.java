package com.razoryield.policy;

/**
 * Thrown when a proposed offer price would sell below the merchant's minimum acceptable margin.
 */
public class MarginFloorBreachedException extends RuntimeException {

    private final String sku;
    private final long floorPricePaise;
    private final long offerPricePaise;

    public MarginFloorBreachedException(String sku, long floorPricePaise, long offerPricePaise) {
        super("Margin floor breached for %s: floor price is %d paise but the proposed offer price is %d paise."
                .formatted(sku, floorPricePaise, offerPricePaise));
        this.sku = sku;
        this.floorPricePaise = floorPricePaise;
        this.offerPricePaise = offerPricePaise;
    }

    public String getSku() {
        return sku;
    }

    public long getFloorPricePaise() {
        return floorPricePaise;
    }

    public long getOfferPricePaise() {
        return offerPricePaise;
    }
}

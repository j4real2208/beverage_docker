package com.online.store.beverageservice;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class Bottle implements Beverage {
    private int id;
    private String name;
    private double volume;
    private boolean isAlcoholic;
    private double volumePercent;
    private double price;
    private String supplier;
    private int inStock;

    @Override
    public int getId() { return id; }
    @Override
    public int getInStock() { return inStock; }
    @Override
    public double getPrice() { return price; }
}

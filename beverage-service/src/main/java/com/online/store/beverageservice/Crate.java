package com.online.store.beverageservice;

import lombok.Data;

@Data
public class Crate implements Beverage {
    private int id;
    private Bottle bottle;
    private int noOfBottles;
    private double price;
    private int inStock;

    @Override
    public int getId() { return id; }
    @Override
    public int getInStock() { return inStock; }
    @Override
    public double getPrice() { return price; }
}

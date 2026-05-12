package controllers;

import entities.OfferFilter;

public interface OfferFilterAware {
    void applyFilter(OfferFilter filter);
}
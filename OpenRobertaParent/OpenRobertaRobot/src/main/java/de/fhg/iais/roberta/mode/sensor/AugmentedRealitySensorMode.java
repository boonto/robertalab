package de.fhg.iais.roberta.mode.sensor;

import de.fhg.iais.roberta.inter.mode.sensor.IAugmentedRealitySensorMode;

public enum AugmentedRealitySensorMode implements IAugmentedRealitySensorMode {
    DEFAULT, DISTANCE( "Distance" );

    private final String[] values;

    private AugmentedRealitySensorMode(String... values) {
        this.values = values;
    }

    @Override
    public String[] getValues() {
        return this.values;
    }

}
package com.affectiva.framedetectordemo;

/**
 * An enum representing all metrics currently available in the Affectiva SDK
 */
public enum Metrics {
    //Emotions
    ANGER,
    DISGUST,
    FEAR,
    JOY,
    SADNESS,
    SURPRISE,
    CONTEMPT,
    ENGAGEMENT,
    VALENCE,

    //Expressions
    ATTENTION,
    BROW_FURROW,
    BROW_RAISE,
    CHIN_RAISER,
    EYE_CLOSURE,
    INNER_BROW_RAISER,
    LIP_DEPRESSOR,
    LIP_PRESS,
    LIP_PUCKER,
    LIP_SUCK,
    MOUTH_OPEN,
    NOSE_WRINKLER,
    SMILE,
    SMIRK,
    UPPER_LIP_RAISER,

    //Measurements
    YAW,
    PITCH,
    ROLL,
    INTER_OCULAR_DISTANCE;

    String getUpperCaseName() {
        return toString().replace("_"," ");
    }

    String getLowerCaseName() {
        return toString().toLowerCase();
    }

    static int numberOfEmotions() {
        return ATTENTION.ordinal();
    }

    static int numberOfMeasurements() {
        return Metrics.values().length - numberOfEmotions() - numberOfExpressions();
    }

    static int numberOfExpressions() {
        return YAW.ordinal() - numberOfEmotions();
    }

    static int numberOfMetrics() {
        return Metrics.values().length;
    }

    /**
     * Returns an array to allow for iteration through all Emotions
     */
    static Metrics[] getEmotions() {
        Metrics[] emotions = new Metrics[numberOfEmotions()];
        Metrics[] allMetrics = Metrics.values();
        for (int n = 0; n < numberOfEmotions(); n++) {
            emotions[n] = allMetrics[n];
        }
        return emotions;
    }

    /**
     * Returns an array to allow for iteration through all Expressions
     */
    static Metrics[] getExpressions() {
        Metrics[] expressions = new Metrics[numberOfExpressions()];
        Metrics[] allMetrics = Metrics.values();
        for (int n = 0; n < numberOfExpressions(); n++) {
            expressions[n] = allMetrics[n + numberOfEmotions()];
        }
        return expressions;
    }

    static Metrics[] getMeasurements() {
        Metrics[] measurements = new Metrics[numberOfMeasurements()];
        Metrics[] allMetrics = Metrics.values();
        for (int n = 0; n < numberOfMeasurements(); n++) {
            measurements[n] = allMetrics[n + numberOfEmotions() + numberOfExpressions()];
        }
        return measurements;
    }
}
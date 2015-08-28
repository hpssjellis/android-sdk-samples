package com.example.alan.imagedetectordemo;

/**
 * Created by Alan on 7/23/2015.
 */
public class MetricsManager {
    //Emotions
    static final int ANGER = 0;
    static final int CONTEMPT = 1;
    static final int DISGUST = 2;
    static final int ENGAGEMENT = 3;
    static final int FEAR = 4;
    static final int JOY = 5;
    static final int SADNESS = 6;
    static final int SURPRISE = 7;
    static final int VALENCE = 8;

    private static final int NUM_EMOTIONS = 9;

    //Expressions
    static final int ATTENTION = 9;
    static final int BROW_FURROW = 10;
    static final int BROW_RAISE = 11;
    static final int CHIN_RAISER = 12;
    static final int EYE_CLOSURE = 13;
    static final int INNER_BROW_RAISER = 14;
    static final int LIP_DEPRESSOR = 15;
    static final int LIP_PRESS = 16;
    static final int LIP_PUCKER = 17;
    static final int LIP_SUCK = 18;
    static final int MOUTH_OPEN = 19;
    static final int NOSE_WRINKLER = 20;
    static final int SMILE = 21;
    static final int SMIRK = 22;
    static final int UPPER_LIP_RAISER = 23;

    private static final int NUM_EMOTIONS_EXPRESSIONS = 24;

    //Measurements
    static final int YAW = 24;
    static final int PITCH = 25;
    static final int ROLL = 26;
    static final int INTER_OCULAR_DISTANCE = 27;

    private static final int NUM_EMOTIONS_EXPRESSIONS_MEASUREMENTS = 28;

    static final int GENDER = 28;

    private static final int NUM_EMOTIONS_EXPRESSIONS_MEASUREMENTS_APPEARANCE = 29;

    private static String[] lowerCaseNames;
    private static String[] upperCaseNames;

    //static initialization of arrays
    static {
        lowerCaseNames = new String[NUM_EMOTIONS_EXPRESSIONS_MEASUREMENTS_APPEARANCE];
        upperCaseNames = new String[NUM_EMOTIONS_EXPRESSIONS_MEASUREMENTS_APPEARANCE];

        //populate lower case array with emotion names
        lowerCaseNames[ANGER] = "anger";
        lowerCaseNames[CONTEMPT] = "contempt";
        lowerCaseNames[DISGUST] = "disgust";
        lowerCaseNames[ENGAGEMENT] = "engagement";
        lowerCaseNames[FEAR] = "fear";
        lowerCaseNames[JOY] = "joy";
        lowerCaseNames[SADNESS] = "sadness";
        lowerCaseNames[SURPRISE] = "surprise";
        lowerCaseNames[VALENCE] = "valence";

        //populate lower case array with expression names
        lowerCaseNames[ATTENTION] = "attention";
        lowerCaseNames[BROW_FURROW] = "brow_furrow";
        lowerCaseNames[BROW_RAISE] = "brow_raise";
        lowerCaseNames[CHIN_RAISER] = "chin_raise";
        lowerCaseNames[EYE_CLOSURE] = "eye_closure";
        lowerCaseNames[INNER_BROW_RAISER] = "inner_brow_raise";
        lowerCaseNames[LIP_DEPRESSOR] = "lip_depressor";
        lowerCaseNames[LIP_PRESS] = "lip_press";
        lowerCaseNames[LIP_PUCKER] = "lip_pucker";
        lowerCaseNames[LIP_SUCK] = "lip_suck";
        lowerCaseNames[MOUTH_OPEN] = "mouth_open";
        lowerCaseNames[NOSE_WRINKLER] = "nose_wrinkle";
        lowerCaseNames[SMILE] = "smile";
        lowerCaseNames[SMIRK] = "smirk";
        lowerCaseNames[UPPER_LIP_RAISER] = "upper_lip_raise";
        lowerCaseNames[YAW] = "yaw";
        lowerCaseNames[PITCH] = "pitch";
        lowerCaseNames[ROLL] = "roll";
        lowerCaseNames[INTER_OCULAR_DISTANCE] = "inter_ocular_distance";
        lowerCaseNames[GENDER] = "gender";

        //use lowerCaseNames array to populate upperCaseNames array
        for (int n = 0; n < lowerCaseNames.length; n++) {
            upperCaseNames[n] = lowerCaseNames[n].replace("_"," ").toUpperCase();
        }

    }

    static String getMetricLowerCaseName(int index){
        if (index >= 0 && index < lowerCaseNames.length) {
            return lowerCaseNames[index];
        } else {
            return "";
        }
    }

    static String getMetricUpperCaseName(int index) {
        if (index >= 0 && index < upperCaseNames.length) {
            return upperCaseNames[index];
        } else {
            return "";
        }
    }

    static int getTotalNumEmotions() {
        return NUM_EMOTIONS;
    }

    static  int getTotalNumExpressions() {
        return NUM_EMOTIONS_EXPRESSIONS_MEASUREMENTS_APPEARANCE - NUM_EMOTIONS;
    }

    static int getTotalNumMeasurements() {
        return NUM_EMOTIONS_EXPRESSIONS_MEASUREMENTS - NUM_EMOTIONS_EXPRESSIONS;
    }

    static int getTotalNumAppearence() {
        return NUM_EMOTIONS_EXPRESSIONS_MEASUREMENTS_APPEARANCE - NUM_EMOTIONS_EXPRESSIONS_MEASUREMENTS;
    }

    static int getTotalNumMetrics() {
        return NUM_EMOTIONS_EXPRESSIONS_MEASUREMENTS_APPEARANCE;
    }

    static int[] getEmotionsIndexArray() {
        int[] toReturn = new int[NUM_EMOTIONS];
        for (int n = 0; n < toReturn.length; n++) {
            toReturn[n] = n;
        }
        return toReturn;
    }

    static int[] getExpressionsIndexArray() {
        int[] toReturn = new int[NUM_EMOTIONS_EXPRESSIONS - NUM_EMOTIONS];
        for (int n = 0; n < toReturn.length; n++) {
            toReturn[n] = n + NUM_EMOTIONS;
        }
        return toReturn;
    }

    static int[] getMeasurementsIndexArray() {
        int[] toReturn = new int[NUM_EMOTIONS_EXPRESSIONS_MEASUREMENTS - NUM_EMOTIONS_EXPRESSIONS];
        for (int n = 0; n < toReturn.length; n++) {
            toReturn[n] = n + NUM_EMOTIONS_EXPRESSIONS;
        }
        return toReturn;
    }

    static int[] getAppearanceIndexArray() {
        int[] toReturn = new int[NUM_EMOTIONS_EXPRESSIONS_MEASUREMENTS_APPEARANCE - NUM_EMOTIONS_EXPRESSIONS_MEASUREMENTS];
        for (int n = 0; n < toReturn.length; n++) {
            toReturn[n] = n + NUM_EMOTIONS_EXPRESSIONS_MEASUREMENTS;
        }
        return toReturn;
    }
}

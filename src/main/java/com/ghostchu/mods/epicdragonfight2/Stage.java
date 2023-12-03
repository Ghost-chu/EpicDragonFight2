package com.ghostchu.mods.epicdragonfight2;

public enum Stage {
    STAGE_1(0.75),
    STAGE_2(0.5),
    STAGE_3(0.15),
    STAGE_4(0.1),
    STAGE_5(0.05);

    private final double percentage;

    Stage(double percentage) {
        this.percentage = percentage;
    }

    public double getPercentage() {
        return percentage;
    }
}

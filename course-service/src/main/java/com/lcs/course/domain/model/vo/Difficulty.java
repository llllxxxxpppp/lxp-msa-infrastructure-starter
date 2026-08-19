package com.lcs.course.domain.model.vo;

public enum Difficulty {
    BEGINNER("입문"),
    PRACTICAL("실전"),
    ADVANCED("심화");

    private final String label;

    Difficulty(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

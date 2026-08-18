package com.lcs.course.domain.model.vo;

public enum Category {
    // 개발
    BACKEND("백엔드 개발"),
    FRONTEND("프론트엔드 개발"),
    MOBILE("모바일 개발"),
    DEVOPS("데브옵스·인프라"),
    SECURITY("보안"),

    // 데이터·AI
    DATA_ANALYSIS("데이터 분석"),
    DATA_ENGINEERING("데이터 엔지니어링"),
    AI_ML("AI·머신러닝"),

    // 기획·디자인
    PRODUCT("프로덕트"),
    DESIGN("디자인·UX");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

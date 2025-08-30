package com.roome.domain.user.entity;

public enum Gender {
    MALE("남성"),
    FEMALE("여성"),
    NONE("비공개");

    private final String displayName;

    Gender(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

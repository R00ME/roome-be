package com.roome.domain.user.entity;

public enum UserRole {

    BEFORE_USER(Authority.BEFORE_USER),
    USER(Authority.USER),
    BEFORE_ADMIN(Authority.BEFORE_ADMIN),
    ADMIN(Authority.ADMIN);

    private final String authority;

    UserRole(String authority) {
        this.authority = authority;
    }

    public String getAuthority() {
        return this.authority;
    }

    public static class Authority {

        public static final String BEFORE_USER = "ROLE_BEFORE_USER";
        public static final String USER = "ROLE_USER";
        public static final String BEFORE_ADMIN = "ROLE_BEFORE_ADMIN";
        public static final String ADMIN = "ROLE_ADMIN";
    }
}

package com.ilkinmehdiyev.kapitalsmallbankingrest.utils;

public class ThreadLocalStorage {

    private static final InheritableThreadLocal<SessionUser> sessionUserInheritableThreadLocal =
            new InheritableThreadLocal<>();

    public static void setSessionUser(SessionUser sessionUser) {
        sessionUserInheritableThreadLocal.set(sessionUser);
    }

    public static SessionUser getSessionUser() {
        return sessionUserInheritableThreadLocal.get();
    }

    public static void updateSessionUser(final Long userId) {
        sessionUserInheritableThreadLocal.get().setUserId(userId);
    }
}

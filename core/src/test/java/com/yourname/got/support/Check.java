package com.yourname.got.support;

import java.util.ArrayList;
import java.util.List;

/** Soft checks: every check is recorded and printed, the test fails at the end if any of them failed. */
public final class Check {
    private static final List<String> failures = new ArrayList<>();

    private Check() {}

    public static void that(boolean condition, String message) {
        if (!condition) failures.add(message);
        System.out.println((condition ? "ok:   " : "FAIL: ") + message);
    }

    public static void clear() {
        failures.clear();
    }

    public static void assertAll() {
        List<String> failed = new ArrayList<>(failures);
        failures.clear();
        if (!failed.isEmpty()) {
            throw new AssertionError(failed.size() + " check(s) failed:\n - " + String.join("\n - ", failed));
        }
    }
}

package com.ilkinmehdiyev.kapitalsmallbankingrest.utils;

import java.util.Map;
import java.util.stream.Collectors;

public final class RepositoryUtils {
    public static final String COMMA_JOINER = ", ";

    private RepositoryUtils() {
    }

    public static void nullSafePut(Map<String, Object> sqlParameters, String key, Object value) {
        if (value != null) {
            sqlParameters.put(key, value);
        }
    }

    public static String columnNamesFrom(Map<String, Object> sqlParameters) {
        return String.join(COMMA_JOINER, sqlParameters.keySet());
    }

    public static String columnValuesFrom(Map<String, Object> sqlParameters) {
        return sqlParameters.keySet().stream().map(" :"::concat).collect(Collectors.joining(COMMA_JOINER));
    }
}

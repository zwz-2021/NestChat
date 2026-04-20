package com.example.nestchat.api;

import android.net.Uri;

public final class MediaUrlResolver {

    private static final String STATIC_SEGMENT = "/static/";

    private MediaUrlResolver() {
    }

    public static String resolve(String value) {
        String normalized = safeTrim(value);
        if (normalized.isEmpty() || isLocalUri(normalized)) {
            return normalized;
        }

        String relativePath = extractRelativeStaticPath(normalized);
        if (relativePath != null) {
            return buildStaticUrl(relativePath);
        }

        if (isHttpUrl(normalized)) {
            return normalized;
        }

        return buildStaticUrl(stripLeadingSlash(normalized));
    }

    public static String toPortableRef(String value) {
        String normalized = safeTrim(value);
        if (normalized.isEmpty() || isLocalUri(normalized)) {
            return normalized;
        }

        String relativePath = extractRelativeStaticPath(normalized);
        return relativePath != null ? relativePath : normalized;
    }

    private static String extractRelativeStaticPath(String value) {
        if (isHttpUrl(value)) {
            Uri uri = Uri.parse(value);
            String path = normalizeSlashes(safeTrim(uri.getEncodedPath()));
            int staticIndex = path.indexOf(STATIC_SEGMENT);
            if (staticIndex >= 0) {
                return stripLeadingSlash(path.substring(staticIndex + STATIC_SEGMENT.length()));
            }
            return null;
        }

        String path = normalizeSlashes(value);
        int staticIndex = path.indexOf(STATIC_SEGMENT);
        if (staticIndex >= 0) {
            return stripLeadingSlash(path.substring(staticIndex + STATIC_SEGMENT.length()));
        }
        if (path.startsWith("static/")) {
            return path.substring("static/".length());
        }
        return stripLeadingSlash(path);
    }

    private static String buildStaticUrl(String relativePath) {
        String staticBaseUrl = getStaticBaseUrl();
        String cleanPath = stripLeadingSlash(relativePath);
        if (cleanPath.isEmpty()) {
            return staticBaseUrl;
        }
        return staticBaseUrl + "/" + cleanPath;
    }

    private static String getStaticBaseUrl() {
        Uri baseUri = Uri.parse(ApiClient.BASE_URL);
        String scheme = safeTrim(baseUri.getScheme());
        String authority = safeTrim(baseUri.getEncodedAuthority());
        String basePath = trimTrailingSlash(normalizeSlashes(safeTrim(baseUri.getEncodedPath())));

        if (scheme.isEmpty() || authority.isEmpty()) {
            return trimTrailingSlash(ApiClient.BASE_URL) + "/static";
        }

        String origin = scheme + "://" + authority;
        if (basePath.isEmpty()) {
            return origin + "/static";
        }
        return origin + basePath + "/static";
    }

    private static boolean isLocalUri(String value) {
        return value.startsWith("content://")
                || value.startsWith("file://")
                || value.startsWith("android.resource://");
    }

    private static boolean isHttpUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }

    private static String normalizeSlashes(String value) {
        return safeTrim(value).replace('\\', '/');
    }

    private static String stripLeadingSlash(String value) {
        String result = safeTrim(value);
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        return result;
    }

    private static String trimTrailingSlash(String value) {
        String result = safeTrim(value);
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}

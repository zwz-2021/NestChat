package com.nestchat.server.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Component
public class MediaUrlHelper {

    private static final String STATIC_SEGMENT = "/static/";

    private final String fallbackStaticBaseUrl;

    public MediaUrlHelper(@Value("${nestchat.upload.base-url:}") String fallbackStaticBaseUrl) {
        this.fallbackStaticBaseUrl = trimTrailingSlash(safeTrim(fallbackStaticBaseUrl));
    }

    public String toStoredPath(String value) {
        String normalized = safeTrim(value);
        if (normalized.isEmpty()) {
            return "";
        }

        String relativePath = extractRelativePath(normalized);
        return relativePath != null ? relativePath : normalized;
    }

    public String toPublicUrl(String value) {
        String normalized = safeTrim(value);
        if (normalized.isEmpty()) {
            return "";
        }

        String relativePath = extractRelativePath(normalized);
        if (relativePath == null) {
            return normalized;
        }

        String staticBaseUrl = resolveStaticBaseUrl();
        return staticBaseUrl.isEmpty() ? relativePath : staticBaseUrl + "/" + relativePath;
    }

    private String resolveStaticBaseUrl() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            HttpServletRequest request = servletAttributes.getRequest();
            String requestBaseUrl = ServletUriComponentsBuilder.fromRequestUri(request)
                    .replacePath(request.getContextPath())
                    .replaceQuery(null)
                    .build()
                    .toUriString();
            return trimTrailingSlash(requestBaseUrl) + "/static";
        }
        return fallbackStaticBaseUrl;
    }

    private String extractRelativePath(String value) {
        String normalized = normalizeSlashes(value);
        if (normalized.isEmpty()) {
            return "";
        }

        if (isHttpUrl(normalized)) {
            try {
                URI uri = URI.create(normalized);
                String path = normalizeSlashes(safeTrim(uri.getPath()));
                return extractRelativePathFromPath(path);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }

        return extractRelativePathFromPath(normalized);
    }

    private String extractRelativePathFromPath(String path) {
        if (path.isEmpty()) {
            return "";
        }

        int staticIndex = path.indexOf(STATIC_SEGMENT);
        if (staticIndex >= 0) {
            return stripLeadingSlash(path.substring(staticIndex + STATIC_SEGMENT.length()));
        }

        if (path.startsWith("static/")) {
            return path.substring("static/".length());
        }

        return stripLeadingSlash(path);
    }

    private boolean isHttpUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }

    private String normalizeSlashes(String value) {
        return safeTrim(value).replace('\\', '/');
    }

    private String stripLeadingSlash(String value) {
        String result = safeTrim(value);
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        return result;
    }

    private String trimTrailingSlash(String value) {
        String result = safeTrim(value);
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}

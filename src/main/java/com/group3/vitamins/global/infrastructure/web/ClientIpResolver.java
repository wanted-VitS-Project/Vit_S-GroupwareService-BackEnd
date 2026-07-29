package com.group3.vitamins.global.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.web")
public class ClientIpResolver {

    private static final List<String> DEFAULT_TRUSTED_PROXIES = List.of(
            "127.0.0.1",
            "0:0:0:0:0:0:0:1",
            "::1"
    );

    private static final List<String> IP_HEADERS = List.of(
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
    );

    private List<String> trustedProxies = DEFAULT_TRUSTED_PROXIES;

    public String resolve(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        if (!isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }

        for (String header : IP_HEADERS) {
            String value = request.getHeader(header);
            String forwardedIp = firstValidForwardedIp(value);

            if (hasText(forwardedIp)) {
                return forwardedIp;
            }
        }

        return remoteAddr;
    }

    public void setTrustedProxies(List<String> trustedProxies) {
        if (trustedProxies == null || trustedProxies.isEmpty()) {
            this.trustedProxies = DEFAULT_TRUSTED_PROXIES;
            return;
        }

        this.trustedProxies = List.copyOf(trustedProxies);
    }

    private boolean isTrustedProxy(String remoteAddr) {
        return hasText(remoteAddr) && trustedProxies.contains(remoteAddr);
    }

    private String firstValidForwardedIp(String value) {
        if (!hasText(value)) {
            return null;
        }

        for (String token : value.split(",")) {
            String candidate = token.trim();

            if (hasText(candidate)
                    && !"unknown".equalsIgnoreCase(candidate)
                    && isValidIpAddress(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private boolean isValidIpAddress(String value) {
        return isValidIpv4(value) || isValidIpv6(value);
    }

    private boolean isValidIpv4(String value) {
        String[] octets = value.split("\\.", -1);

        if (octets.length != 4) {
            return false;
        }

        for (String octet : octets) {
            if (octet.isEmpty() || octet.length() > 3 || !isDigits(octet)) {
                return false;
            }

            int parsed = Integer.parseInt(octet);

            if (parsed > 255) {
                return false;
            }
        }

        return true;
    }

    private boolean isValidIpv6(String value) {
        if (!value.contains(":") || value.length() > 45) {
            return false;
        }

        try {
            return InetAddress.getByName(value) instanceof Inet6Address;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private boolean isDigits(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

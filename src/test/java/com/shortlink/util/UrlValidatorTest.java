package com.shortlink.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UrlValidatorTest {

    @Test
    void acceptsHttpAndHttpsUrlsWithHosts() {
        assertThat(UrlValidator.isValidHttpUrl("https://example.com/path?a=1")).isTrue();
        assertThat(UrlValidator.isValidHttpUrl("http://localhost:8080/demo")).isTrue();
    }

    @Test
    void rejectsBlankRelativeAndNonHttpUrls() {
        assertThat(UrlValidator.isValidHttpUrl(null)).isFalse();
        assertThat(UrlValidator.isValidHttpUrl("")).isFalse();
        assertThat(UrlValidator.isValidHttpUrl("/relative/path")).isFalse();
        assertThat(UrlValidator.isValidHttpUrl("ftp://example.com/file")).isFalse();
        assertThat(UrlValidator.isValidHttpUrl("https:///missing-host")).isFalse();
    }
}

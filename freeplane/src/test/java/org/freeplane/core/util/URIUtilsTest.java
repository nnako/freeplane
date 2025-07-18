package org.freeplane.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

public class URIUtilsTest {

    @Test
    public void testCreateURIFromString_WithSpaces() throws URISyntaxException {
        String uriString = "file:///path/to/file with spaces.txt";
        URI uri = URIUtils.createURIFromString(uriString);
        
        assertThat(uri.getScheme()).isEqualTo("file");
        assertThat(uri.getPath()).isEqualTo("/path/to/file with spaces.txt");
        assertThat(uri.getFragment()).isNull();
    }

    @Test
    public void testCreateURIFromString_WithSpacesAndFragment() throws URISyntaxException {
        String uriString = "file:///path/to/file with spaces.txt#ID_123";
        URI uri = URIUtils.createURIFromString(uriString);
        
        assertThat(uri.getScheme()).isEqualTo("file");
        assertThat(uri.getPath()).isEqualTo("/path/to/file with spaces.txt");
        assertThat(uri.getFragment()).isEqualTo("ID_123");
    }

    @Test
    public void testCreateURIFromString_WithEncodedSpaces() throws URISyntaxException {
        String uriString = "file:///path/to/file%20with%20spaces.txt";
        URI uri = URIUtils.createURIFromString(uriString);
        
        assertThat(uri.getScheme()).isEqualTo("file");
        assertThat(uri.getPath()).isEqualTo("/path/to/file with spaces.txt");
        assertThat(uri.getFragment()).isNull();
    }

    @Test
    public void testCreateURIFromString_WithEncodedSpacesAndFragment() throws URISyntaxException {
        String uriString = "file:///path/to/file%20with%20spaces.txt#ID_456";
        URI uri = URIUtils.createURIFromString(uriString);
        
        assertThat(uri.getScheme()).isEqualTo("file");
        assertThat(uri.getPath()).isEqualTo("/path/to/file with spaces.txt");
        assertThat(uri.getFragment()).isEqualTo("ID_456");
    }

    @Test
    public void testCreateURIFromString_HttpUrlWithSpaces() {
        String uriString = "http://example.com/path with spaces/page.html";
        
        assertThatThrownBy(() -> URIUtils.createURIFromString(uriString))
            .isInstanceOf(URISyntaxException.class);
    }

    @Test
    public void testCreateURIFromString_HttpUrlWithSpacesAndFragment() {
        String uriString = "http://example.com/path with spaces/page.html#section1";
        
        assertThatThrownBy(() -> URIUtils.createURIFromString(uriString))
            .isInstanceOf(URISyntaxException.class);
    }

    @Test
    public void testCreateURIFromString_WithQueryParameters() {
        String uriString = "http://example.com/path with spaces/page.html?param=value&other=test";
        
        assertThatThrownBy(() -> URIUtils.createURIFromString(uriString))
            .isInstanceOf(URISyntaxException.class);
    }

    @Test
    public void testCreateURIFromString_WithQueryAndFragment() {
        String uriString = "http://example.com/path with spaces/page.html?param=value#section1";
        
        assertThatThrownBy(() -> URIUtils.createURIFromString(uriString))
            .isInstanceOf(URISyntaxException.class);
    }

    @Test
    public void testCreateURIFromString_OnlyFragment() throws URISyntaxException {
        String uriString = "#ID_789";
        URI uri = URIUtils.createURIFromString(uriString);
        
        assertThat(uri.getScheme()).isNull();
        assertThat(uri.getHost()).isNull();
        assertThat(uri.getPath()).isEmpty();
        assertThat(uri.getFragment()).isEqualTo("ID_789");
    }

    @Test
    public void testCreateURIFromString_EmptyString() throws URISyntaxException {
        String uriString = "";
        URI uri = URIUtils.createURIFromString(uriString);
        
        assertThat(uri.getScheme()).isNull();
        assertThat(uri.getHost()).isNull();
        assertThat(uri.getPath()).isEmpty();
        assertThat(uri.getFragment()).isNull();
    }

    @Test
    public void testCreateURIFromString_InvalidURI() {
        String uriString = "http://[invalid";
        
        assertThatThrownBy(() -> URIUtils.createURIFromString(uriString))
            .isInstanceOf(URISyntaxException.class);
    }

    @Test
    public void testCreateURIFromString_WithSpecialCharacters() throws URISyntaxException {
        String uriString = "file:///path/to/file with spaces & special chars (test).txt";
        URI uri = URIUtils.createURIFromString(uriString);
        
        assertThat(uri.getScheme()).isEqualTo("file");
        assertThat(uri.getPath()).isEqualTo("/path/to/file with spaces & special chars (test).txt");
        assertThat(uri.getFragment()).isNull();
    }
} 
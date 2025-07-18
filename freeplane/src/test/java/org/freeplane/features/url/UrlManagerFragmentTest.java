package org.freeplane.features.url;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.freeplane.core.util.URIUtils;

import org.freeplane.features.map.MapModel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UrlManagerFragmentTest {

    @Mock
    private MapModel mapModel;

    @Test
    public void testFragmentPreservation_CurrentImplementation() throws MalformedURLException {
        when(mapModel.getURL()).thenReturn(new URL("file:///base/path/"));
        
        // Test with fragment
        URI relativeUri = URI.create("file.txt#ID_123");
        URI result = UrlManager.getAbsoluteUri(mapModel, relativeUri);
        
        assertThat(result).isNotNull();
        assertThat(result.getFragment()).isEqualTo("ID_123");
        assertThat(result.getPath()).isEqualTo("/base/path/file.txt");
    }

    @Test
    public void testFragmentPreservation_WithSpaces() throws MalformedURLException, URISyntaxException {
        when(mapModel.getURL()).thenReturn(new URL("file:///base/path/"));
        
        // Test with spaces and fragment
        URI relativeUri = URIUtils.createURIFromString("file with spaces.txt#ID_123");
        URI result = UrlManager.getAbsoluteUri(mapModel, relativeUri);
        
        assertThat(result).isNotNull();
        assertThat(result.getFragment()).isEqualTo("ID_123");
        assertThat(result.getPath()).isEqualTo("/base/path/file with spaces.txt");
    }

    @Test
    public void testFragmentPreservation_WithQuery() throws MalformedURLException {
        when(mapModel.getURL()).thenReturn(new URL("file:///base/path/"));
        
        // Test with query and fragment
        URI relativeUri = URI.create("file.txt?param=value#ID_123");
        URI result = UrlManager.getAbsoluteUri(mapModel, relativeUri);
        
        assertThat(result).isNotNull();
        assertThat(result.getFragment()).isEqualTo("ID_123");
        assertThat(result.getQuery()).isEqualTo("param=value");
        assertThat(result.getPath()).isEqualTo("/base/path/file.txt");
    }

    @Test
    public void testFragmentPreservation_ComplexCase() throws MalformedURLException, URISyntaxException {
        when(mapModel.getURL()).thenReturn(new URL("file:///base/path/"));
        
        // Test with spaces, query, and fragment
        URI relativeUri = URIUtils.createURIFromString("file with spaces.txt?param=value&other=test#ID_123");
        URI result = UrlManager.getAbsoluteUri(mapModel, relativeUri);
        
        assertThat(result).isNotNull();
        assertThat(result.getFragment()).isEqualTo("ID_123");
        assertThat(result.getQuery()).isEqualTo("param=value&other=test");
        assertThat(result.getPath()).isEqualTo("/base/path/file with spaces.txt");
    }

    @Test
    public void testFragmentPreservation_OnlyFragment() throws MalformedURLException {
        when(mapModel.getURL()).thenReturn(new URL("file:///base/path/"));
        
        // Test with only fragment (no path)
        URI relativeUri = URI.create("#ID_123");
        URI result = UrlManager.getAbsoluteUri(mapModel, relativeUri);
        
        assertThat(result).isNotNull();
        assertThat(result.getFragment()).isEqualTo("ID_123");
        assertThat(result.getPath()).isEqualTo("/base/path/");
    }
} 
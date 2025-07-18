package org.freeplane.features.url;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.freeplane.features.map.MapModel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UrlManagerTest {

    @Mock
    private MapModel mapModel;

    @Test
    public void testGetAbsoluteUri_NullUri() throws MalformedURLException {
        URI result = UrlManager.getAbsoluteUri(mapModel, null);
        assertThat(result).isNull();
    }

    @Test
    public void testGetAbsoluteUri_AbsoluteUri() throws MalformedURLException {
        URI absoluteUri = URI.create("file:///absolute/path/file.txt");
        URI result = UrlManager.getAbsoluteUri(mapModel, absoluteUri);
        assertThat(result).isEqualTo(absoluteUri);
    }

    @Test
    public void testGetAbsoluteUri_RelativeUri() throws MalformedURLException {
        when(mapModel.getURL()).thenReturn(new URL("file:///base/path/"));
        
        URI relativeUri = URI.create("relative/file.txt#ID_123");
        URI result = UrlManager.getAbsoluteUri(mapModel, relativeUri);
        
        assertThat(result).isNotNull();
        assertThat(result.getScheme()).isEqualTo("file");
        assertThat(result.getPath()).isEqualTo("/base/path/relative/file.txt");
        assertThat(result.getFragment()).isEqualTo("ID_123");
    }

    @Test
    public void testGetAbsoluteUri_NullMapUrl() throws MalformedURLException {
        when(mapModel.getURL()).thenReturn(null);
        
        URI relativeUri = URI.create("relative/file.txt#ID_123");
        URI result = UrlManager.getAbsoluteUri(mapModel, relativeUri);
        
        assertThat(result).isNull();
    }
} 
package org.freeplane.view.swing.features.filepreview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.freeplane.features.map.MapModel;
import org.freeplane.core.util.URIUtils;
import org.freeplane.features.url.UrlManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ViewerControllerIntegrationTest {

    @Mock
    private MapModel mapModel;

    @Test
    public void testCompleteFlow_URIUtilsAndUrlManagerWorkTogether() throws Exception {
        when(mapModel.getURL()).thenReturn(new URL("file:///base/path/"));
        
        String uriString = "file with spaces.txt#ID_123";
        
        URI uri = URIUtils.createURIFromString(uriString);
        assertThat(uri).as("URI should be created successfully").isNotNull();
        assertThat(uri.getPath()).isEqualTo("file with spaces.txt");
        assertThat(uri.getFragment()).isEqualTo("ID_123");
        
        URI absoluteUri = UrlManager.getAbsoluteUri(mapModel, uri);
        assertThat(absoluteUri).as("Absolute URI should be resolved successfully").isNotNull();
        assertThat(absoluteUri.getPath()).isEqualTo("/base/path/file with spaces.txt");
        assertThat(absoluteUri.getFragment()).isEqualTo("ID_123");
    }
} 
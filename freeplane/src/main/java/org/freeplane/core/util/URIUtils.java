package org.freeplane.core.util;

import java.net.URI;
import java.net.URISyntaxException;

public class URIUtils {
    
    public static URI createURIFromString(String uriString) throws URISyntaxException {
        if (uriString == null || uriString.trim().isEmpty()) {
            return new URI(null, null, "", null);
        }
        
        try {
            return new URI(uriString);
        } catch (URISyntaxException e) {
            // Handle fragment-only URIs (node IDs) - these actually work with URI constructor
            if (uriString.startsWith("#")) {
                return new URI(uriString);
            }
            
            // Handle URIs with spaces - only for file URIs and relative paths
            if (uriString.contains(" ")) {
                // For file URIs
                if (uriString.startsWith("file:")) {
                    String path = uriString.substring(5); // Remove "file:"
                    String fragment = null;
                    String query = null;
                    
                    // Extract fragment
                    int fragmentIndex = path.indexOf('#');
                    if (fragmentIndex >= 0) {
                        fragment = path.substring(fragmentIndex + 1);
                        path = path.substring(0, fragmentIndex);
                    }
                    
                    // Extract query
                    int queryIndex = path.indexOf('?');
                    if (queryIndex >= 0) {
                        query = path.substring(queryIndex + 1);
                        path = path.substring(0, queryIndex);
                    }
                    
                    return new URI("file", null, path, query, fragment);
                }
                
                // For relative paths (not absolute HTTP URLs)
                if (!uriString.contains("://")) {
                    String fragment = null;
                    String query = null;
                    int fragmentIndex = uriString.indexOf('#');
                    if (fragmentIndex >= 0) {
                        fragment = uriString.substring(fragmentIndex + 1);
                        uriString = uriString.substring(0, fragmentIndex);
                    }
                    
                    int queryIndex = uriString.indexOf('?');
                    if (queryIndex >= 0) {
                        query = uriString.substring(queryIndex + 1);
                        uriString = uriString.substring(0, queryIndex);
                    }
                    
                    return new URI(null, null, uriString, query, fragment);
                }
            }
            throw e;
        }
    }
} 
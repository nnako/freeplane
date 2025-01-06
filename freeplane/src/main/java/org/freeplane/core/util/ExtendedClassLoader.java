/*
 * Created on 6 Jan 2025
 *
 * author dimitry
 */
package org.freeplane.core.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;

public class ExtendedClassLoader extends ClassLoader implements AutoCloseable {

	private final URLClassLoader urlClassLoader;
	private final ProtectionDomain protectionDomain;
	public ExtendedClassLoader(URL[] extensionUrls, Class<?> prototype) {
    	this(extensionUrls, prototype.getClassLoader(), prototype.getProtectionDomain());
    }

    protected ExtendedClassLoader(URL[] extensionUrls, ClassLoader parent, ProtectionDomain protectionDomain) {
        super(parent);
		this.protectionDomain = protectionDomain;
        this.urlClassLoader = new URLClassLoader(extensionUrls);
    }

	public Class<?> preload(String name) {
		synchronized (getClassLoadingLock(name)) {
			return findClassFromAdditionalLoader(getParent(), name);
		}
	}

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // Attempt to find the class using additional class loaders
        Class<?> cls = findClassFromAdditionalLoader(urlClassLoader, name);
        if (cls != null) {
            return cls;
        }

        // If not found in additional class loaders, throw ClassNotFoundException
        throw new ClassNotFoundException(name);
    }

    @Override
	protected URL findResource(String name) {
    	URL urlResource = urlClassLoader.findResource(name);
    	return urlResource != null ? urlResource : super.findResource(name);
	}

    private Class<?> findClassFromAdditionalLoader(ClassLoader cl, String name) {
    	String resourceName = name.replace('.', '/').concat(".class");

    		try (InputStream is = cl.getResourceAsStream(resourceName)) {
    			if (is != null) {
    				byte[] classBytes = readAllBytes(is);
    				return defineClass(name, classBytes, 0, classBytes.length, protectionDomain);
    			}
    		} catch (IOException e) {
    			e.printStackTrace();
    		}

        return null;
    }

    private byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int bytesRead;
        byte[] dataChunk = new byte[16384]; // 16KB buffer

        while ((bytesRead = is.read(dataChunk, 0, dataChunk.length)) != -1) {
            buffer.write(dataChunk, 0, bytesRead);
        }

        return buffer.toByteArray();
    }

	@Override
	public void close() throws IOException {
		urlClassLoader.close();
	}
}
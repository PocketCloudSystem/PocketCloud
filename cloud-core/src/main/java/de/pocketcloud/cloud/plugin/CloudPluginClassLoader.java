package de.pocketcloud.cloud.plugin;

import lombok.Getter;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

@Getter
public final class CloudPluginClassLoader extends URLClassLoader {

    private final Map<String, Class<?>> classes = new HashMap<>();
    private final File pluginJar;

    public CloudPluginClassLoader(ClassLoader parent, File pluginJar) throws MalformedURLException {
        super(new URL[]{pluginJar.toURI().toURL()}, parent);
        this.pluginJar = pluginJar;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> result = classes.get(name);
        if (result == null) {
            result = super.findClass(name);
            if (result != null) classes.put(name, result);
        }
        return result;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> clazz = findLoadedClass(name);
            if (clazz == null) {
                if (name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("de.pocketcloud.")) {
                    try {
                        clazz = getParent().loadClass(name);
                    } catch (ClassNotFoundException ex) {
                        clazz = findClass(name);
                    }
                } else {
                    try {
                        clazz = findClass(name);
                    } catch (ClassNotFoundException ex) {
                        clazz = getParent().loadClass(name);
                    }
                }
            }

            if (resolve) {
                resolveClass(clazz);
            }

            return clazz;
        }
    }
}
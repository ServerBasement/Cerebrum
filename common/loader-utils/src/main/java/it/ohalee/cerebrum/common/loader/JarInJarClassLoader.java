package it.ohalee.cerebrum.common.loader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Classloader that can load a jar from within another jar file.
 *
 * <p>The "loader" jar contains the loading code & public API classes,
 * and is class-loaded by the platform.</p>
 *
 * <p>The inner "plugin" jar contains the plugin itself, and is class-loaded
 * by the loading code & this classloader.</p>
 */
public class JarInJarClassLoader extends URLClassLoader {
    static {
        ClassLoader.registerAsParallelCapable();
    }

    /**
     * Creates a new jar-in-jar class loader.
     *
     * @param loaderClassLoader the loader plugin's classloader (setup and created by the platform)
     * @param jarResourcePath   the path to the jar-in-jar resource within the loader jar
     * @throws LoadingException if something unexpectedly bad happens
     */
    public JarInJarClassLoader(ClassLoader loaderClassLoader, String jarResourcePath) throws LoadingException {
        super(new URL[]{extractJar(loaderClassLoader, jarResourcePath)}, loaderClassLoader);
    }

    /**
     * Extracts the "jar-in-jar" from the loader plugin into a temporary file,
     * then returns a URL that can be used by the {@link JarInJarClassLoader}.
     *
     * @param loaderClassLoader the classloader for the "host" loader plugin
     * @param jarResourcePath   the inner jar resource path
     * @return a URL to the extracted file
     */
    private static URL extractJar(ClassLoader loaderClassLoader, String jarResourcePath) throws LoadingException {
        // get the jar-in-jar resource
        URL jarInJar = loaderClassLoader.getResource(jarResourcePath);
        if (jarInJar == null) {
            throw new LoadingException("Could not locate jar-in-jar");
        }

        // create a temporary file
        // on posix systems by default this is only read/writable by the process owner
        Path path;
        try {
            path = Files.createTempFile("cerebrum-jarinjar", ".jar.tmp");
        } catch (IOException e) {
            throw new LoadingException("Unable to create a temporary file", e);
        }

        // mark that the file should be deleted on exit
        path.toFile().deleteOnExit();

        // copy the jar-in-jar to the temporary file path
        try (InputStream in = jarInJar.openStream()) {
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new LoadingException("Unable to copy jar-in-jar to temporary path", e);
        }

        try {
            return path.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new LoadingException("Unable to get URL from path", e);
        }
    }

    public void addJarToClasspath(URL url) {
        addURL(url);
    }

    public void deleteJarResource() {
        URL[] urls = getURLs();
        if (urls.length == 0) {
            return;
        }

        try {
            Path path = Paths.get(urls[0].toURI());
            Files.deleteIfExists(path);
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * Creates a new plugin instance.
     *
     * @param bootstrapClass   the name of the bootstrap plugin class
     * @param loaderPluginType the type of the loader plugin, the only parameter of the bootstrap
     *                         plugin constructor
     * @param loaderPlugin     the loader plugin instance
     * @param <T>              the type of the loader plugin
     * @return the instantiated bootstrap plugin
     */
    public <T> LoaderBootstrap instantiatePlugin(String bootstrapClass, Class<T> loaderPluginType, T loaderPlugin) throws LoadingException {
        Class<? extends LoaderBootstrap> plugin;
        try {
            plugin = loadClass(bootstrapClass).asSubclass(LoaderBootstrap.class);
        } catch (ReflectiveOperationException e) {
            throw new LoadingException("Unable to load bootstrap class", e);
        }

        Constructor<? extends LoaderBootstrap> constructor;
        try {
            constructor = plugin.getConstructor(loaderPluginType);
        } catch (ReflectiveOperationException e) {
            throw new LoadingException("Unable to get bootstrap constructor", e);
        }

        try {
            return constructor.newInstance(loaderPlugin);
        } catch (ReflectiveOperationException e) {
            throw new LoadingException("Unable to create bootstrap plugin instance", e);
        }
    }

    public <T> LoaderBootstrap instantiatePlugin(String bootstrapClass) throws LoadingException {
        Class<? extends LoaderBootstrap> plugin;
        try {
            plugin = loadClass(bootstrapClass).asSubclass(LoaderBootstrap.class);
        } catch (ReflectiveOperationException e) {
            throw new LoadingException("Unable to load bootstrap class", e);
        }

        Constructor<? extends LoaderBootstrap> constructor;
        try {
            constructor = plugin.getConstructor();
        } catch (ReflectiveOperationException e) {
            throw new LoadingException("Unable to get bootstrap constructor", e);
        }

        try {
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new LoadingException("Unable to create bootstrap plugin instance", e);
        }
    }

    public void runMain(String bootstrapClass, String[] args) throws LoadingException {
        Class<?> clazz;
        try {
            clazz = loadClass(bootstrapClass);
        } catch (ReflectiveOperationException e) {
            throw new LoadingException("Unable to load bootstrap class", e);
        }

        Method method;
        try {
            method = clazz.getMethod("main", String[].class);
        } catch (NoSuchMethodException e) {
            throw new LoadingException("Unable to get bootstrap main method", e);
        }

        try {
            method.invoke(null, (Object) args);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new LoadingException("Unable to invoke bootstrap main method", e);
        }
    }

}

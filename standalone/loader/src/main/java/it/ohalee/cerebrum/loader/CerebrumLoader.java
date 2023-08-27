package it.ohalee.cerebrum.loader;

import it.ohalee.cerebrum.common.loader.JarInJarClassLoader;
import it.ohalee.cerebrum.common.loader.LoaderBootstrap;

public class CerebrumLoader {
    private static final String JAR_NAME = "cerebrum-standalone.jarinjar";
    private static final String BOOTSTRAP_PLUGIN_CLASS = "it.ohalee.cerebrum.standalone.CerebrumApplication";

    private JarInJarClassLoader loader;
    private LoaderBootstrap plugin;

    // Entrypoint
    public static void main(String[] args) {
        CerebrumLoader loader = new CerebrumLoader();
        loader.start(args);
    }

    public void start(String[] args) {
        this.loader = new JarInJarClassLoader(getClass().getClassLoader(), JAR_NAME);

        this.plugin = this.loader.instantiatePlugin(BOOTSTRAP_PLUGIN_CLASS);
        this.plugin.onLoad();
        this.plugin.onEnable(args);
    }

}

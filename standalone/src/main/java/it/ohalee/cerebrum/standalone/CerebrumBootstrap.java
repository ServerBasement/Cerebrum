package it.ohalee.cerebrum.standalone;

import it.ohalee.cerebrum.app.integration.CommandExecutor;
import it.ohalee.cerebrum.common.classpath.ClassPathAppender;
import it.ohalee.cerebrum.common.classpath.JarInJarClassPathAppender;
import it.ohalee.cerebrum.common.loader.LoaderBootstrap;
import it.ohalee.cerebrum.standalone.command.StandaloneCommandManager;
import it.ohalee.cerebrum.standalone.config.CerebrumConfigAdapter;
import it.ohalee.cerebrum.standalone.dependency.DependencyManager;
import it.ohalee.cerebrum.standalone.docker.DockerService;
import it.ohalee.cerebrum.standalone.basement.BasementLoader;
import it.ohalee.cerebrum.standalone.basement.BasementService;
import it.ohalee.cerebrum.standalone.dependency.Dependency;
import lombok.Getter;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class CerebrumBootstrap implements LoaderBootstrap {

    private final Consumer<CommandExecutor> consumer;
    private final Path dir;
    @Getter
    private final ClassPathAppender classPathAppender;
    private BasementLoader loader;

    public CerebrumBootstrap(Consumer<CommandExecutor> consumer) {
        this.consumer = consumer;
        this.dir = new File(".").toPath();
        this.classPathAppender = new JarInJarClassPathAppender(getClass().getClassLoader());
    }

    protected Set<Dependency> getGlobalDependencies() {
        return new HashSet<>(Arrays.asList(Dependency.values()));
    }

    @Override
    public void onLoad() {
        new DependencyManager(this).loadDependencies(getGlobalDependencies());
        this.loader = new BasementLoader(dir);
    }

    @Override
    public void onEnable() {
        CerebrumConfigAdapter settings = (CerebrumConfigAdapter) loader.getBasement().plugin()
                .provideConfigurationAdapter(CerebrumBootstrap.class, dir.resolve("settings.yml").toFile(), true);
        CerebrumConfigAdapter share = (CerebrumConfigAdapter) loader.getBasement().plugin()
                .provideConfigurationAdapter(CerebrumBootstrap.class, dir.resolve("share.yml").toFile(), true);

        DockerService dockerService = new DockerService(settings, share);
        new BasementService(loader, dockerService);

        StandaloneCommandManager commandManager = new StandaloneCommandManager(dockerService);
        consumer.accept(commandManager);

        dockerService.postExecution();
    }

    @Override
    public void onDisable() {

    }
}

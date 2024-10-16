package it.ohalee.cerebrum.standalone.dependency;

import it.ohalee.cerebrum.app.Logger;
import it.ohalee.cerebrum.app.scheduler.CerebrumScheduler;
import it.ohalee.cerebrum.common.classpath.ClassPathAppender;
import it.ohalee.cerebrum.standalone.CerebrumBootstrap;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class DependencyManager {

    private final Path cacheDirectory;
    private final EnumMap<Dependency, Path> loaded = new EnumMap<>(Dependency.class);
    private final CerebrumScheduler scheduler;
    private final ClassPathAppender classPathAppender;

    public DependencyManager(CerebrumBootstrap bootstrap) {
        this.cacheDirectory = setupCacheDirectory();
        this.scheduler = bootstrap.getScheduler();
        this.classPathAppender = bootstrap.getClassPathAppender();
    }

    private static Path setupCacheDirectory() {
        Path cacheDirectory = new File(".").toPath().resolve("libs");
        try {
            if (Files.exists(cacheDirectory) && (Files.isDirectory(cacheDirectory) || Files.isSymbolicLink(cacheDirectory))) {
                return cacheDirectory;
            }

            try {
                Files.createDirectories(cacheDirectory);
            } catch (FileAlreadyExistsException e) {
                // ignore
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to create libs directory", e);
        }
        return cacheDirectory;
    }

    public void loadDependencies(Set<Dependency> dependencies) {
        Logger.info("Loading dependencies...");
        CountDownLatch latch = new CountDownLatch(dependencies.size());

        for (Dependency dependency : dependencies) {
            if (this.loaded.containsKey(dependency)) {
                latch.countDown();
                continue;
            }

            this.scheduler.execute(() -> {
                try {
                    loadDependency(dependency);
                } catch (Throwable e) {
                    new RuntimeException("Unable to load dependency " + dependency.name(), e).printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void loadDependency(Dependency dependency) throws Exception {
        if (this.loaded.containsKey(dependency)) {
            return;
        }

        Path file = downloadDependency(dependency);

        this.loaded.put(dependency, file);

        if (this.classPathAppender != null) {
            this.classPathAppender.addJarToClasspath(file);
        }
    }

    private Path downloadDependency(Dependency dependency) throws DependencyDownloadException {
        Path file = this.cacheDirectory.resolve(dependency.getFileName(null));

        // if the file already exists, don't attempt to re-download it.
        if (Files.exists(file)) {
            return file;
        }

        DependencyDownloadException lastError = null;

        // attempt to download the dependency from each repo in order.
        for (DependencyRepository repo : DependencyRepository.values()) {
            try {
                repo.download(dependency, file);
                return file;
            } catch (DependencyDownloadException e) {
                lastError = e;
            }
        }

        throw Objects.requireNonNull(lastError);
    }

}

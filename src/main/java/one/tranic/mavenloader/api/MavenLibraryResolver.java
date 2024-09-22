package one.tranic.mavenloader.api;

import one.tranic.mavenloader.boost.Boost;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides the functionality to dynamically resolve and download Maven dependencies.
 * It allows adding remote repositories, resolving dependencies, and automatically downloading
 * artifacts to a local directory.
 * <p>
 * No need to add central repository and sonatype, they are already included by default
 */
public class MavenLibraryResolver {

    private static final Logger logger = LoggerFactory.getLogger("MavenLibraryResolver");
    private static boolean enabled;

    static {
        try {
            Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            enabled = true;
        } catch (Exception e) {
            enabled = false;
            logger.error("--add-opens=java.base/java.net=ALL-UNNAMED is not enabled.", e);
        }
    }

    private final RepositorySystem system;
    private final DefaultRepositorySystemSession session;
    private final List<RemoteRepository> repositories;

    /**
     * Creates an instance of {@code MavenLibraryResolver} with default repositories.
     * The central Maven repository and Sonatype repository are automatically added
     * to the list of repositories.
     */
    public MavenLibraryResolver() {
        this.system = newRepositorySystem();
        this.session = newRepositorySystemSession();

        this.repositories = new ArrayList<>();
        this.repositories.add(CentralRepository());
        this.repositories.add(getSonatypeRepo());
    }

    /**
     * Adds a custom remote repository to the list of repositories for dependency resolution.
     *
     * @param repository the remote repository to be added, not be {@code null}.
     */
    public void addRepository(@NotNull RemoteRepository repository) {
        repositories.add(Boost.get(repository));
    }

    /**
     * Adds a custom remote repository to the list of repositories for dependency resolution.
     *
     * @param repository the remote repository to be added, not be {@code null}.
     * @param name       the identifier of the repository, not be {@code null}.
     */
    public void addRepository(@NotNull String repository, @NotNull String name) {
        repositories.add(Boost.get(new RemoteRepository.Builder(name, "default", repository).build()));
    }

    /**
     * Adds a custom remote repository to the list of repositories for dependency resolution.
     *
     * @param repository the remote repository to be added, not be {@code null}.
     * @param type       the type of the repository, not be {@code null}.
     * @param name       the identifier of the repository, not be {@code null}.
     */
    public void addRepository(@NotNull String repository, @NotNull String type, @NotNull String name) {
        repositories.add(Boost.get(new RemoteRepository.Builder(name, type, repository).build()));
    }

    /**
     * Adds a Maven dependency to be resolved and downloaded.
     *
     * @param dependency the Maven dependency to resolve, not be {@code null}.
     * @throws Exception if the dependency resolution fails
     */
    public void addDependency(@NotNull Dependency dependency) throws Exception {
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRepositories(repositories);
        collectRequest.addDependency(dependency);

        DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, null);
        DependencyResult dependencyResult = system.resolveDependencies(session, dependencyRequest);

        List<ArtifactResult> results = dependencyResult.getArtifactResults();
        if (results.isEmpty()) return;
        // Load each downloaded artifact (including dependencies)
        for (ArtifactResult artifactResult : results) {
            File jarFile = artifactResult.getArtifact().getFile();
            if (jarFile != null && jarFile.exists()) {
                loadJar(jarFile);
            }
        }
    }

    /**
     * Adds a Maven dependency to be resolved and downloaded.
     *
     * @param dep the Maven dependency to resolve, not be {@code null}.
     * @throws Exception if the dependency resolution fails
     */
    public void addDependency(@NotNull String dep) throws Exception {
        Dependency dependency = new Dependency(new DefaultArtifact(dep), null);
        addDependency(dependency);
    }

    /**
     * Adds a Maven dependency to be resolved and downloaded.
     *
     * @param dep   the Maven dependency to resolve, not be {@code null}.
     * @param scope the scope of the dependency, may be {@code null}.
     * @throws Exception if the dependency resolution fails
     */
    public void addDependency(@NotNull String dep, @Nullable String scope) throws Exception {
        Dependency dependency = new Dependency(new DefaultArtifact(dep), scope);
        addDependency(dependency);
    }

    /**
     * Dynamically load the specified JAR file into the current thread's context classloader.
     *
     * @param jarFile the JAR file to load
     * @throws Exception if any error occurs during loading
     */
    private void loadJar(File jarFile) throws Exception {
        if (!jarFile.exists()) throw new FileNotFoundException();
        if (!enabled) return;
        URL jarUrl = jarFile.toURI().toURL();

        ClassLoader pluginClassLoader = this.getClass().getClassLoader();

        Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        method.setAccessible(true);

        if (pluginClassLoader instanceof URLClassLoader) {
            method.invoke(pluginClassLoader, jarUrl);
            logger.info("Loaded JAR into plugin classloader: " + jarFile.getAbsolutePath());
        } else {
            logger.error("ClassLoader is not an instance of URLClassLoader");
        }
    }

    /**
     * Initializes and returns a new instance of the Maven {@code RepositorySystem}.
     * This system is responsible for resolving dependencies.
     *
     * @return a configured {@code RepositorySystem}
     */
    private RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        return locator.getService(RepositorySystem.class);
    }

    /**
     * Initializes and returns a new session for the Maven repository system.
     * The session is configured to use system properties and local repository management.
     * A transfer listener is set up to log download events.
     *
     * @return a configured {@code DefaultRepositorySystemSession}
     */
    private DefaultRepositorySystemSession newRepositorySystemSession() {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        session.setSystemProperties(System.getProperties());
        session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_FAIL);
        session.setLocalRepositoryManager(this.system.newLocalRepositoryManager(session, new LocalRepository("libraries")));
        session.setTransferListener(new AbstractTransferListener() {
            @Override
            public void transferInitiated(@NotNull TransferEvent event) {
                logger.info("Downloading " + event.getResource().getRepositoryUrl() + event.getResource().getResourceName());
            }
        });
        session.setReadOnly();

        return session;
    }

    /**
     * Returns the default Maven Central repository.
     * This repository is used for resolving dependencies from Maven's official central repository.
     *
     * @return the Maven Central repository
     */
    private RemoteRepository CentralRepository() {
        return Boost.get();
    }

    /**
     * Returns the Sonatype OSS repository.
     * This repository is used for resolving dependencies from the Sonatype open-source repository.
     *
     * @return the Sonatype OSS repository
     */
    private RemoteRepository getSonatypeRepo() {
        return new RemoteRepository.Builder("sonatype", "default", "https://oss.sonatype.org/content/groups/public/").build();
    }
}

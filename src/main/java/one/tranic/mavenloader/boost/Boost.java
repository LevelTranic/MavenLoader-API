package one.tranic.mavenloader.boost;

import org.eclipse.aether.repository.RemoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

/**
 * The {@code Boost} class is responsible for selecting the fastest available Maven repository mirror.
 * It provides utility methods for determining if a repository is a central Maven repository, retrieving
 * repository configurations, and replacing repository URLs with the selected mirror URL.
 * <p>
 * On initialization, it tests multiple predefined mirrors and selects the fastest one, which is then
 * used as the default Maven repository for dependency resolution.
 */
public class Boost {

    private static final Map<String, String> mirrors = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger("MavenLibraryResolver");
    private static final String central = "https://repo.maven.apache.org/maven2";
    private static final String central2 = "https://repo1.maven.org/maven2";
    private static final URI centralUri = URI.create(central);
    private static final URI central2Uri = URI.create(central2);
    private static String maven = "";

    public static void ping() {
        mirrors();
        String s = System.getProperty("Maven.select");
        if (s != null) {
            String p = mirrors.get(s);
            if (p != null) {
                maven = p;
                logger.info("The mirror " + s + " (" + p + ") has been selected");
                return;
            }
        }
        selectMirror();
    }

    private static void mirrors() {
        if (!mirrors.isEmpty()) return;
        mirrors.put("central", "https://repo.maven.apache.org/maven2");
        mirrors.put("central1", "https://repo1.maven.org/maven2");
        mirrors.put("redhat", "https://repository.jboss.org/nexus/content/groups/public");
        mirrors.put("google-asia", "https://maven-central-asia.storage-download.googleapis.com/maven2/");
        mirrors.put("google-eu", "https://maven-central-eu.storage-download.googleapis.com/maven2/");
        mirrors.put("google-us", "https://maven-central.storage-download.googleapis.com/maven2/");
        String r = System.getProperty("Maven.central");
        if (r != null && !r.isEmpty()) {
            try {
                new URI(r);
            } catch (Exception e) {
                return;
            }
            mirrors.put("user-custom-mirror", r);
        }
    }

    /**
     * Checks if a given URL string belongs to the central Maven repository.
     *
     * <pre>{@code
     * boolean b = Boost.isCentral("https://repo.maven.apache.org/maven2");
     * }</pre>
     *
     * @param str the repository URL to check
     * @return {@code true} if the URL matches the central Maven repository, {@code false} otherwise
     */
    public static boolean isCentral(String str) {
        try {
            String s = URI.create(str).getHost();
            return Objects.equals(s, centralUri.getHost()) || Objects.equals(s, central2Uri.getHost());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if a given {@link RemoteRepository} belongs to the central Maven repository.
     *
     * @param remoteRepository the repository to check
     * @return {@code true} if the repository matches the central Maven repository, {@code false} otherwise
     */
    public static boolean isCentral(RemoteRepository remoteRepository) {
        return isCentral(remoteRepository.getUrl());
    }

    /**
     * Retrieves a {@link RemoteRepository} instance for the selected fastest Maven repository mirror.
     *
     * @return a {@link RemoteRepository} pointing to the fastest selected mirror
     */
    public static RemoteRepository get() {
        if (maven.isEmpty()) ping();
        return new RemoteRepository.Builder("central", "default", maven).build();
    }

    /**
     * Replaces a central repository URL with the selected fastest mirror URL, if applicable.
     *
     * @param remoteRepository the repository whose URL is to be replaced
     * @return a new {@link RemoteRepository} with the mirror URL if the repository is central,
     * or the original repository otherwise
     */
    public static RemoteRepository get(RemoteRepository remoteRepository) {
        if (isCentral(remoteRepository)) return get();
        return remoteRepository;
    }

    /**
     * Replaces the provided repository URL string with the selected fastest mirror URL
     * if the URL matches the central Maven repository.
     *
     * <pre>{@code
     * boolean b = Boost.replace("https://repo.maven.apache.org/maven2");
     * }</pre>
     *
     * @param str the repository URL to potentially replace
     * @return the mirror URL if the repository is central, or the original URL otherwise
     */
    public static String replace(String str) {
        return Objects.equals(str, central) ? maven : str;
    }

    /**
     * Selects the fastest available mirror from the predefined list by testing their response times.
     * The mirror with the shortest response time is selected as the default Maven repository.
     */
    private static void selectMirror() {
        ExecutorService executor = Executors.newCachedThreadPool();
        List<Future<MirrorResult>> futures = new ArrayList<>();
        for (Map.Entry<String, String> entry : mirrors.entrySet()) {
            futures.add(executor.submit(() -> testMirror(entry.getKey(), entry.getValue())));
        }

        long bestTime = Long.MAX_VALUE;
        String bestMirror = central;

        if (futures.isEmpty()) {
            maven = central;
            executor.shutdown();
            return;
        }

        for (Future<MirrorResult> future : futures) {
            try {
                MirrorResult result = future.get(4, TimeUnit.SECONDS);
                if (result.time < bestTime) {
                    bestTime = result.time;
                    bestMirror = result.url;
                }
            } catch (TimeoutException | InterruptedException | ExecutionException e) {
                logger.warn("Error testing mirror: " + e.getMessage());
            }
        }

        maven = bestMirror;
        logger.info("The fastest mirror is selected: " + bestMirror + " (" + bestTime + " ms)");

        executor.shutdown();
    }

    /**
     * Tests the response time of a given mirror by sending a GET request and measuring the round-trip time.
     *
     * @param name the name of the mirror being tested
     * @param url  the URL of the mirror being tested
     * @return a {@link MirrorResult} object containing the mirror URL and the response time
     */
    private static MirrorResult testMirror(String name, String url) {
        long start = System.currentTimeMillis();
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode == 200 || responseCode == 404 || responseCode == 302 || responseCode == 301) {
                long time = System.currentTimeMillis() - start;
                logger.info("Mirror " + name + " responded in " + time + " ms");
                return new MirrorResult(url, time);
            } else {
                logger.warn("Mirror " + name + " failed with response code: " + responseCode);
            }
        } catch (IOException e) {
            logger.warn("Mirror " + name + " failed to connect: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return new MirrorResult(url, Long.MAX_VALUE);
    }

    /**
     * A simple class to store the result of a mirror test, including the URL of the mirror and its response time.
     */
    private static class MirrorResult {
        String url;
        long time;

        MirrorResult(String url, long time) {
            this.url = url;
            this.time = time;
        }
    }
}

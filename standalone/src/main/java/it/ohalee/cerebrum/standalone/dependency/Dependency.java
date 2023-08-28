package it.ohalee.cerebrum.standalone.dependency;

import java.util.Locale;

public enum Dependency {

    SPRING_BOOT("org.springframework.boot", "spring-boot", "2.6.6"),
    SPRING_BOOT_AUTOCONFIGURE("org.springframework.boot", "spring-boot-autoconfigure", "2.6.6"),
    SPRING_BOOT_STARTER("org.springframework.boot", "spring-boot-starter", "2.6.6"),
    SPRING_BOOT_STARTER_LOG4J2("org.springframework.boot","spring-boot-starter-log4j2","2.6.6"),
    SPRING_BOOT_STARTER_LOGGING("org.springframework.boot","spring-boot-starter-logging","2.6.6"),
    SPRING_BOOT_STARTER_VALIDATOR("org.springframework.boot","spring-boot-starter-validation","2.6.6"),

    SPRING_SHEEL_AUTOCONFIGURE("org.springframework.shell", "spring-shell-autoconfigure", "2.1.1"),
    SPRING_SHELL_CORE("org.springframework.shell","spring-shell-core","2.1.1"),
    SPRING_SHELL_STANDARD("org.springframework.shell","spring-shell-standard","2.1.1"),
    SPRING_SHELL_STANDARD_COMMANDS("org.springframework.shell","spring-shell-standard-commands","2.1.1"),
    SPRING_SHELL_STARTER("org.springframework.shell","spring-shell-starter","2.1.1"),
    SPRING_SHELL_STARTER_JNA("org.springframework.shell","spring-shell-starter-jna","2.1.1"),
    SPRING_SHELL_TABLE("org.springframework.shell","spring-shell-table","2.1.1"),

    SPRING_AOP("org.springframework","spring-aop","6.0.11"),
    SPRING_BEANS("org.springframework","spring-beans","6.0.11"),
    SPRING_CONTEXT("org.springframework","spring-context","6.0.11"),
    SPRING_CORE("org.springframework","spring-core","6.0.11"),
    SPRING_EXPRESSION("org.springframework","spring-expression","6.0.11"),
    SPRING_JCL("org.springframework","spring-jcl","6.0.11"),
    SPRING_MESSAGING("org.springframework","spring-messaging","6.0.11"),

    BASEMENTLIB("com.github.ServerBasement.BasementLib","bukkit","1.3.42-all"),
    JACKSON_CORE("com.fasterxml.jackson.core","jackson-core","2.13.0"),
    JAVA_JNA("net.java.dev.jna","jna","5.12.1"),

    DOCKER_JAVA("com.github.docker-java","docker-java","3.3.3"),
    DOCKER_JAVA_API("com.github.docker-java","docker-java-api","3.3.3"),
    DOCKER_JAVA_CORE("com.github.docker-java","docker-java-core","3.3.3"),
    DOCKER_JAVA_TRANSPORT("com.github.docker-java","docker-java-transport","3.3.3"),
    DOCKER_JAVA_TRANSPORT_HTTPCLIENT5("com.github.docker-java","docker-java-transport-httpclient5","3.3.3"),
    DOCKER_JAVA_TRANSPORT_JERSEY("com.github.docker-java","docker-java-transport-jersey","3.3.3"),
    DOCKER_JAVA_TRANSPORT_NETTY("com.github.docker-java","docker-java-transport-netty","3.3.3"),

    COMMONS_COMPRESS("org.apache.commons","commons-compress","1.23.0"),
    COMMONS_LANG3("org.apache.commons","commons-lang3","3.12.0"),
    HTTPCLIENT5("org.apache.httpcomponents.client5","httpclient5","5.0.3"),
    HTTPCORE5_H2("org.apache.httpcomponents.core5","httpcore5-h2","5.2.2"),

    HTTPCLIENT("org.apache.httpcomponents.core5","httpcore5","5.0.2"),

    JCL_OVER_SLF4J("org.slf4j","jcl-over-slf4j","1.7.30"),

    LOG4J_API("org.apache.logging.log4j","log4j-api","2.20.0"),
    LOG4J_CORE("org.apache.logging.log4j","log4j-core","2.20.0"),
    LOG4J_JUL("org.apache.logging.log4j","log4j-jul","2.20.0"),
    LOG4J_SLF4J_IMPL("org.apache.logging.log4j","log4j-slf4j-impl","2.20.0"),
    LOG4J_TO_SLF4J("org.apache.logging.log4j","log4j-to-slf4j","2.20.0"),
    ;

    private final String mavenRepoPath;
    private final String version;

    private static final String MAVEN_FORMAT = "%s/%s/%s/%s-%s.jar";

    Dependency(String groupId, String artifactId, String version) {
        this.mavenRepoPath = String.format(MAVEN_FORMAT,
                rewriteEscaping(groupId).replace(".", "/"),
                rewriteEscaping(artifactId),
                version.replace("-all",""),
                rewriteEscaping(artifactId),
                version
        );
        this.version = version;
    }

    private static String rewriteEscaping(String s) {
        return s.replace("{}", ".");
    }

    public String getFileName(String classifier) {
        String name = name().toLowerCase(Locale.ROOT).replace('_', '-');
        String extra = classifier == null || classifier.isEmpty()
                ? ""
                : "-" + classifier;

        return name + "-" + this.version + extra + ".jar";
    }

    String getMavenRepoPath() {
        return this.mavenRepoPath;
    }

}

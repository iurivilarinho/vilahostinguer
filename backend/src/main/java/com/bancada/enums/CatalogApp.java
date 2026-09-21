package com.bancada.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Applications the panel knows how to install. Package names differ per distribution, so every
 * entry carries the package list for each supported package manager ({@code null} = not offered).
 */
@Schema(description = "Aplicativo do catálogo de instalação")
public enum CatalogApp {

    @Schema(description = "Nginx")
    NGINX("Nginx", "Servidor web e proxy reverso leve.", AppCategory.WEB_SERVER,
        "nginx", "nginx -v 2>&1 | head -1",
        "nginx", "nginx", "nginx", "nginx",
        "nginx", "nginx", null),

    @Schema(description = "Caddy")
    CADDY("Caddy", "Servidor web com HTTPS automático.", AppCategory.WEB_SERVER,
        "caddy", "caddy version 2>&1 | head -1",
        "caddy", "caddy", "caddy", "caddy",
        "caddy", "caddy", null),

    @Schema(description = "Node.js")
    NODEJS("Node.js", "Runtime JavaScript com npm.", AppCategory.RUNTIME,
        "node", "node --version",
        "nodejs npm", "nodejs npm", "nodejs npm", "nodejs npm",
        null, null, null),

    @Schema(description = "Java 17")
    JAVA17("Java 17", "OpenJDK 17 (JRE headless) para aplicações Spring e afins.", AppCategory.RUNTIME,
        "java", "java -version 2>&1 | head -1",
        "openjdk17-jre-headless", "openjdk-17-jre-headless", "java-17-openjdk-headless", "jre17-openjdk-headless",
        null, null, null),

    @Schema(description = "Java 21")
    JAVA21("Java 21", "OpenJDK 21 (JRE headless).", AppCategory.RUNTIME,
        "java", "java -version 2>&1 | head -1",
        "openjdk21-jre-headless", "openjdk-21-jre-headless", "java-21-openjdk-headless", "jre21-openjdk-headless",
        null, null, null),

    @Schema(description = "Python 3")
    PYTHON("Python 3", "Interpretador Python com pip.", AppCategory.RUNTIME,
        "python3", "python3 --version 2>&1",
        "python3 py3-pip", "python3 python3-pip", "python3 python3-pip", "python python-pip",
        null, null, null),

    @Schema(description = "PostgreSQL")
    POSTGRESQL("PostgreSQL", "Banco de dados relacional.", AppCategory.DATABASE,
        "postgres", "postgres --version 2>&1 || psql --version 2>&1",
        "postgresql postgresql-client", "postgresql", "postgresql-server", "postgresql",
        "postgresql", "postgresql", "rc-service postgresql setup || true"),

    @Schema(description = "MariaDB")
    MARIADB("MariaDB", "Banco de dados compatível com MySQL.", AppCategory.DATABASE,
        "mariadbd", "mariadb --version 2>&1 || mysql --version 2>&1",
        "mariadb mariadb-client", "mariadb-server", "mariadb-server", "mariadb",
        "mariadb", "mariadb", "rc-service mariadb setup || true"),

    @Schema(description = "Redis")
    REDIS("Redis", "Banco em memória para cache e filas.", AppCategory.DATABASE,
        "redis-server", "redis-server --version 2>&1",
        "redis", "redis-server", "redis", "redis",
        "redis", "redis-server", null),

    @Schema(description = "Docker")
    DOCKER("Docker", "Contêineres. Exige kernel com cgroups e namespaces completos.", AppCategory.CONTAINER,
        "docker", "docker --version 2>&1",
        "docker docker-cli-compose", "docker.io docker-compose", "moby-engine docker-compose", "docker docker-compose",
        "docker", "docker", null),

    @Schema(description = "Git")
    GIT("Git", "Controle de versão.", AppCategory.TOOL,
        "git", "git --version",
        "git", "git", "git", "git",
        null, null, null),

    @Schema(description = "Certbot")
    CERTBOT("Certbot", "Certificados HTTPS gratuitos (Let's Encrypt).", AppCategory.TOOL,
        "certbot", "certbot --version 2>&1",
        "certbot", "certbot", "certbot", "certbot",
        null, null, null),

    @Schema(description = "Ferramentas básicas")
    ESSENTIALS("Ferramentas básicas", "curl, wget, htop, nano e rsync.", AppCategory.TOOL,
        "htop", "htop --version 2>&1 | head -1",
        "curl wget htop nano rsync", "curl wget htop nano rsync", "curl wget htop nano rsync", "curl wget htop nano rsync",
        null, null, null);

    private final String displayName;
    private final String description;
    private final AppCategory category;
    private final String binary;
    private final String versionCommand;
    private final String apkPackages;
    private final String aptPackages;
    private final String dnfPackages;
    private final String pacmanPackages;
    private final String serviceName;
    private final String aptServiceName;
    private final String apkPostInstall;

    CatalogApp(String displayName, String description, AppCategory category, String binary, String versionCommand,
               String apkPackages, String aptPackages, String dnfPackages, String pacmanPackages,
               String serviceName, String aptServiceName, String apkPostInstall) {
        this.displayName = displayName;
        this.description = description;
        this.category = category;
        this.binary = binary;
        this.versionCommand = versionCommand;
        this.apkPackages = apkPackages;
        this.aptPackages = aptPackages;
        this.dnfPackages = dnfPackages;
        this.pacmanPackages = pacmanPackages;
        this.serviceName = serviceName;
        this.aptServiceName = aptServiceName;
        this.apkPostInstall = apkPostInstall;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public AppCategory getCategory() {
        return category;
    }

    public String getBinary() {
        return binary;
    }

    public String getVersionCommand() {
        return versionCommand;
    }

    public String packagesFor(PackageManager manager) {
        return switch (manager) {
            case APK -> apkPackages;
            case APT -> aptPackages;
            case DNF -> dnfPackages;
            case PACMAN -> pacmanPackages;
        };
    }

    public String serviceFor(PackageManager manager) {
        if (manager == PackageManager.APT && aptServiceName != null) {
            return aptServiceName;
        }
        return serviceName;
    }

    public String postInstallFor(PackageManager manager) {
        return manager == PackageManager.APK ? apkPostInstall : null;
    }
}

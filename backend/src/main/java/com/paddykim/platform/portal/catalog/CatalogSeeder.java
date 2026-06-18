package com.paddykim.platform.portal.catalog;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CatalogSeeder implements CommandLineRunner {

    private final ApplicationRepository applicationRepository;

    public CatalogSeeder(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (applicationRepository.existsByName("platform-app")) {
            return;
        }

        Application application = new Application(
                "platform-app",
                "Sample workload managed by the Platform Starter Kit",
                "platform-team",
                "https://github.com/paddyKim/platform-app"
        );

        ApplicationEnvironment dev = new ApplicationEnvironment(
                "dev",
                "dev",
                "platform-dev",
                "platform-deploy/environments/dev/values.yaml",
                "http://localhost:3000"
        );

        dev.addComponent(new ApplicationComponent(
                "platform-api",
                "api",
                "platform-api",
                "platform-api",
                "ghcr.io/paddykim/platform-api"
        ));
        dev.addComponent(new ApplicationComponent(
                "platform-web",
                "web",
                "platform-web",
                "platform-web",
                "ghcr.io/paddykim/platform-web"
        ));
        dev.addComponent(new ApplicationComponent(
                "platform-mariadb",
                "database",
                "platform-mariadb",
                "platform-mariadb",
                "mariadb"
        ));

        application.addEnvironment(dev);
        applicationRepository.save(application);
    }
}

package com.paddykim.platform.portal.source;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SourceRepositorySeeder implements CommandLineRunner {

    private final SourceRepositoryRepository sourceRepositoryRepository;

    public SourceRepositorySeeder(SourceRepositoryRepository sourceRepositoryRepository) {
        this.sourceRepositoryRepository = sourceRepositoryRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seed(
                "platform-app",
                SourceRepositoryProvider.GITHUB,
                "https://github.com/paddyKim/platform-app",
                "https://api.github.com",
                "paddyKim",
                "local-placeholder-token",
                "main",
                "Application source repository for API and Web images"
        );
        seed(
                "platform-deploy",
                SourceRepositoryProvider.GITHUB,
                "https://github.com/paddyKim/platform-deploy",
                "https://api.github.com",
                "paddyKim",
                "local-placeholder-token",
                "main",
                "GitOps deployment repository watched by ArgoCD"
        );
    }

    private void seed(
            String name,
            SourceRepositoryProvider provider,
            String repositoryUrl,
            String apiBaseUrl,
            String accountName,
            String accessToken,
            String defaultBranch,
            String description
    ) {
        if (sourceRepositoryRepository.existsByRepositoryUrl(repositoryUrl)) {
            return;
        }

        sourceRepositoryRepository.save(new SourceRepository(
                name,
                provider,
                repositoryUrl,
                apiBaseUrl,
                accountName,
                accessToken,
                defaultBranch,
                description
        ));
    }
}

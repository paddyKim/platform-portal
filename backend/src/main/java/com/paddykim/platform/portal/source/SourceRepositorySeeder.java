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
                "https://github.com/paddyKim/platform-app",
                "main",
                "Application source repository for API and Web images"
        );
        seed(
                "platform-deploy",
                "https://github.com/paddyKim/platform-deploy",
                "main",
                "GitOps deployment repository watched by ArgoCD"
        );
    }

    private void seed(String name, String repositoryUrl, String defaultBranch, String description) {
        if (sourceRepositoryRepository.existsByRepositoryUrl(repositoryUrl)) {
            return;
        }

        sourceRepositoryRepository.save(new SourceRepository(name, repositoryUrl, defaultBranch, description));
    }
}

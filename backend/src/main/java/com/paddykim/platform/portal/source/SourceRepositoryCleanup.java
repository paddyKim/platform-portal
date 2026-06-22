package com.paddykim.platform.portal.source;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SourceRepositoryCleanup implements CommandLineRunner {

    private static final String LEGACY_PLACEHOLDER_TOKEN = "local-placeholder-token";

    private final SourceRepositoryRepository sourceRepositoryRepository;

    public SourceRepositoryCleanup(SourceRepositoryRepository sourceRepositoryRepository) {
        this.sourceRepositoryRepository = sourceRepositoryRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        sourceRepositoryRepository.deleteByAccessToken(LEGACY_PLACEHOLDER_TOKEN);
        sourceRepositoryRepository.deleteByProviderIsNull();
    }
}

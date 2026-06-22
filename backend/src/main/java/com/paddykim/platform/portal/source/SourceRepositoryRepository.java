package com.paddykim.platform.portal.source;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceRepositoryRepository extends JpaRepository<SourceRepository, Long> {

    boolean existsByRepositoryUrl(String repositoryUrl);

    void deleteByAccessToken(String accessToken);

    void deleteByProviderIsNull();
}

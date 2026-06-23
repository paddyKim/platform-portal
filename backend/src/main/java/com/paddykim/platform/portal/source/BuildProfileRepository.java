package com.paddykim.platform.portal.source;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuildProfileRepository extends JpaRepository<BuildProfile, Long> {

    List<BuildProfile> findBySourceRepositoryId(Long sourceRepositoryId);

    Optional<BuildProfile> findByIdAndSourceRepositoryId(Long id, Long sourceRepositoryId);

    void deleteBySourceRepositoryId(Long sourceRepositoryId);
}

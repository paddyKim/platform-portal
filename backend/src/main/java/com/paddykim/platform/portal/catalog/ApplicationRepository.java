package com.paddykim.platform.portal.catalog;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByName(String name);

    Optional<Application> findByName(String name);

    @EntityGraph(attributePaths = "environments")
    Optional<Application> findWithEnvironmentsById(Long id);
}

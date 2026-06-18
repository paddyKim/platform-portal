package com.paddykim.platform.portal.catalog;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {

    private final ApplicationRepository applicationRepository;

    public CatalogService(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @Transactional(readOnly = true)
    public List<CatalogApplicationResponse> listApplications() {
        return applicationRepository.findAll().stream()
                .map(application -> CatalogApplicationResponse.from(application, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public CatalogApplicationResponse getApplication(Long id) {
        Application application = applicationRepository.findWithEnvironmentsById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(id));

        return CatalogApplicationResponse.from(application, true);
    }
}

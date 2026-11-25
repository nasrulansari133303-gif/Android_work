package com.kaammitra.service;

import com.kaammitra.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/services")
public class ServiceCatalogController {

    private final ServiceRepository serviceRepository;

    public ServiceCatalogController(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ServiceEntity>>> listActiveServices(
            @RequestParam(value = "q", required = false) String query) {
        List<ServiceEntity> all = serviceRepository.findByActiveTrueOrderByNameAsc();
        if (query != null && !query.isBlank()) {
            String lower = query.trim().toLowerCase();
            all = all.stream()
                    .filter(s -> s.getName().toLowerCase().contains(lower)
                            || (s.getDescription() != null && s.getDescription().toLowerCase().contains(lower)))
                    .toList();
        }
        return ResponseEntity.ok(ApiResponse.ok(all));
    }
}

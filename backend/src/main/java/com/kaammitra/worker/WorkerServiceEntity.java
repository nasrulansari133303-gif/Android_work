package com.kaammitra.worker;

import com.kaammitra.service.ServiceEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "worker_services", uniqueConstraints = @UniqueConstraint(columnNames = {"worker_id", "service_id"}))
public class WorkerServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "worker_id", nullable = false)
    private WorkerEntity worker;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceEntity service;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public WorkerEntity getWorker() { return worker; }
    public void setWorker(WorkerEntity worker) { this.worker = worker; }
    public ServiceEntity getService() { return service; }
    public void setService(ServiceEntity service) { this.service = service; }
}

package com.kaammitra.job;

import jakarta.persistence.*;

@Entity
@Table(name = "job_photos")
public class JobPhotoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private JobEntity job;

    @Column(name = "photo_url", nullable = false, length = 500)
    private String photoUrl;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public JobEntity getJob() { return job; }
    public void setJob(JobEntity job) { this.job = job; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
}

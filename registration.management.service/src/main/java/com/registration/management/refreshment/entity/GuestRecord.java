package com.registration.management.refreshment.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "plan")
@Entity
@Table(name = "guest_records")
public class GuestRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false, foreignKey = @ForeignKey(name = "fk_guest_records_plan"))
    private RefreshmentPlan plan;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String name;

    @Size(max = 255)
    @Column(length = 255)
    private String designation;

    @Size(max = 255)
    @Column(length = 255)
    private String organization;

    @Size(max = 50)
    @Column(name = "contact_number", length = 50)
    private String contactNumber;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

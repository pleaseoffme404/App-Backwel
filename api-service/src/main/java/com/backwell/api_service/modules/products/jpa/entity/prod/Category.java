package com.backwell.api_service.modules.products.jpa.entity.prod;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    @Id
    private UUID id;

    @Setter
    @Column(length = 100, nullable = false, unique = true)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Category> children = new ArrayList<>();

    @Column(nullable = false)
    @JdbcTypeCode(Types.TIMESTAMP_WITH_TIMEZONE)
    private Instant createdAt;

    @PrePersist
    public void onSave() {
        simpleSelfParentConstraint();
        createdAt = Instant.now();
    }
    @PreUpdate
    protected void onUpdate() {
        simpleSelfParentConstraint();
    }

    private void simpleSelfParentConstraint() {
        if (this.parent != null && this.parent.getId().equals(this.id)) {
            throw new IllegalStateException("Self-referencing categories are not Allowed");
        }
    }

   public void setParent(Category parent) {
        if (parent != null && this.id != null && parent.getId().equals(this.id)) {
            throw new IllegalArgumentException("Self-referencing categories are not Allowed");
        }
        this.parent = parent;
    }

    @Builder
    public Category(UUID id, String name, Category parent) {
        this.id = id;
        this.name = name;
        this.parent = parent;
    }

    public void addChild(Category child) {
        children.add(child);
        child.setParent(this);
    }

    public void removeChild(Category child) {
        children.remove(child);
        child.setParent(null);
    }

}

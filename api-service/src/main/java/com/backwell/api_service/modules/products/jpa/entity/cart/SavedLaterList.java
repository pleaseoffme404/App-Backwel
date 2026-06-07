package com.backwell.api_service.modules.products.jpa.entity.cart;


import com.backwell.api_service.modules.users.entity.UserInfo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@NamedEntityGraphs({
        @NamedEntityGraph(name = "SavedLaterList.withItemsAndVariants", attributeNodes = {
                @NamedAttributeNode(value = "items", subgraph = "items-subgraph")
        },
                subgraphs = {
                        @NamedSubgraph(
                                name = "items-subgraph",
                                attributeNodes = {
                                        @NamedAttributeNode(value = "item")
                                }
                        )
                })
})
public class SavedLaterList {
    @Id
    private UUID id;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private UserInfo userInfo;

    @OneToMany(
            mappedBy = "list",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<SavedLaterItem> items = new ArrayList<>();

    @Column(nullable = false)
    @JdbcTypeCode(Types.TIMESTAMP_WITH_TIMEZONE)
    private Instant lastUpdate;

    @PrePersist
    protected void onCreate() {
        lastUpdate = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdate = Instant.now();
    }

    private SavedLaterList (UUID id, UserInfo userInfo) {
        this.id = id;
        this.userInfo = userInfo;
    }

    public static SavedLaterList initForUser(UUID id, UserInfo userInfo) {
        return new SavedLaterList(id, userInfo);
    }

    /**
     * @implNote To be called only in service layer with previous validation*/
    public void addItem(SavedLaterItem item) {
        this.items.add(item);
        item.setList(this);
    }

    /**
     * @implNote To be called only in service layer with previous validation*/
    public void removeItem(SavedLaterItem item) {
        this.items.remove(item);
        item.setList(null);
    }
}

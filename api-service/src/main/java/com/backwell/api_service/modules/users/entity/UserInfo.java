package com.backwell.api_service.modules.users.entity;

import com.backwell.api_service.common.config.user.UserSession;
import com.backwell.api_service.modules.users.dto.CompleteAccountRequest;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Table(
        indexes = {
                @Index(name = "idx_user_info_email", columnList = "email")
        }
)
public class UserInfo {
    @Id
    private UUID uuid;

    @Column(unique = true, nullable = false)
    private String email;

    @Setter
    @Column(nullable = false)
    private String name;

    @Setter
    @Column(nullable = false)
    private String surname;

    @Column(length = 2, nullable = false)
    private String countryCode;

    @Column(length = 3, nullable = false)
    private String currencyCode;

    @Setter
    @Column(nullable = false)
    private String phoneNumber;

    @Setter
    @Column(nullable = false, columnDefinition = "TEXT")
    private String pictureUrl;

    @OneToMany(mappedBy = "user",  fetch = FetchType.LAZY,  cascade = CascadeType.ALL)
    private List<UserAddress> userAddresses = new ArrayList<>();

    private Instant createdAt;
    private Instant lastUpdated;

    @PrePersist
    public void onSave(){
        createdAt = Instant.now();
        lastUpdated = Instant.now();
    }

    @PreUpdate
    public void onUpdate(){
        lastUpdated = Instant.now();
    }

    public void addUserAddress(UserAddress userAddress){
        userAddresses.add(userAddress);
        userAddress.setUser(this);
    }

    public void removeUserAddress(UserAddress userAddress){
        userAddresses.remove(userAddress);
        userAddress.setUser(null);
    }

    private UserInfo (
            UUID uuid,
            String email,
            String name,
            String surname,
            String countryCode,
            String currencyCode,
            String phoneNumber,
            String pictureUrl
    ) {
        this.uuid = uuid;
        this.email = email;
        this.name = name;
        this.surname = surname;
        this.countryCode = countryCode;
        this.currencyCode = currencyCode;
        this.phoneNumber = phoneNumber;
        this.pictureUrl = pictureUrl;
        this.userAddresses = new ArrayList<>();
    }

    /**
     * Creates a UserInfo instance with the provided values
     * @implNote This method returns an object with NO ADDRESSES, only an appendable array list*/
    public static UserInfo from(CompleteAccountRequest req, UserSession session) {
        String avatarUrl = buildDefaultAvatar(req.name(), req.surname());
        UserInfo user = new UserInfo(
                session.uuid(),
                session.email(),
                req.name(),
                req.surname(),
                req.countryCode(),
                req.currencyCode(),
                req.phoneNumber(),
                avatarUrl
        );

        UserAddress firstAddress = new UserAddress(
                0,
                req.addressName(),
                req.firstAddress().googleAddressDTO().toEntity()
        );

        user.addUserAddress(firstAddress);

        return user;
    }

    private static String buildDefaultAvatar(String name, String surname) {
        String fullName = UriUtils.encode(name + " " + surname, StandardCharsets.UTF_8);
        return String.format("https://ui-avatars.com/api/?name=%s&background=F6F6EF&length=1&font-size=0.8", fullName);
    }
}

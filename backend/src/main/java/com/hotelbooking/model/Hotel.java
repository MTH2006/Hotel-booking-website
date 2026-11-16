package com.hotelbooking.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hotels")
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String badge;

    @Column(name = "badge_type")
    private String badgeType;

    @Column
    private Double rating;

    @Column(name = "reviews_count")
    private Integer reviewsCount;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private Long price;

    @Column(name = "original_price")
    private Long originalPrice;

    @Column
    private String image;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String address;
    private String phone;
    private String email;
    @Column(columnDefinition = "TEXT")
    private String images;
    @Column(columnDefinition = "TEXT")
    private String amenities;
    private Integer stars;
    private Boolean verified = false;
    private String checkin;
    private String checkout;
    @Column(name = "cancellation_policy", columnDefinition = "TEXT")
    private String cancellationPolicy;
    @Column(name = "stay_policy", columnDefinition = "TEXT")
    private String stayPolicy;
    private Boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Hotel() {}

    public Hotel(Long id, String name, String badge, String badgeType, Double rating, Integer reviewsCount, String location, String city, Long price, Long originalPrice, String image, String description) {
        this.id = id;
        this.name = name;
        this.badge = badge;
        this.badgeType = badgeType;
        this.rating = rating;
        this.reviewsCount = reviewsCount;
        this.location = location;
        this.city = city;
        this.price = price;
        this.originalPrice = originalPrice;
        this.image = image;
        this.description = description;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }

    public String getBadgeType() { return badgeType; }
    public void setBadgeType(String badgeType) { this.badgeType = badgeType; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public Integer getReviewsCount() { return reviewsCount; }
    public void setReviewsCount(Integer reviewsCount) { this.reviewsCount = reviewsCount; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public Long getPrice() { return price; }
    public void setPrice(Long price) { this.price = price; }

    public Long getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(Long originalPrice) { this.originalPrice = originalPrice; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getImages() { return images; }
    public void setImages(String images) { this.images = images; }
    public String getAmenities() { return amenities; }
    public void setAmenities(String amenities) { this.amenities = amenities; }
    public Integer getStars() { return stars; }
    public void setStars(Integer stars) { this.stars = stars; }
    public Boolean getVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }
    public String getCheckin() { return checkin; }
    public void setCheckin(String checkin) { this.checkin = checkin; }
    public String getCheckout() { return checkout; }
    public void setCheckout(String checkout) { this.checkout = checkout; }
    public String getCancellationPolicy() { return cancellationPolicy; }
    public void setCancellationPolicy(String cancellationPolicy) { this.cancellationPolicy = cancellationPolicy; }
    public String getStayPolicy() { return stayPolicy; }
    public void setStayPolicy(String stayPolicy) { this.stayPolicy = stayPolicy; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

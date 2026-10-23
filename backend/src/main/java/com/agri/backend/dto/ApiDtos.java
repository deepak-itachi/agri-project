package com.agri.backend.dto;

import com.agri.backend.model.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response shapes. Entities are never returned directly from the API, so private fields
 * (password hash, payout details, seller contact info) cannot leak to the wrong role by accident.
 */
public final class ApiDtos {
    private ApiDtos() {}

    /** The account as seen by its owner or by an admin. Never contains the password hash. */
    public record UserView(Long id, String email, String fullName, String phone, String city, String businessName,
                           String role, String upiId, String bankAccountNumber, String bankIfsc, Long farmerId,
                           boolean active, LocalDateTime createdAt, LocalDateTime lastLoginAt, int loginCount) {
        public static UserView of(User u) {
            return new UserView(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(), u.getCity(),
                    u.getBusinessName(), u.getRole() == null ? null : u.getRole().name(), u.getUpiId(),
                    u.getBankAccountNumber(), u.getBankIfsc(), u.getFarmerId(), u.isEnabled(),
                    u.getCreatedAt(), u.getLastLoginAt(), u.getLoginCount() == null ? 0 : u.getLoginCount());
        }
    }

    /** What a BUYER (or anonymous visitor) sees: product + shop name only. No seller id, email, phone or payout info. */
    public record ListingView(Long id, String cropName, Double quantityKg, Double pricePerKg, String location,
                              String description, String imageUrl, String sellerName, LocalDate availableFrom,
                              String status, LocalDateTime createdAt) {}

    /** What a SELLER sees for their own listings. */
    public record SellerListingView(Long id, String cropName, Double quantityKg, Double pricePerKg, String location,
                                    String description, String imageUrl, boolean customImage, LocalDate availableFrom,
                                    String status, LocalDateTime createdAt) {}

    public record AdminListingView(Long id, String cropName, Double quantityKg, Double pricePerKg, String location,
                                   String description, String imageUrl, String status, Long sellerUserId,
                                   String sellerName, String sellerEmail, LocalDateTime createdAt) {}

    /** Customer details a seller may see for people who bought from THEM. */
    public record CustomerView(Long buyerId, String name, String email, String phone, String city) {}

    public record SellerOrderView(Long id, Long listingId, String cropName, String imageUrl, Double quantityKg,
                                  Double pricePerKg, Double totalAmount, String status, LocalDateTime createdAt,
                                  LocalDateTime paidAt, CustomerView customer) {}

    public record BuyerOrderView(Long id, Long listingId, String cropName, String imageUrl, Double quantityKg,
                                 Double pricePerKg, Double totalAmount, String status, LocalDateTime createdAt,
                                 String sellerName) {}

    public record AdminOrderView(Long id, String cropName, Double quantityKg, Double totalAmount, String status,
                                 LocalDateTime createdAt, String buyerName, String buyerEmail, String sellerName,
                                 String sellerEmail) {}

    public record CustomerSummary(Long buyerId, String name, String email, String phone, String city, int orders,
                                  double totalSpent, LocalDateTime lastOrderAt) {}

    public record CropSales(String cropName, double kgSold, double revenue) {}

    public record SellerStats(double netBusiness, double pendingAmount, int totalOrders, int paidOrders,
                              double kgSold, int customers, int activeListings) {}

    public record SellerDashboard(SellerStats stats, List<SellerOrderView> recentOrders,
                                  List<CustomerSummary> customers, List<CropSales> cropBreakdown) {}
}

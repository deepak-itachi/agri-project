package com.agri.backend.service;

import com.agri.backend.dto.ApiDtos.AdminListingView;
import com.agri.backend.dto.ApiDtos.ListingView;
import com.agri.backend.dto.ApiDtos.SellerListingView;
import com.agri.backend.model.ProduceListing;
import com.agri.backend.model.User;
import com.agri.backend.repository.CropImageRepository;
import com.agri.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns ProduceListing rows into the right view for each audience and resolves the product picture:
 * the seller's own upload if there is one, otherwise the crop-dataset image, otherwise a default photo.
 * Buyers only ever get the shop name - never the seller's id, email, phone or payout details.
 */
@Service
public class ListingService {

    public static final String DEFAULT_IMAGE = "/images/crops/default.jpg";

    private final UserRepository userRepository;
    private final CropImageRepository cropImageRepository;

    public ListingService(UserRepository userRepository, CropImageRepository cropImageRepository) {
        this.userRepository = userRepository;
        this.cropImageRepository = cropImageRepository;
    }

    public String resolveImage(ProduceListing l) {
        if (l.getImageUrl() != null && !l.getImageUrl().isBlank()) return l.getImageUrl();
        if (l.getCropName() == null) return DEFAULT_IMAGE;
        return cropImageRepository.findFirstByCropNameIgnoreCase(l.getCropName())
                .map(ci -> ci.getImageUrl())
                .orElse(DEFAULT_IMAGE);
    }

    /** Public shop name for a seller. Falls back to "<first name>'s Farm" so a full personal name is never shown. */
    public static String publicSellerName(User seller) {
        if (seller == null) return "Local farmer";
        if (seller.getBusinessName() != null && !seller.getBusinessName().isBlank()) return seller.getBusinessName();
        String full = seller.getFullName();
        if (full == null || full.isBlank()) return "Local farmer";
        return full.trim().split("\\s+")[0] + "'s Farm";
    }

    public List<ListingView> toBuyerViews(List<ProduceListing> listings) {
        Map<Long, String> names = new HashMap<>();
        return listings.stream().map(l -> {
            String name = names.computeIfAbsent(l.getSellerUserId() == null ? -1L : l.getSellerUserId(),
                    id -> id < 0 ? "Local farmer"
                            : publicSellerName(userRepository.findById(id).orElse(null)));
            return toBuyerView(l, name);
        }).toList();
    }

    public ListingView toBuyerView(ProduceListing l) {
        User seller = l.getSellerUserId() == null ? null : userRepository.findById(l.getSellerUserId()).orElse(null);
        return toBuyerView(l, publicSellerName(seller));
    }

    private ListingView toBuyerView(ProduceListing l, String sellerName) {
        return new ListingView(l.getId(), l.getCropName(), l.getQuantityKg(), l.getExpectedPricePerKg(),
                l.getLocation(), l.getDescription(), resolveImage(l), sellerName, l.getAvailableFrom(),
                l.getStatus(), l.getCreatedAt());
    }

    public SellerListingView toSellerView(ProduceListing l) {
        boolean custom = l.getImageUrl() != null && !l.getImageUrl().isBlank();
        return new SellerListingView(l.getId(), l.getCropName(), l.getQuantityKg(), l.getExpectedPricePerKg(),
                l.getLocation(), l.getDescription(), resolveImage(l), custom, l.getAvailableFrom(),
                l.getStatus(), l.getCreatedAt());
    }

    public AdminListingView toAdminView(ProduceListing l) {
        User seller = l.getSellerUserId() == null ? null : userRepository.findById(l.getSellerUserId()).orElse(null);
        return new AdminListingView(l.getId(), l.getCropName(), l.getQuantityKg(), l.getExpectedPricePerKg(),
                l.getLocation(), l.getDescription(), resolveImage(l), l.getStatus(), l.getSellerUserId(),
                seller == null ? null : publicSellerName(seller), seller == null ? null : seller.getEmail(),
                l.getCreatedAt());
    }
}

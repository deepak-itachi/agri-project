package com.agri.backend.controller;

import com.agri.backend.dto.ApiDtos.*;
import com.agri.backend.exception.ApiException;
import com.agri.backend.model.*;
import com.agri.backend.repository.*;
import com.agri.backend.security.AuthInterceptor;
import com.agri.backend.service.ImageStorageService;
import com.agri.backend.service.ListingService;
import com.agri.backend.service.OrderViewService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin console API (ADMIN role only - enforced in AccessRules). This is the "data store" screen: the admin can see every
 * account and who has signed in, and can edit user details, product listings, the crop-image dataset and crop guides.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final LoginEventRepository loginEventRepository;
    private final ProduceListingRepository listingRepository;
    private final OrderRepository orderRepository;
    private final CropImageRepository cropImageRepository;
    private final CropGuideRepository cropGuideRepository;
    private final ListingService listingService;
    private final OrderViewService orderViews;
    private final ImageStorageService imageStorage;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AdminController(UserRepository userRepository, LoginEventRepository loginEventRepository,
                           ProduceListingRepository listingRepository, OrderRepository orderRepository,
                           CropImageRepository cropImageRepository, CropGuideRepository cropGuideRepository,
                           ListingService listingService, OrderViewService orderViews,
                           ImageStorageService imageStorage) {
        this.userRepository = userRepository;
        this.loginEventRepository = loginEventRepository;
        this.listingRepository = listingRepository;
        this.orderRepository = orderRepository;
        this.cropImageRepository = cropImageRepository;
        this.cropGuideRepository = cropGuideRepository;
        this.listingService = listingService;
        this.orderViews = orderViews;
        this.imageStorage = imageStorage;
    }

    // ------------------------------------------------------------------- stats

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("learners", userRepository.countByRole(User.Role.FARMER));
        m.put("sellers", userRepository.countByRole(User.Role.SELLER));
        m.put("buyers", userRepository.countByRole(User.Role.BUYER));

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        m.put("loginsToday", loginEventRepository.countByEventTypeAndCreatedAtAfter("LOGIN", startOfDay));
        Set<Long> activeNow = loginEventRepository
                .findByEventTypeAndCreatedAtAfter("LOGIN", LocalDateTime.now().minusMinutes(30)).stream()
                .map(LoginEvent::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        m.put("activeLast30Min", activeNow.size());

        List<ProduceListing> listings = listingRepository.findAll();
        m.put("listings", listings.size());
        m.put("availableListings", listings.stream().filter(l -> "AVAILABLE".equals(l.getStatus())).count());

        List<Order> orders = orderRepository.findAll();
        m.put("orders", orders.size());
        m.put("paidOrders", orders.stream().filter(o -> OrderViewService.BUSINESS.contains(o.getStatus())).count());
        m.put("totalBusiness", orders.stream().filter(o -> OrderViewService.BUSINESS.contains(o.getStatus()))
                .mapToDouble(o -> o.getTotalAmountRupees() == null ? 0 : o.getTotalAmountRupees()).sum());
        return m;
    }

    // ------------------------------------------------------------------- users

    @GetMapping("/users")
    public List<UserView> users(@RequestParam(required = false) String role) {
        List<User> rows;
        if (role == null || role.isBlank() || "ALL".equalsIgnoreCase(role)) {
            rows = userRepository.findAllByOrderByCreatedAtDesc();
        } else {
            rows = userRepository.findByRoleOrderByCreatedAtDesc(User.Role.valueOf(role.trim().toUpperCase()));
        }
        return rows.stream().map(UserView::of).toList();
    }

    @Data
    public static class UserUpdate {
        private String email;
        private String fullName;
        private String phone;
        private String city;
        private String businessName;
        private String upiId;
        private String bankAccountNumber;
        private String bankIfsc;
        private Boolean active;
    }

    @PutMapping("/users/{id}")
    public UserView updateUser(@PathVariable Long id, @RequestBody UserUpdate req, HttpServletRequest http) {
        User admin = AuthInterceptor.currentUser(http);
        User u = userRepository.findById(id).orElseThrow(() -> ApiException.notFound("That user was not found."));

        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            String email = req.getEmail().trim().toLowerCase(Locale.ROOT);
            if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) throw ApiException.badRequest("That email address is not valid.");
            Optional<User> clash = userRepository.findByEmailAndRole(email, u.getRole());
            if (clash.isPresent() && !clash.get().getId().equals(u.getId())) {
                throw ApiException.conflict("Another " + u.getRole().name().toLowerCase() + " account already uses that email.");
            }
            u.setEmail(email);
        }
        if (req.getFullName() != null && !req.getFullName().isBlank()) u.setFullName(req.getFullName().trim());
        u.setPhone(blankToNull(req.getPhone()));
        u.setCity(blankToNull(req.getCity()));
        if (u.getRole() == User.Role.SELLER) {
            u.setBusinessName(blankToNull(req.getBusinessName()));
            u.setUpiId(blankToNull(req.getUpiId()));
            u.setBankAccountNumber(blankToNull(req.getBankAccountNumber()));
            u.setBankIfsc(blankToNull(req.getBankIfsc()));
        }
        if (req.getActive() != null) {
            if (!req.getActive() && u.getId().equals(admin.getId())) {
                throw ApiException.badRequest("You cannot disable your own admin account.");
            }
            u.setActive(req.getActive());
        }
        return UserView.of(userRepository.save(u));
    }

    @Data
    public static class PasswordReset {
        private String newPassword;
    }

    @PostMapping("/users/{id}/reset-password")
    public Map<String, Object> resetPassword(@PathVariable Long id, @RequestBody PasswordReset req) {
        User u = userRepository.findById(id).orElseThrow(() -> ApiException.notFound("That user was not found."));
        if (req.getNewPassword() == null || req.getNewPassword().length() < 8) {
            throw ApiException.badRequest("The new password must be at least 8 characters long.");
        }
        u.setPasswordHash(encoder.encode(req.getNewPassword()));
        userRepository.save(u);
        return Map.of("ok", true);
    }

    // ---------------------------------------------------------- login activity

    @GetMapping("/login-events")
    public List<LoginEvent> loginEvents(@RequestParam(required = false) String role) {
        if (role == null || role.isBlank() || "ALL".equalsIgnoreCase(role)) {
            return loginEventRepository.findTop300ByOrderByCreatedAtDesc();
        }
        return loginEventRepository.findTop300ByRoleOrderByCreatedAtDesc(role.trim().toUpperCase());
    }

    // ---------------------------------------------------------------- listings

    @GetMapping("/listings")
    public List<AdminListingView> listings() {
        return listingRepository.findAllByOrderByCreatedAtDesc().stream().map(listingService::toAdminView).toList();
    }

    @Data
    public static class ListingEdit {
        private String cropName;
        private Double quantityKg;
        private Double pricePerKg;
        private String location;
        private String description;
        private String status; // AVAILABLE, SOLD_OUT, HIDDEN
    }

    @PutMapping("/listings/{id}")
    public AdminListingView editListing(@PathVariable Long id, @RequestBody ListingEdit req) {
        ProduceListing l = listingRepository.findById(id).orElseThrow(() -> ApiException.notFound("That listing was not found."));
        if (req.getCropName() != null && !req.getCropName().isBlank()) l.setCropName(req.getCropName().trim());
        if (req.getQuantityKg() != null) {
            if (req.getQuantityKg() < 0) throw ApiException.badRequest("Quantity cannot be negative.");
            l.setQuantityKg(req.getQuantityKg());
        }
        if (req.getPricePerKg() != null) {
            if (req.getPricePerKg() <= 0) throw ApiException.badRequest("Price must be greater than zero.");
            l.setExpectedPricePerKg(req.getPricePerKg());
        }
        if (req.getLocation() != null) l.setLocation(blankToNull(req.getLocation()));
        if (req.getDescription() != null) l.setDescription(blankToNull(req.getDescription()));
        if (req.getStatus() != null && Set.of("AVAILABLE", "SOLD_OUT", "HIDDEN").contains(req.getStatus().toUpperCase())) {
            l.setStatus(req.getStatus().toUpperCase());
        }
        return listingService.toAdminView(listingRepository.save(l));
    }

    @PostMapping(value = "/listings/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminListingView replaceListingImage(@PathVariable Long id,
                                                @RequestParam("image") MultipartFile image) throws IOException {
        ProduceListing l = listingRepository.findById(id).orElseThrow(() -> ApiException.notFound("That listing was not found."));
        String old = l.getImageUrl();
        l.setImageUrl(imageStorage.saveImage(image, "listings"));
        imageStorage.deleteQuietly(old);
        return listingService.toAdminView(listingRepository.save(l));
    }

    /** Hides the listing from buyers (order history keeps pointing at it). */
    @DeleteMapping("/listings/{id}")
    public Map<String, Object> hideListing(@PathVariable Long id) {
        ProduceListing l = listingRepository.findById(id).orElseThrow(() -> ApiException.notFound("That listing was not found."));
        l.setStatus("HIDDEN");
        listingRepository.save(l);
        return Map.of("hidden", true);
    }

    // ------------------------------------------------------------------ orders

    @GetMapping("/orders")
    public List<AdminOrderView> orders() {
        return orderViews.adminOrders(orderRepository.findAllByOrderByCreatedAtDesc());
    }

    // ------------------------------------------------------------- crop images

    @GetMapping("/crop-images")
    public List<CropImage> cropImages() {
        List<CropImage> all = new ArrayList<>(cropImageRepository.findAll());
        all.sort(Comparator.comparing(c -> c.getCropName() == null ? "" : c.getCropName().toLowerCase()));
        return all;
    }

    @PostMapping(value = "/crop-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CropImage addCropImage(@RequestParam String cropName,
                                  @RequestParam(required = false) String imageUrl,
                                  @RequestParam(required = false) String sourceCredit,
                                  @RequestParam(value = "image", required = false) MultipartFile image) throws IOException {
        if (cropName == null || cropName.isBlank()) throw ApiException.badRequest("Please enter the crop name.");
        CropImage c = new CropImage();
        c.setCropName(cropName.trim());
        c.setSourceCredit(blankToNull(sourceCredit));
        if (image != null && !image.isEmpty()) {
            c.setImageUrl(imageStorage.saveImage(image, "crops"));
        } else if (imageUrl != null && !imageUrl.isBlank()) {
            c.setImageUrl(imageUrl.trim());
        } else {
            throw ApiException.badRequest("Please upload an image or give an image link.");
        }
        return cropImageRepository.save(c);
    }

    @PostMapping(value = "/crop-images/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CropImage updateCropImage(@PathVariable Long id,
                                     @RequestParam String cropName,
                                     @RequestParam(required = false) String imageUrl,
                                     @RequestParam(required = false) String sourceCredit,
                                     @RequestParam(value = "image", required = false) MultipartFile image) throws IOException {
        CropImage c = cropImageRepository.findById(id).orElseThrow(() -> ApiException.notFound("That crop image was not found."));
        if (cropName == null || cropName.isBlank()) throw ApiException.badRequest("Please enter the crop name.");
        c.setCropName(cropName.trim());
        c.setSourceCredit(blankToNull(sourceCredit));
        if (image != null && !image.isEmpty()) {
            String old = c.getImageUrl();
            c.setImageUrl(imageStorage.saveImage(image, "crops"));
            imageStorage.deleteQuietly(old);
        } else if (imageUrl != null && !imageUrl.isBlank()) {
            c.setImageUrl(imageUrl.trim());
        }
        return cropImageRepository.save(c);
    }

    @DeleteMapping("/crop-images/{id}")
    public Map<String, Object> deleteCropImage(@PathVariable Long id) {
        CropImage c = cropImageRepository.findById(id).orElseThrow(() -> ApiException.notFound("That crop image was not found."));
        imageStorage.deleteQuietly(c.getImageUrl());
        cropImageRepository.delete(c);
        return Map.of("deleted", true);
    }

    // ------------------------------------------------------------- crop guides

    @PutMapping("/crop-guides/{id}")
    public CropGuide updateGuide(@PathVariable Long id, @RequestBody CropGuide edit) {
        CropGuide g = cropGuideRepository.findById(id).orElseThrow(() -> ApiException.notFound("That crop guide was not found."));
        edit.setId(g.getId());
        return cropGuideRepository.save(edit);
    }

    @DeleteMapping("/crop-guides/{id}")
    public Map<String, Object> deleteGuide(@PathVariable Long id) {
        try {
            cropGuideRepository.deleteById(id);
            cropGuideRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflict("This crop guide is being used by a learner's growth log and cannot be deleted.");
        }
        return Map.of("deleted", true);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}

package com.agri.backend.controller;

import com.agri.backend.dto.ApiDtos.SellerDashboard;
import com.agri.backend.dto.ApiDtos.SellerListingView;
import com.agri.backend.dto.ApiDtos.SellerOrderView;
import com.agri.backend.exception.ApiException;
import com.agri.backend.model.Order;
import com.agri.backend.model.ProduceListing;
import com.agri.backend.model.User;
import com.agri.backend.repository.OrderRepository;
import com.agri.backend.repository.ProduceListingRepository;
import com.agri.backend.security.AuthInterceptor;
import com.agri.backend.service.ImageStorageService;
import com.agri.backend.service.ListingService;
import com.agri.backend.service.OrderViewService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * Seller workspace. Everything here is scoped to the signed-in seller: a seller can only see and change their own
 * products and orders, and sees the customer details of people who bought from THEM (and nobody else's).
 * Product edits are written straight to the same table buyers read from, so changes show up on the buyer side at once.
 * (Updates use POST rather than PUT because browsers send multipart image uploads most reliably with POST.)
 */
@RestController
@RequestMapping("/api/seller")
public class SellerController {

    private final ProduceListingRepository listingRepository;
    private final OrderRepository orderRepository;
    private final ListingService listingService;
    private final OrderViewService orderViews;
    private final ImageStorageService imageStorage;

    public SellerController(ProduceListingRepository listingRepository, OrderRepository orderRepository,
                            ListingService listingService, OrderViewService orderViews,
                            ImageStorageService imageStorage) {
        this.listingRepository = listingRepository;
        this.orderRepository = orderRepository;
        this.listingService = listingService;
        this.orderViews = orderViews;
        this.imageStorage = imageStorage;
    }

    // --------------------------------------------------------------- dashboard

    @GetMapping("/dashboard")
    public SellerDashboard dashboard(HttpServletRequest http) {
        User seller = AuthInterceptor.currentUser(http);
        List<Order> orders = orderRepository.findBySellerUserIdOrderByCreatedAtDesc(seller.getId());
        int active = (int) listingRepository.findBySellerUserIdOrderByCreatedAtDesc(seller.getId()).stream()
                .filter(l -> "AVAILABLE".equals(l.getStatus())).count();
        return orderViews.sellerDashboard(seller, orders, active);
    }

    // ------------------------------------------------------------------ orders

    @GetMapping("/orders")
    public List<SellerOrderView> orders(HttpServletRequest http) {
        User seller = AuthInterceptor.currentUser(http);
        return orderViews.sellerOrders(orderRepository.findBySellerUserIdOrderByCreatedAtDesc(seller.getId()));
    }

    /** Move a paid order forward: PAID -> SHIPPED -> DELIVERED. */
    @PutMapping("/orders/{id}/status")
    public SellerOrderView updateOrderStatus(@PathVariable Long id, @RequestParam String status, HttpServletRequest http) {
        User seller = AuthInterceptor.currentUser(http);
        Order order = orderRepository.findById(id).orElseThrow();
        if (!seller.getId().equals(order.getSellerUserId())) throw ApiException.forbidden("That is not your order.");

        Order.Status next;
        try {
            next = Order.Status.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Unknown order status.");
        }
        boolean ok = (order.getStatus() == Order.Status.PAID && next == Order.Status.SHIPPED)
                || (order.getStatus() == Order.Status.SHIPPED && next == Order.Status.DELIVERED);
        if (!ok) throw ApiException.conflict("An order that is " + order.getStatus() + " cannot be moved to " + next + ".");
        order.setStatus(next);
        orderRepository.save(order);
        return orderViews.sellerOrders(List.of(order)).get(0);
    }

    // ---------------------------------------------------------------- listings

    @GetMapping("/listings")
    public List<SellerListingView> myListings(HttpServletRequest http) {
        User seller = AuthInterceptor.currentUser(http);
        return listingRepository.findBySellerUserIdOrderByCreatedAtDesc(seller.getId()).stream()
                .map(listingService::toSellerView).toList();
    }

    @PostMapping(value = "/listings", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SellerListingView createListing(
            @RequestParam String cropName,
            @RequestParam Double quantityKg,
            @RequestParam Double pricePerKg,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String availableFrom,
            @RequestParam(value = "image", required = false) MultipartFile image,
            HttpServletRequest http) throws IOException {

        User seller = AuthInterceptor.currentUser(http);
        validate(cropName, quantityKg, pricePerKg);

        ProduceListing l = new ProduceListing();
        l.setSellerUserId(seller.getId());
        l.setCropName(cropName.trim());
        l.setQuantityKg(quantityKg);
        l.setExpectedPricePerKg(pricePerKg);
        l.setLocation(blankToDefault(location, seller.getCity()));
        l.setDescription(blankToNull(description));
        l.setAvailableFrom(parseDate(availableFrom));
        l.setStatus("AVAILABLE");
        l.setCreatedAt(LocalDateTime.now());
        if (image != null && !image.isEmpty()) l.setImageUrl(imageStorage.saveImage(image, "listings"));
        return listingService.toSellerView(listingRepository.save(l));
    }

    @PostMapping(value = "/listings/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SellerListingView updateListing(
            @PathVariable Long id,
            @RequestParam String cropName,
            @RequestParam Double quantityKg,
            @RequestParam Double pricePerKg,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String availableFrom,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean removeImage,
            @RequestParam(value = "image", required = false) MultipartFile image,
            HttpServletRequest http) throws IOException {

        User seller = AuthInterceptor.currentUser(http);
        ProduceListing l = owned(id, seller);
        validate(cropName, quantityKg, pricePerKg);

        l.setCropName(cropName.trim());
        l.setQuantityKg(quantityKg);
        l.setExpectedPricePerKg(pricePerKg);
        l.setLocation(blankToDefault(location, l.getLocation()));
        l.setDescription(blankToNull(description));
        l.setAvailableFrom(parseDate(availableFrom));

        if (image != null && !image.isEmpty()) {
            String old = l.getImageUrl();
            l.setImageUrl(imageStorage.saveImage(image, "listings"));
            imageStorage.deleteQuietly(old);
        } else if (Boolean.TRUE.equals(removeImage)) {
            imageStorage.deleteQuietly(l.getImageUrl());
            l.setImageUrl(null);
        }

        if ("HIDDEN".equalsIgnoreCase(status)) l.setStatus("HIDDEN");
        else if ("AVAILABLE".equalsIgnoreCase(status) || "SOLD_OUT".equals(l.getStatus())) l.setStatus("AVAILABLE");
        if (quantityKg <= 0) l.setStatus("SOLD_OUT");
        return listingService.toSellerView(listingRepository.save(l));
    }

    /** Removes the listing, or just hides it if orders already refer to it (order history must stay intact). */
    @DeleteMapping("/listings/{id}")
    public Map<String, Object> deleteListing(@PathVariable Long id, HttpServletRequest http) {
        User seller = AuthInterceptor.currentUser(http);
        ProduceListing l = owned(id, seller);
        if (orderRepository.existsByListingId(id)) {
            l.setStatus("HIDDEN");
            listingRepository.save(l);
            return Map.of("deleted", false, "hidden", true);
        }
        imageStorage.deleteQuietly(l.getImageUrl());
        listingRepository.delete(l);
        return Map.of("deleted", true, "hidden", false);
    }

    // ----------------------------------------------------------------- helpers

    private ProduceListing owned(Long id, User seller) {
        ProduceListing l = listingRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("That listing was not found."));
        if (!seller.getId().equals(l.getSellerUserId())) throw ApiException.forbidden("That listing belongs to another seller.");
        return l;
    }

    private static void validate(String cropName, Double qty, Double price) {
        if (cropName == null || cropName.isBlank()) throw ApiException.badRequest("Please enter the crop name.");
        if (qty == null || qty < 0) throw ApiException.badRequest("Quantity cannot be negative.");
        if (price == null || price <= 0) throw ApiException.badRequest("Price per kg must be greater than zero.");
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.trim());
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("Please use a valid date.");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String blankToDefault(String s, String fallback) {
        return s == null || s.isBlank() ? fallback : s.trim();
    }
}

package com.agri.backend.service;

import com.agri.backend.dto.ApiDtos.*;
import com.agri.backend.model.Order;
import com.agri.backend.model.ProduceListing;
import com.agri.backend.model.User;
import com.agri.backend.repository.ProduceListingRepository;
import com.agri.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/** Builds the different order views and the seller's business statistics. */
@Service
public class OrderViewService {

    /** Orders that count as real business (money received or being fulfilled). */
    public static final Set<Order.Status> BUSINESS = EnumSet.of(Order.Status.PAID, Order.Status.SHIPPED, Order.Status.DELIVERED);

    private final UserRepository userRepository;
    private final ProduceListingRepository listingRepository;
    private final ListingService listingService;

    public OrderViewService(UserRepository userRepository, ProduceListingRepository listingRepository,
                            ListingService listingService) {
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
        this.listingService = listingService;
    }

    private Map<Long, User> usersById(Collection<Long> ids) {
        Set<Long> clean = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> map = new HashMap<>();
        userRepository.findAllById(clean).forEach(u -> map.put(u.getId(), u));
        return map;
    }

    private Map<Long, ProduceListing> listingsById(Collection<Long> ids) {
        Set<Long> clean = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, ProduceListing> map = new HashMap<>();
        listingRepository.findAllById(clean).forEach(l -> map.put(l.getId(), l));
        return map;
    }

    private String imageFor(Order o, Map<Long, ProduceListing> listings) {
        ProduceListing l = listings.get(o.getListingId());
        return l == null ? ListingService.DEFAULT_IMAGE : listingService.resolveImage(l);
    }

    public List<SellerOrderView> sellerOrders(List<Order> orders) {
        Map<Long, User> buyers = usersById(orders.stream().map(Order::getBuyerUserId).toList());
        Map<Long, ProduceListing> listings = listingsById(orders.stream().map(Order::getListingId).toList());
        return orders.stream().map(o -> {
            User b = buyers.get(o.getBuyerUserId());
            CustomerView customer = b == null ? null
                    : new CustomerView(b.getId(), b.getFullName(), b.getEmail(), b.getPhone(), b.getCity());
            return new SellerOrderView(o.getId(), o.getListingId(), o.getCropName(), imageFor(o, listings),
                    o.getQuantityKg(), o.getPricePerKg(), o.getTotalAmountRupees(), o.getStatus().name(),
                    o.getCreatedAt(), o.getPaidAt(), customer);
        }).toList();
    }

    /** Buyers see only the seller's public shop name. */
    public List<BuyerOrderView> buyerOrders(List<Order> orders) {
        Map<Long, User> sellers = usersById(orders.stream().map(Order::getSellerUserId).toList());
        Map<Long, ProduceListing> listings = listingsById(orders.stream().map(Order::getListingId).toList());
        return orders.stream().map(o -> new BuyerOrderView(o.getId(), o.getListingId(), o.getCropName(),
                imageFor(o, listings), o.getQuantityKg(), o.getPricePerKg(), o.getTotalAmountRupees(),
                o.getStatus().name(), o.getCreatedAt(),
                ListingService.publicSellerName(sellers.get(o.getSellerUserId())))).toList();
    }

    public List<AdminOrderView> adminOrders(List<Order> orders) {
        Set<Long> ids = new HashSet<>();
        orders.forEach(o -> { ids.add(o.getBuyerUserId()); ids.add(o.getSellerUserId()); });
        Map<Long, User> users = usersById(ids);
        return orders.stream().map(o -> {
            User b = users.get(o.getBuyerUserId());
            User s = users.get(o.getSellerUserId());
            return new AdminOrderView(o.getId(), o.getCropName(), o.getQuantityKg(), o.getTotalAmountRupees(),
                    o.getStatus().name(), o.getCreatedAt(),
                    b == null ? null : b.getFullName(), b == null ? null : b.getEmail(),
                    s == null ? null : ListingService.publicSellerName(s), s == null ? null : s.getEmail());
        }).toList();
    }

    public SellerDashboard sellerDashboard(User seller, List<Order> allOrders, int activeListings) {
        List<Order> business = allOrders.stream().filter(o -> BUSINESS.contains(o.getStatus())).toList();

        double net = business.stream().mapToDouble(o -> nz(o.getTotalAmountRupees())).sum();
        double pending = allOrders.stream().filter(o -> o.getStatus() == Order.Status.PENDING_PAYMENT)
                .mapToDouble(o -> nz(o.getTotalAmountRupees())).sum();
        double kg = business.stream().mapToDouble(o -> nz(o.getQuantityKg())).sum();
        long distinctBuyers = business.stream().map(Order::getBuyerUserId).filter(Objects::nonNull).distinct().count();

        SellerStats stats = new SellerStats(net, pending, allOrders.size(), business.size(), kg,
                (int) distinctBuyers, activeListings);

        // customers (only people who actually bought from this seller)
        Map<Long, List<Order>> byBuyer = business.stream().filter(o -> o.getBuyerUserId() != null)
                .collect(Collectors.groupingBy(Order::getBuyerUserId));
        Map<Long, User> buyers = usersById(byBuyer.keySet());
        List<CustomerSummary> customers = byBuyer.entrySet().stream().map(e -> {
            User b = buyers.get(e.getKey());
            List<Order> list = e.getValue();
            double spent = list.stream().mapToDouble(o -> nz(o.getTotalAmountRupees())).sum();
            var last = list.stream().map(Order::getCreatedAt).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
            return new CustomerSummary(e.getKey(), b == null ? "Unknown buyer" : b.getFullName(),
                    b == null ? null : b.getEmail(), b == null ? null : b.getPhone(), b == null ? null : b.getCity(),
                    list.size(), spent, last);
        }).sorted(Comparator.comparingDouble(CustomerSummary::totalSpent).reversed()).toList();

        // revenue by crop
        Map<String, List<Order>> byCrop = business.stream()
                .collect(Collectors.groupingBy(o -> o.getCropName() == null ? "Other" : o.getCropName()));
        List<CropSales> crops = byCrop.entrySet().stream().map(e -> new CropSales(e.getKey(),
                e.getValue().stream().mapToDouble(o -> nz(o.getQuantityKg())).sum(),
                e.getValue().stream().mapToDouble(o -> nz(o.getTotalAmountRupees())).sum()))
                .sorted(Comparator.comparingDouble(CropSales::revenue).reversed()).toList();

        List<SellerOrderView> recent = sellerOrders(allOrders.stream().limit(8).toList());
        return new SellerDashboard(stats, recent, customers, crops);
    }

    private static double nz(Double d) {
        return d == null ? 0.0 : d;
    }
}

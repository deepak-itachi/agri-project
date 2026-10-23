package com.agri.backend.config;

import com.agri.backend.model.CropGuide;
import com.agri.backend.model.CropImage;
import com.agri.backend.model.ProduceListing;
import com.agri.backend.model.User;
import com.agri.backend.repository.CropGuideRepository;
import com.agri.backend.repository.CropImageRepository;
import com.agri.backend.repository.ProduceListingRepository;
import com.agri.backend.repository.UserRepository;
import com.agri.backend.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Runs once at start-up and only fills gaps:
 *  - creates the first ADMIN account if none exists
 *  - makes sure the crop-image dataset has photos for common crops
 *  - (optional, SEED_DEMO=true) creates demo seller / buyer / learner accounts and a few sample products
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin@123";

    private final UserRepository userRepository;
    private final ProduceListingRepository listingRepository;
    private final CropImageRepository cropImageRepository;
    private final CropGuideRepository cropGuideRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Value("${app.admin.email:admin@agrisetu.in}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@123}")
    private String adminPassword;

    @Value("${app.seed-demo:true}")
    private boolean seedDemo;

    public DataSeeder(UserRepository userRepository, ProduceListingRepository listingRepository,
                      CropImageRepository cropImageRepository, CropGuideRepository cropGuideRepository,
                      JwtService jwtService) {
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
        this.cropImageRepository = cropImageRepository;
        this.cropGuideRepository = cropGuideRepository;
        this.jwtService = jwtService;
    }

    @Override
    public void run(String... args) {
        ensureAdmin();
        ensureCropImages();
        ensureCropGuides();
        if (seedDemo) seedDemoData();
        if (jwtService.isEphemeralSecret()) {
            log.warn("JWT_SECRET is not set: a random signing key is used, so everyone is signed out whenever the server restarts. Set JWT_SECRET for a stable key.");
        }
    }

    private void ensureAdmin() {
        String email = adminEmail.trim().toLowerCase();
        if (userRepository.findByEmailAndRole(email, User.Role.ADMIN).isPresent()) return;
        User admin = newUser(email, adminPassword, "Platform Admin", User.Role.ADMIN);
        userRepository.save(admin);
        log.info("Created admin account {}", email);
        if (DEFAULT_ADMIN_PASSWORD.equals(adminPassword)) {
            log.warn("The admin account is using the DEFAULT password. Set ADMIN_PASSWORD (and ADMIN_EMAIL) before putting this site online.");
        }
    }

    private void ensureCropImages() {
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("Tomato", "/images/crops/tomato.jpg");
        defaults.put("Paddy (Rice)", "/images/crops/paddy.jpg");
        defaults.put("Rose", "/images/crops/rose.jpg");
        defaults.put("Lotus", "/images/crops/lotus.jpg");
        defaults.put("Red Chilli", "/images/crops/chilli.jpg");
        defaults.put("Ginger", "/images/crops/ginger.jpg");
        defaults.put("Sweet Potato", "/images/crops/sweet-potato.jpg");
        defaults.put("Mango", "/images/crops/mango.jpg");
        defaults.put("Banana", "/images/crops/banana.jpg");
        defaults.put("Grapes", "/images/crops/grapes.jpg");
        defaults.put("Pineapple", "/images/crops/pineapple.jpg");
        defaults.put("Watermelon", "/images/crops/watermelon.jpg");
        defaults.put("Cucumber", "/images/crops/cucumber.jpg");
        defaults.put("Cauliflower", "/images/crops/cauliflower.jpg");
        defaults.put("Broccoli", "/images/crops/broccoli.jpg");
        defaults.put("Strawberry", "/images/crops/strawberry.jpg");
        defaults.put("Green Beans", "/images/crops/green-beans.jpg");
        defaults.forEach((name, url) -> {
            if (cropImageRepository.findFirstByCropNameIgnoreCase(name).isEmpty()) {
                CropImage c = new CropImage();
                c.setCropName(name);
                c.setImageUrl(url);
                c.setSourceCredit("Project sample photo");
                cropImageRepository.save(c);
            }
        });
    }

    private void seedDemoData() {
        User seller = userRepository.findByEmailAndRole("seller@demo.in", User.Role.SELLER).orElseGet(() -> {
            User u = newUser("seller@demo.in", "Demo@1234", "Muthu Kumar", User.Role.SELLER);
            u.setBusinessName("Thanjavur Green Farms");
            u.setCity("Thanjavur");
            u.setPhone("9876500001");
            u.setUpiId("thanjavurgreen@okaxis");
            return userRepository.save(u);
        });
        userRepository.findByEmailAndRole("buyer@demo.in", User.Role.BUYER).orElseGet(() -> {
            User u = newUser("buyer@demo.in", "Demo@1234", "Anitha Traders", User.Role.BUYER);
            u.setCity("Chennai");
            u.setPhone("9876500002");
            return userRepository.save(u);
        });
        userRepository.findByEmailAndRole("learner@demo.in", User.Role.FARMER).orElseGet(() -> {
            User u = newUser("learner@demo.in", "Demo@1234", "Hari Learner", User.Role.FARMER);
            u.setCity("Coimbatore");
            return userRepository.save(u);
        });

        if (listingRepository.count() == 0) {
            addListing(seller, "Paddy (Rice)", 500, 38, "Fresh harvest, sun-dried, ready for milling.");
            addListing(seller, "Tomato", 120, 28, "Firm red tomatoes picked this morning.");
            addListing(seller, "Banana", 200, 40, "Naturally ripened, no carbide.");
            addListing(seller, "Mango", 80, 90, "Sweet, fibre-free mangoes from our own orchard.");
            addListing(seller, "Red Chilli", 60, 70, "Bright red, sun-dried chillies with good heat.");
            addListing(seller, "Ginger", 90, 110, "Fresh, thick rhizomes.");
            addListing(seller, "Sweet Potato", 150, 35, "Cleaned and graded.");
            addListing(seller, "Rose", 40, 180, "Fragrant red roses for garlands and events.");
            log.info("Seeded demo accounts (seller@demo.in / buyer@demo.in / learner@demo.in, password Demo@1234) and sample products. Set SEED_DEMO=false to disable.");
        }
    }

    private void addListing(User seller, String crop, double qty, double price, String description) {
        ProduceListing l = new ProduceListing();
        l.setSellerUserId(seller.getId());
        l.setCropName(crop);
        l.setQuantityKg(qty);
        l.setExpectedPricePerKg(price);
        l.setLocation(seller.getCity());
        l.setDescription(description);
        l.setAvailableFrom(LocalDate.now());
        l.setStatus("AVAILABLE");
        l.setCreatedAt(LocalDateTime.now());
        listingRepository.save(l);
    }

    private User newUser(String email, String password, String fullName, User.Role role) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(password));
        u.setFullName(fullName);
        u.setRole(role);
        u.setActive(true);
        u.setCreatedAt(LocalDateTime.now());
        u.setLoginCount(0);
        return u;
    }

    // ---------------------------------------------------------------- crop guides
    /** Adds starter crop guides that are missing (matched by name), so a fresh cloud database works without running schema.sql. */
    private void ensureCropGuides() {
        Set<String> have = new HashSet<>();
        cropGuideRepository.findAll().forEach(g -> { if (g.getCropName() != null) have.add(g.getCropName().toLowerCase()); });
        String urban = "TERRACE,BACKYARD,ROOFTOP_CONTAINER";
        String field = "OPEN_FIELD,BACKYARD";
        Object[][] rows = {
            // name, land types, scale, days, cost, yield kg, prep, sowing, water, fertiliser, pests, harvest
            {"Tomato", urban, "SMALL_SCALE_URBAN", 90, 1500.0, 40.0, "Fill grow bags with a 3:1:1 mix of soil, compost and sand.", "Sow seed 0.5 cm deep; transplant seedlings after about 3 weeks.", "Water every 2 days, more during flowering.", "Compost at planting; NPK 19:19:19 every 15 days after transplanting.", "Watch for aphids and fruit borer; spray neem oil weekly.", "Harvest when fruit is firm and fully coloured, around day 75 to 90."},
            {"Cucumber", urban, "SMALL_SCALE_URBAN", 60, 1200.0, 35.0, "Use deep containers with well-drained compost-rich soil and a trellis.", "Sow 2 seeds per pit, 1 cm deep, and keep the stronger seedling.", "Light water daily; never let the soil dry out in flowering.", "Compost at sowing; liquid manure every 10 days.", "Look out for red pumpkin beetle and powdery mildew.", "Pick tender fruits every 2 days from about day 40."},
            {"Green Beans", urban, "SMALL_SCALE_URBAN", 65, 900.0, 25.0, "Loosen the soil and add compost; set up a bamboo support.", "Sow seeds 3 cm deep, 15 cm apart.", "Water every 2 to 3 days; avoid waterlogging.", "Light compost; beans fix their own nitrogen.", "Aphids and pod borer; neem spray when seen.", "Harvest pods while young and tender, every 3 days."},
            {"Red Chilli", urban, "SMALL_SCALE_URBAN", 150, 1600.0, 20.0, "Grow bags with a soil, compost and coco peat mix.", "Raise seedlings in a tray, transplant at 30 days.", "Water every 2 days; keep the soil moist, not wet.", "Compost at planting; potash when fruits set.", "Thrips and mites cause leaf curl; spray neem oil.", "Pick when fully red; dry in the sun for storage."},
            {"Strawberry", urban, "SMALL_SCALE_URBAN", 120, 2500.0, 12.0, "Shallow containers with sandy, compost-rich soil in light shade.", "Plant runners with the crown just above soil.", "Light, frequent watering; keep fruit off wet soil.", "Balanced fertiliser monthly.", "Birds and grey mould; use a net and keep leaves dry.", "Pick fully red berries every 2 days."},
            {"Cauliflower", field, "SMALL_SCALE", 100, 18000.0, 6000.0, "Plough finely and mix in farmyard manure.", "Transplant 4 week old seedlings at 45 x 45 cm.", "Irrigate every 7 days; steady moisture avoids buttoning.", "FYM at land preparation; split doses of nitrogen.", "Diamondback moth and black rot; use traps and clean seed.", "Cut when the curd is compact and white."},
            {"Broccoli", field, "SMALL_SCALE", 90, 20000.0, 4000.0, "Prepare a well-drained bed rich in compost.", "Transplant 4 week old seedlings at 45 x 45 cm.", "Irrigate every 5 to 7 days.", "FYM at planting; top-dress nitrogen at 3 weeks.", "Aphids and caterpillars; neem and hand picking.", "Cut the main head before the flowers open."},
            {"Ginger", field, "SMALL_SCALE", 240, 90000.0, 6000.0, "Raised beds in light shade with compost and good drainage.", "Plant 30 g rhizome bits 5 cm deep, 25 cm apart.", "Irrigate every 7 to 10 days; mulch to hold moisture.", "FYM and neem cake at planting; top-dress at 60 and 120 days.", "Rhizome rot in wet soil; keep drainage clear.", "Harvest when leaves yellow and dry, around 8 months."},
            {"Watermelon", field, "SMALL_SCALE", 85, 25000.0, 10000.0, "Prepare pits or ridges with sandy loam and manure.", "Sow 2 seeds per pit at 2 m spacing.", "Irrigate every 5 days; reduce at ripening for sweetness.", "FYM at planting; potash at fruit set.", "Fruit fly and downy mildew; use traps and resistant seed.", "Ripe when the tendril near the stalk dries and the base turns yellow."},
            {"Sweet Potato", "OPEN_FIELD", "MEDIUM_SCALE", 110, 20000.0, 8000.0, "Make ridges 60 cm apart in light, sandy loam.", "Plant 30 cm vine cuttings on the ridges.", "Irrigate every 7 days until established.", "FYM at preparation; potash helps tuber size.", "Weevils; earth up ridges to cover tubers.", "Dig when leaves yellow, about 3.5 months."},
            {"Banana", "OPEN_FIELD", "MEDIUM_SCALE", 300, 120000.0, 25000.0, "Dig 45 cm pits and fill with manure and topsoil.", "Plant tissue-culture plants at 1.8 x 1.8 m.", "Drip or basin irrigation every 3 to 4 days.", "Split doses of N and K through the crop.", "Rhizome weevil and sigatoka leaf spot.", "Harvest bunches 3 months after flowering."},
            {"Pineapple", "OPEN_FIELD", "MEDIUM_SCALE", 480, 90000.0, 20000.0, "Make raised beds with good drainage.", "Plant suckers or slips at 60 x 30 cm.", "Irrigate every 10 days in dry spells.", "Split doses of N and K every 2 months.", "Mealybug wilt; use clean planting material.", "Pick when the fruit turns yellow at the base."},
            {"Paddy (Rice)", "OPEN_FIELD", "LARGE_SCALE", 120, 25000.0, 2200.0, "Plough 2 to 3 times, level and prepare nursery beds.", "Transplant 21 day old seedlings at 20 x 15 cm.", "Keep 2 to 5 cm standing water through the vegetative stage.", "Urea in 3 split doses; DAP basal at transplanting.", "Stem borer and blast disease.", "Harvest when 80 percent of the grains turn golden."},
            {"Mango", "OPEN_FIELD", "LARGE_SCALE", 1460, 60000.0, 4000.0, "Dig 1 m pits, fill with manure and topsoil.", "Plant grafts at 10 x 10 m before the monsoon.", "Water young trees weekly; stop irrigation before flowering.", "FYM each year; NPK in two doses.", "Hoppers and powdery mildew at flowering.", "Harvest mature fruit from year 4 or 5."},
            {"Grapes", "OPEN_FIELD", "LARGE_SCALE", 540, 250000.0, 10000.0, "Set up a trellis or pandal; dig pits with manure.", "Plant rooted cuttings at 3 x 2 m.", "Drip irrigation; cut back at fruit ripening.", "Split nutrition by growth stage.", "Downy mildew and thrips.", "Harvest when berries reach full sweetness."},
        };
        int added = 0;
        for (Object[] r : rows) {
            if (have.contains(((String) r[0]).toLowerCase())) continue;
            CropGuide g = new CropGuide();
            g.setCropName((String) r[0]);
            g.setSuitableLandTypes((String) r[1]);
            g.setSuitableScale((String) r[2]);
            g.setDurationDays((Integer) r[3]);
            g.setEstimatedCostPerAcreOrUnit((Double) r[4]);
            g.setEstimatedYieldPerAcreOrUnit((Double) r[5]);
            g.setLandPreparationSteps((String) r[6]);
            g.setSowingInstructions((String) r[7]);
            g.setWateringSchedule((String) r[8]);
            g.setFertilizerPlan((String) r[9]);
            g.setPestWatch((String) r[10]);
            g.setHarvestGuide((String) r[11]);
            cropGuideRepository.save(g);
            added++;
        }
        if (added > 0) log.info("Added {} starter crop guides. Costs and yields are indicative; check TNAU guides for your district.", added);
    }
}

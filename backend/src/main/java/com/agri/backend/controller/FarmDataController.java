package com.agri.backend.controller;

import com.agri.backend.exception.ApiException;
import com.agri.backend.model.LandPhoto;
import com.agri.backend.model.SoilReport;
import com.agri.backend.model.User;
import com.agri.backend.model.WaterResource;
import com.agri.backend.repository.LandPhotoRepository;
import com.agri.backend.repository.SoilReportRepository;
import com.agri.backend.repository.WaterResourceRepository;
import com.agri.backend.security.AuthInterceptor;
import com.agri.backend.service.ImageStorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Extra inputs that make crop recommendations more accurate than satellite soil data alone:
 *   1. the lab soil-test report (PDF or photo) plus its key values
 *   2. water resources (source, availability, irrigation method)
 *   3. a photo of the actual land
 * These are private to the learner (and the admin): the upload folders are not served publicly.
 */
@RestController
@RequestMapping("/api/farm-data")
public class FarmDataController {

    private static final Set<String> REPORT_EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg", "webp");

    private final SoilReportRepository soilReportRepository;
    private final WaterResourceRepository waterResourceRepository;
    private final LandPhotoRepository landPhotoRepository;
    private final ImageStorageService imageStorage;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public FarmDataController(SoilReportRepository soilReportRepository,
                              WaterResourceRepository waterResourceRepository,
                              LandPhotoRepository landPhotoRepository,
                              ImageStorageService imageStorage) {
        this.soilReportRepository = soilReportRepository;
        this.waterResourceRepository = waterResourceRepository;
        this.landPhotoRepository = landPhotoRepository;
        this.imageStorage = imageStorage;
    }

    /** Stores a soil report under a random name; only pdf/png/jpg/webp are accepted. */
    private String saveReport(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw ApiException.badRequest("Please choose the soil test report file.");
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        String ext = dot < 0 ? "" : original.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!REPORT_EXTENSIONS.contains(ext)) throw ApiException.badRequest("Please upload a PDF, JPG, PNG or WEBP file.");
        Path dir = Paths.get(uploadDir, "soil-reports").toAbsolutePath().normalize();
        Files.createDirectories(dir);
        Path target = dir.resolve(UUID.randomUUID() + "." + ext);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return target.toString();
    }

    /** Learners always act on their own profile; admins may act on any farmerId. */
    private Long effectiveFarmerId(Long requested, HttpServletRequest http) {
        User user = AuthInterceptor.currentUser(http);
        if (user.getRole() == User.Role.FARMER) {
            if (user.getFarmerId() == null) throw ApiException.badRequest("Please complete your farm profile first.");
            return user.getFarmerId();
        }
        if (requested == null) throw ApiException.badRequest("farmerId is required.");
        return requested;
    }

    // ---- 1. Soil test report ----

    @PostMapping(value = "/soil-report", consumes = "multipart/form-data")
    public SoilReport uploadSoilReport(
            @RequestParam(required = false) Long farmerId,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) Double phLevel,
            @RequestParam(required = false) Double nitrogenKgPerHa,
            @RequestParam(required = false) Double phosphorusKgPerHa,
            @RequestParam(required = false) Double potassiumKgPerHa,
            @RequestParam(required = false) Double organicCarbonPercent,
            @RequestParam(required = false) String soilTexture,
            @RequestParam(required = false) String labName,
            HttpServletRequest http) throws IOException {

        SoilReport report = new SoilReport();
        report.setFarmerId(effectiveFarmerId(farmerId, http));
        report.setFileName(file.getOriginalFilename());
        report.setContentType(file.getContentType());
        report.setStoredPath(saveReport(file));
        report.setPhLevel(phLevel);
        report.setNitrogenKgPerHa(nitrogenKgPerHa);
        report.setPhosphorusKgPerHa(phosphorusKgPerHa);
        report.setPotassiumKgPerHa(potassiumKgPerHa);
        report.setOrganicCarbonPercent(organicCarbonPercent);
        report.setSoilTexture(soilTexture);
        report.setLabName(labName);
        report.setUploadedAt(LocalDateTime.now());
        return soilReportRepository.save(report);
    }

    @GetMapping("/soil-report/farmer/{farmerId}")
    public List<SoilReport> getSoilReports(@PathVariable Long farmerId) {
        return soilReportRepository.findByFarmerIdOrderByUploadedAtDesc(farmerId);
    }

    // ---- 2. Water resources ----

    @PostMapping("/water-resource")
    public WaterResource saveWaterResource(@RequestBody WaterResource resource, HttpServletRequest http) {
        resource.setId(null); // always create a new record; never overwrite someone else's by sending an id
        resource.setFarmerId(effectiveFarmerId(resource.getFarmerId(), http));
        return waterResourceRepository.save(resource);
    }

    @GetMapping("/water-resource/farmer/{farmerId}")
    public List<WaterResource> getWaterResources(@PathVariable Long farmerId) {
        return waterResourceRepository.findByFarmerId(farmerId);
    }

    // ---- 3. Land photo ----

    @PostMapping(value = "/land-photo", consumes = "multipart/form-data")
    public LandPhoto uploadLandPhoto(
            @RequestParam(required = false) Long farmerId,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String caption,
            HttpServletRequest http) throws IOException {

        LandPhoto photo = new LandPhoto();
        photo.setFarmerId(effectiveFarmerId(farmerId, http));
        photo.setFileName(file.getOriginalFilename());
        photo.setContentType(file.getContentType());
        photo.setStoredPath(imageStorage.saveImage(file, "land-photos"));
        photo.setCaption(caption);
        photo.setUploadedAt(LocalDateTime.now());
        return landPhotoRepository.save(photo);
    }

    @GetMapping("/land-photo/farmer/{farmerId}")
    public List<LandPhoto> getLandPhotos(@PathVariable Long farmerId) {
        return landPhotoRepository.findByFarmerIdOrderByUploadedAtDesc(farmerId);
    }
}

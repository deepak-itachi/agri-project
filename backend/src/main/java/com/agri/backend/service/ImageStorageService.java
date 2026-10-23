package com.agri.backend.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/**
 * Stores uploaded product / crop photos on disk and returns a public URL path such as /uploads/listings/<uuid>.jpg
 * Safety: only real image files are accepted (content-type AND magic bytes are checked), the stored file name is a
 * random UUID (the client-supplied name is never used), and nothing outside the upload folder can be touched.
 * Only the "listings" and "crops" sub-folders are served publicly (see WebConfig); private uploads such as soil
 * reports and land photos live in other folders and are not exposed.
 */
@Service
public class ImageStorageService {

    private static final long MAX_BYTES = 6L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg", "image/png", "png", "image/webp", "webp", "image/gif", "gif");

    private final Path root;

    public ImageStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(root.resolve("listings"));
        Files.createDirectories(root.resolve("crops"));
    }

    public Path getRoot() {
        return root;
    }

    public String saveImage(MultipartFile file, String subfolder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please choose an image file.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("That image is too large. Please upload one under 6 MB.");
        }
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        String ext = EXTENSIONS.get(type);
        if (ext == null) {
            throw new IllegalArgumentException("Only JPG, PNG, WEBP or GIF images are allowed.");
        }
        byte[] head = new byte[12];
        try (InputStream in = file.getInputStream()) {
            int read = in.readNBytes(head, 0, head.length);
            if (read < 4 || !looksLikeImage(head)) {
                throw new IllegalArgumentException("That file does not look like a valid image.");
            }
        }
        Path folder = root.resolve(subfolder).normalize();
        if (!folder.startsWith(root)) throw new IllegalArgumentException("Invalid upload location.");
        Files.createDirectories(folder);
        String name = UUID.randomUUID() + "." + ext;
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, folder.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        }
        return "/uploads/" + subfolder + "/" + name;
    }

    /** Removes a previously uploaded file. Ignores URLs that are not ours (e.g. bundled /images/... files). */
    public void deleteQuietly(String url) {
        try {
            if (url == null || !url.startsWith("/uploads/")) return;
            Path p = root.resolve(url.substring("/uploads/".length())).normalize();
            if (p.startsWith(root)) Files.deleteIfExists(p);
        } catch (Exception ignored) {
            // best effort only
        }
    }

    static boolean looksLikeImage(byte[] h) {
        boolean jpeg = (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
        boolean png = (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G';
        boolean gif = h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8';
        boolean webp = h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
        return jpeg || png || gif || webp;
    }
}

package tm.delje.shop.service;

import jakarta.annotation.PostConstruct;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.UUID;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tm.delje.shop.config.AppProperties;

/**
 * Converts uploaded pictures to WebP and stores them in uploads/products.
 * The database keeps public paths like /uploads/products/&lt;file&gt;.webp.
 */
@Service
public class ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageService.class);

    private static final String PUBLIC_PREFIX = "/uploads/products/";
    private static final int THUMB_SIZE = 400;
    private static final int FULL_MAX = 1600;
    private static final int BANNER_MAX_WIDTH = 1920;
    private static final int BANNER_MAX_HEIGHT = 700;

    private final Path uploadsRoot;
    private final Path productsDir;

    public ImageService(AppProperties properties) {
        this.uploadsRoot = Paths.get(properties.getUploadsDir()).toAbsolutePath().normalize();
        this.productsDir = uploadsRoot.resolve("products");
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(productsDir);
        ImageIO.scanForPlugins();
    }

    public Path getUploadsRoot() {
        return uploadsRoot;
    }

    /**
     * Product picture in two sizes from one uuid:
     * thumb 400x400 (cover, quality 80) and full up to 1600px (inside, no upscaling, quality 85).
     */
    public StoredImage saveProductImage(byte[] data) throws IOException {
        BufferedImage source = read(data);
        String id = UUID.randomUUID().toString();
        String thumbFile = id + "_thumb.webp";
        String fullFile = id + ".webp";

        try {
            BufferedImage thumb = Thumbnails.of(source)
                    .size(THUMB_SIZE, THUMB_SIZE)
                    .crop(Positions.CENTER)
                    .asBufferedImage();
            writeWebp(thumb, productsDir.resolve(thumbFile), 0.80f);
            writeWebp(fitInside(source, FULL_MAX, FULL_MAX), productsDir.resolve(fullFile), 0.85f);
        } catch (IOException | RuntimeException e) {
            deleteQuietly(productsDir.resolve(thumbFile));
            deleteQuietly(productsDir.resolve(fullFile));
            throw e;
        }
        return new StoredImage(PUBLIC_PREFIX + thumbFile, PUBLIC_PREFIX + fullFile);
    }

    /**
     * Banner picture: fits into 1920x700 without upscaling, quality 85.
     */
    public String saveBanner(byte[] data) throws IOException {
        BufferedImage source = read(data);
        String file = UUID.randomUUID() + "_banner.webp";
        try {
            writeWebp(fitInside(source, BANNER_MAX_WIDTH, BANNER_MAX_HEIGHT), productsDir.resolve(file), 0.85f);
        } catch (IOException | RuntimeException e) {
            deleteQuietly(productsDir.resolve(file));
            throw e;
        }
        return PUBLIC_PREFIX + file;
    }

    /**
     * Writes a picture as it is (used by the seed task for generated placeholders).
     */
    public String saveGenerated(BufferedImage image, String fileName) throws IOException {
        writeWebp(image, productsDir.resolve(fileName), 0.80f);
        return PUBLIC_PREFIX + fileName;
    }

    /**
     * Removes a file by its public path. A missing file is not an error.
     * On Windows a freshly written file can be locked for a moment, so a few retries are made.
     */
    public void deleteUpload(String publicPath) {
        if (publicPath == null || !publicPath.startsWith("/uploads/")) {
            return;
        }
        Path file = uploadsRoot.resolve(publicPath.substring("/uploads/".length())).normalize();
        if (!file.startsWith(uploadsRoot)) {
            log.warn("refusing to delete file outside uploads: {}", publicPath);
            return;
        }
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                Files.deleteIfExists(file);
                return;
            } catch (NoSuchFileException e) {
                return;
            } catch (IOException e) {
                if (attempt == 3) {
                    log.warn("could not delete upload {}", publicPath, e);
                    return;
                }
                try {
                    Thread.sleep(attempt * 150L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private BufferedImage read(byte[] data) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
        if (image == null) {
            throw new InvalidImageException();
        }
        return image;
    }

    private BufferedImage fitInside(BufferedImage source, int maxWidth, int maxHeight) throws IOException {
        if (source.getWidth() <= maxWidth && source.getHeight() <= maxHeight) {
            return source;
        }
        return Thumbnails.of(source).size(maxWidth, maxHeight).keepAspectRatio(true).asBufferedImage();
    }

    private void writeWebp(BufferedImage image, Path target, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType("image/webp");
        if (!writers.hasNext()) {
            throw new IOException("WebP writer is not available");
        }
        ImageWriter writer = writers.next();
        try (ImageOutputStream out = ImageIO.createImageOutputStream(target.toFile())) {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionType("Lossy");
            param.setCompressionQuality(quality);
            writer.setOutput(out);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    private void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("could not clean up {}", file, e);
        }
    }

    public record StoredImage(String thumb, String full) {
    }

    public static class InvalidImageException extends IOException {
        public InvalidImageException() {
            super("file is not a supported image");
        }
    }
}

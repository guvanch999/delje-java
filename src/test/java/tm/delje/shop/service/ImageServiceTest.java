package tm.delje.shop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tm.delje.shop.config.AppProperties;

class ImageServiceTest {

    @TempDir
    Path tempDir;

    private ImageService imageService;

    @BeforeEach
    void setUp() throws IOException {
        AppProperties properties = new AppProperties();
        properties.setUploadsDir(tempDir.resolve("uploads").toString());
        imageService = new ImageService(properties);
        imageService.init();
    }

    @Test
    void productImageHasThumbAndFullSizes() throws IOException {
        ImageService.StoredImage stored = imageService.saveProductImage(png(3000, 2000));

        assertTrue(stored.thumb().matches("/uploads/products/[0-9a-f-]{36}_thumb\\.webp"));
        assertTrue(stored.full().matches("/uploads/products/[0-9a-f-]{36}\\.webp"));

        BufferedImage thumb = readUpload(stored.thumb());
        assertEquals(400, thumb.getWidth());
        assertEquals(400, thumb.getHeight());

        BufferedImage full = readUpload(stored.full());
        assertEquals(1600, full.getWidth());
        assertEquals(1067, full.getHeight());
    }

    @Test
    void smallImageIsNotEnlarged() throws IOException {
        ImageService.StoredImage stored = imageService.saveProductImage(png(300, 200));

        BufferedImage full = readUpload(stored.full());
        assertEquals(300, full.getWidth());
        assertEquals(200, full.getHeight());

        BufferedImage thumb = readUpload(stored.thumb());
        assertEquals(400, thumb.getWidth());
        assertEquals(400, thumb.getHeight());
    }

    @Test
    void bannerFitsInto1920x700() throws IOException {
        String banner = imageService.saveBanner(png(2400, 1256));

        assertTrue(banner.endsWith("_banner.webp"));
        BufferedImage image = readUpload(banner);
        assertEquals(1338, image.getWidth());
        assertEquals(700, image.getHeight());
    }

    @Test
    void notAnImageIsRejected() {
        assertThrows(ImageService.InvalidImageException.class,
                () -> imageService.saveProductImage("hello".getBytes()));
    }

    @Test
    void deleteRemovesFileAndIgnoresMissingOrForeignPaths() throws IOException {
        String banner = imageService.saveBanner(png(100, 50));
        Path file = tempDir.resolve("uploads").resolve(banner.substring("/uploads/".length()));
        assertTrue(Files.exists(file));

        imageService.deleteUpload(banner);
        assertFalse(Files.exists(file));

        imageService.deleteUpload(banner);
        imageService.deleteUpload("/uploads/../../secret.txt");
        imageService.deleteUpload(null);
    }

    private BufferedImage readUpload(String publicPath) throws IOException {
        Path file = tempDir.resolve("uploads").resolve(publicPath.substring("/uploads/".length()));
        BufferedImage image = ImageIO.read(file.toFile());
        if (image == null) {
            throw new IOException("cannot read " + file);
        }
        return image;
    }

    private static byte[] png(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(214, 64, 159));
        g.fillRect(0, 0, width, height);
        g.setColor(Color.WHITE);
        g.fillOval(width / 4, height / 4, width / 2, height / 2);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}

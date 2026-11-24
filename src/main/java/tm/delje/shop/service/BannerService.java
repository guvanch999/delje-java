package tm.delje.shop.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tm.delje.shop.dto.BannerForm;
import tm.delje.shop.entity.Banner;
import tm.delje.shop.repository.BannerRepository;

@Service
public class BannerService {

    private final BannerRepository bannerRepository;
    private final ImageService imageService;

    public BannerService(BannerRepository bannerRepository, ImageService imageService) {
        this.bannerRepository = bannerRepository;
        this.imageService = imageService;
    }

    public List<Banner> findAll() {
        return bannerRepository.findAllByOrderByIdAsc();
    }

    public Banner create(BannerForm form, MultipartFile file) throws IOException {
        String image = imageService.saveBanner(file.getBytes());
        Banner banner = new Banner();
        banner.setImage(image);
        banner.setTitle(emptyToNull(form.getTitle()));
        banner.setTitleTk(emptyToNull(form.getTitleTk()));
        banner.setLink(emptyToNull(form.getLink()));
        banner.setActive(true);
        try {
            return bannerRepository.save(banner);
        } catch (RuntimeException e) {
            imageService.deleteUpload(image);
            throw e;
        }
    }

    @Transactional
    public void toggle(Integer id) {
        bannerRepository.findById(id).ifPresent(banner -> banner.setActive(!banner.isActive()));
    }

    public void delete(Integer id) {
        Optional<Banner> banner = bannerRepository.findById(id);
        if (banner.isPresent()) {
            bannerRepository.delete(banner.get());
            imageService.deleteUpload(banner.get().getImage());
        }
    }

    private static String emptyToNull(String s) {
        return s == null || s.isEmpty() ? null : s;
    }
}

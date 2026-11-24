package tm.delje.shop.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import tm.delje.shop.entity.Product;

/**
 * Admin product form. Values are kept as entered so the form can be shown again with an error.
 */
public class ProductForm {

    private static final BigDecimal MAX_PRICE = new BigDecimal("9999999999.99");

    private String name = "";
    private String nameTk = "";
    private String description = "";
    private String descriptionTk = "";
    private String price = "";
    private String categoryId = "";
    private boolean isTop;
    private List<String> deleteImageIds = new ArrayList<>();

    public static ProductForm of(Product product) {
        ProductForm form = new ProductForm();
        form.name = product.getName();
        form.nameTk = nullToEmpty(product.getNameTk());
        form.description = nullToEmpty(product.getDescription());
        form.descriptionTk = nullToEmpty(product.getDescriptionTk());
        form.price = product.getPrice() != null ? product.getPrice().toPlainString() : "";
        form.categoryId = product.getCategoryId() != null ? product.getCategoryId().toString() : "";
        form.isTop = product.isTop();
        return form;
    }

    /**
     * @return error message for the form, or null when everything is fine
     */
    public String validate() {
        if (name == null || name.isBlank()) {
            return "Название не может быть пустым";
        }
        if (name.length() > 255 || (nameTk != null && nameTk.length() > 255)) {
            return "Название не должно быть длиннее 255 символов";
        }
        BigDecimal value = getPriceValue();
        if (value == null || value.signum() <= 0) {
            return "Цена должна быть числом больше нуля";
        }
        if (value.compareTo(MAX_PRICE) > 0) {
            return "Цена слишком большая";
        }
        return null;
    }

    public BigDecimal getPriceValue() {
        if (price == null || price.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(price.trim()).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Integer getCategoryIdValue() {
        if (categoryId == null || categoryId.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(categoryId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public List<Integer> getDeleteImageIdValues() {
        List<Integer> ids = new ArrayList<>();
        if (deleteImageIds == null) {
            return ids;
        }
        for (String id : deleteImageIds) {
            try {
                ids.add(Integer.valueOf(id.trim()));
            } catch (NumberFormatException | NullPointerException e) {
                // broken ids are skipped
            }
        }
        return ids;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameTk() {
        return nameTk;
    }

    public void setNameTk(String nameTk) {
        this.nameTk = nameTk;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescriptionTk() {
        return descriptionTk;
    }

    public void setDescriptionTk(String descriptionTk) {
        this.descriptionTk = descriptionTk;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public boolean getIsTop() {
        return isTop;
    }

    public void setIsTop(boolean isTop) {
        this.isTop = isTop;
    }

    public List<String> getDeleteImageIds() {
        return deleteImageIds;
    }

    public void setDeleteImageIds(List<String> deleteImageIds) {
        this.deleteImageIds = deleteImageIds;
    }
}

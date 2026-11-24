package tm.delje.shop.dto;

import tm.delje.shop.entity.Category;

public class CategoryForm {

    private String name = "";
    private String nameTk = "";

    public static CategoryForm of(Category category) {
        CategoryForm form = new CategoryForm();
        form.name = category.getName();
        form.nameTk = category.getNameTk() == null ? "" : category.getNameTk();
        return form;
    }

    public String validate() {
        if (name == null || name.isBlank()) {
            return "Название не может быть пустым";
        }
        if (name.length() > 255 || (nameTk != null && nameTk.length() > 255)) {
            return "Название не должно быть длиннее 255 символов";
        }
        return null;
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
}

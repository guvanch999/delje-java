package tm.delje.shop.dto;

public class BannerForm {

    private String title = "";
    private String titleTk = "";
    private String link = "";

    public String validate() {
        if (length(title) > 255 || length(titleTk) > 255 || length(link) > 255) {
            return "Заголовок и ссылка не должны быть длиннее 255 символов";
        }
        return null;
    }

    private static int length(String s) {
        return s == null ? 0 : s.length();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitleTk() {
        return titleTk;
    }

    public void setTitleTk(String titleTk) {
        this.titleTk = titleTk;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}

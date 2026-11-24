package tm.delje.shop.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Contact form of the public site. Messages are keys of messages*.properties.
 */
public class ContactForm {

    @NotBlank(message = "contact.error.name")
    @Size(max = 100, message = "contact.error.tooLong")
    private String name = "";

    @NotBlank(message = "contact.error.email")
    @Email(message = "contact.error.email")
    @Size(max = 200, message = "contact.error.tooLong")
    private String email = "";

    @NotBlank(message = "contact.error.message")
    @Size(max = 5000, message = "contact.error.tooLong")
    private String message = "";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

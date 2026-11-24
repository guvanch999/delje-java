package tm.delje.shop.service;

/**
 * Thrown when a requested record does not exist; rendered as the 404 page.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}

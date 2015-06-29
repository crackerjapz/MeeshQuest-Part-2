package cmsc420.exception;

/**
 * Thrown if city is already in the spatial map upon attempted insertion.
 */
public class CityAlreadyMappedException extends Throwable {
	private static final long serialVersionUID = -4096614031875292057L;

	public CityAlreadyMappedException() {
	}

	public CityAlreadyMappedException(String message) {
		super(message);
	}
}

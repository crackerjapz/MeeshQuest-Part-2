package cmsc420.exception;

public class RoadOutOfBoundsException extends Throwable {
	private static final long serialVersionUID = -68780714302494395L;

	public RoadOutOfBoundsException() {
	}

	public RoadOutOfBoundsException(String message) {
		super(message);
	}
}

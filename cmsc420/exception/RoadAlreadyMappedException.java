package cmsc420.exception;

public class RoadAlreadyMappedException extends Throwable {
	private static final long serialVersionUID = -687807114302494395L;

	public RoadAlreadyMappedException() {
	}

	public RoadAlreadyMappedException(String message) {
		super(message);
	}
}

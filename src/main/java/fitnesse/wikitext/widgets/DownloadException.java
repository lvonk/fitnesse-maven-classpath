package fitnesse.wikitext.widgets;

public class DownloadException extends Exception {

	private static final long serialVersionUID = 5290037392378102463L;

	public DownloadException() {
	}

	public DownloadException(String message) {
		super(message);
	}

	public DownloadException(Throwable cause) {
		super(cause);
	}

	public DownloadException(String message, Throwable cause) {
		super(message, cause);
	}

}

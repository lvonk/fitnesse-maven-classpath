package fitnesse.wikitext.widgets;

@SuppressWarnings("serial")
public class MavenClasspathExtractionException extends Exception {

    public MavenClasspathExtractionException(String message, Throwable cause) {
        super(message, cause);
    }

    public MavenClasspathExtractionException(Throwable cause) {
        super(cause);
    }
}

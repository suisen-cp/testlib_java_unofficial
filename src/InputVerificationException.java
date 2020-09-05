package src;

public class InputVerificationException extends Exception {
    private static final long serialVersionUID = 1L;

    private static final String errorMessageFormat = "Verification failed at line %d.\n\texpected: %s\n\tactual: %s";

    private static final int OMIT_THRETHOLD = 1000;
    
    public InputVerificationException(int line, String expected, String actual) {
        super(String.format(errorMessageFormat, line, expected, actual.length() >= OMIT_THRETHOLD ? "(omit)" : actual));
    }
}
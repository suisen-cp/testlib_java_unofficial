package src;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

public class InputVerifier {

    private static final Pattern DOUBLE_PATTERN = Pattern.compile("-?(0|[1-9]+[0-9]*)\\.[0-9]*[1-9]");

    private static final BiFunction<Integer, Integer, Pattern> DOUBLE_STRICT_PATTERN_BUILDER = new BiFunction<>() {
        public Pattern apply(Integer minAfterPointDigitCountInclusive, Integer maxAfterPointDigitCountInclusive) {
            return Pattern.compile(String.format("-?(0|[1-9]+[0-9]*)\\.[0-9]{%d,%d}[1-9]", minAfterPointDigitCountInclusive - 1, maxAfterPointDigitCountInclusive - 1));
        }
    };

    private static final int BUFFER_SIZE = 1024;

    private InputStream in;

    private byte[] buf = new byte[BUFFER_SIZE];
    private int bufptr = 0;
    private int buflen = 0;

    private byte[] undo = new byte[BUFFER_SIZE];
    private int undoptr = 0;
    private int undolen = 0;

    private int line = 1;

    public InputVerifier(InputStream in) {
        Objects.requireNonNull(in);
        this.in = in;
    }

    public InputVerifier() {
        this(System.in);
    }

    private boolean hasNextByte() throws IOException {
        if (undoptr < undolen) return true;
        undoptr = undolen = 0;
        if (bufptr < buflen) return true;
        bufptr = 0;
        buflen = in.read(buf);
        return buflen > 0;
    }

    private int readByte() throws IOException {
        int b;
        if (undoptr < undolen) {
            b = undo[undoptr++];
        } else if (hasNextByte()) {
            b = buf[bufptr++];
        } else {
            b = -1;
        }
        if (isNewLine(b)) line++;
        return b;
    }
    
    public void readEOF() throws IOException, InputVerificationException {
        if (hasNextByte()) {
            int b = readByte();
            undo[undolen++] = (byte) b;
            throw new InputVerificationException(line, "EOF", toReadableString(b));
        }
    }

    public void readNewLine() throws IOException, InputVerificationException {
        if (!hasNextByte()) {
            throw new InputVerificationException(line, "EOL", "EOF");
        }
        int b = readByte();
        if (!isNewLine(b)) {
            undo[undolen++] = (byte) b;
            throw new InputVerificationException(line, "EOL", toReadableString(b));
        }
    }

    public void readSpace() throws IOException, InputVerificationException {
        if (!hasNextByte()) {
            throw new InputVerificationException(line, "Space", "EOF");
        }
        int b = readByte();
        if (!isSpace(b)) {
            undo[undolen++] = (byte) b;
            throw new InputVerificationException(line, "Space", toReadableString(b));
        }
    }

    public char readLowerCaseCharacter() throws IOException, InputVerificationException {
        if (!hasNextByte()) {
            throw new InputVerificationException(line, "Lower-case character", "EOF");
        }
        int b = readByte();
        if (!isLowerCaseAlphabet(b)) {
            undo[undolen++] = (byte) b;
            throw new InputVerificationException(line, "Lower-case character", toReadableString(b));
        }
        return (char) b;
    }

    public char readUpperCaseCharacter() throws IOException, InputVerificationException {
        if (!hasNextByte()) {
            throw new InputVerificationException(line, "Upper-case character", "EOF");
        }
        int b = readByte();
        if (!isUpperCaseAlphabet(b)) {
            undo[undolen++] = (byte) b;
            throw new InputVerificationException(line, "Upper-case character", toReadableString(b));
        }
        return (char) b;
    }

    public String readToken() throws IOException, InputVerificationException {
        if (!hasNextByte()) {
            throw new InputVerificationException(line, "Token", "EOF");
        }
        var sb = new StringBuilder();
        int b = readByte();
        for (;isPrintableAsciiCharacter(b); b = readByte()) {
            sb.appendCodePoint(b);
        }
        if (b != -1) undo[undolen++] = (byte) b;
        if (sb.length() == 0) {
            throw new InputVerificationException(line, "Token", toReadableString(b));
        }
        return sb.toString();
    }

    public String readToken(int expectedLength) throws IOException, InputVerificationException {
        var token = readToken();
        int actualLength = token.length();
        if (actualLength != expectedLength) {
            throw new InputVerificationException(line, String.format("Token with the length %d", expectedLength), String.format("Token with the length %d", actualLength));
        }
        return token;
    }

    public String readToken(Pattern regexPattern) throws IOException, InputVerificationException {
        var token = readToken();
        if (!regexPattern.matcher(token).matches()) {
            throw new InputVerificationException(line, String.format("token that matches with %s.", regexPattern.toString()), token);
        }
        return token;
    }

    public String[] readTokens(int size, char delimiter) throws IOException, InputVerificationException {
        var tokens = new String[size];
        for (int i = 0; i < size; i++) {
            tokens[i] = readToken();
            if (i < size - 1) {
                int b = readByte();
                if (b != delimiter) {
                    throw new InputVerificationException(line, toReadableString(delimiter), toReadableString(b));
                }
            }
        }
        return tokens;
    }

    public String[] readTokens(int size, Pattern regex, char delimiter) throws IOException, InputVerificationException {
        var tokens = new String[size];
        for (int i = 0; i < size; i++) {
            tokens[i] = readToken(regex);
            if (i < size - 1) {
                int b = readByte();
                if (b != delimiter) {
                    throw new InputVerificationException(line, toReadableString(delimiter), toReadableString(b));
                }
            }
        }
        return tokens;
    }

    public String readLowerCaseToken() throws IOException, InputVerificationException {
        var token = readToken();
        if (!token.codePoints().allMatch(InputVerifier::isLowerCaseAlphabet)) {
            throw new InputVerificationException(line, "lower-case token", token);
        }
        return token;
    }

    public String readLowerCaseToken(int expectedLength) throws IOException, InputVerificationException {
        var token = readLowerCaseToken();
        int actualLength = token.length();
        if (actualLength != expectedLength) {
            throw new InputVerificationException(line, String.format("token with the length %d", expectedLength), String.format("token with the length %d", actualLength));
        }
        return token;
    }

    public String[] readLowerCaseTokens(int size, char delimiter) throws IOException, InputVerificationException {
        var tokens = new String[size];
        for (int i = 0; i < size; i++) {
            tokens[i] = readLowerCaseToken();
            if (i < size - 1) {
                int b = readByte();
                if (b != delimiter) {
                    throw new InputVerificationException(line, toReadableString(delimiter), toReadableString(b));
                }
            }
        }
        return tokens;
    }

    public String readUpperCaseToken() throws IOException, InputVerificationException {
        var token = readToken();
        if (!token.codePoints().allMatch(InputVerifier::isUpperCaseAlphabet)) {
            throw new InputVerificationException(line, "upper-case token", token);
        }
        return token;
    }

    public String readUpperCaseToken(int expectedLength) throws IOException, InputVerificationException {
        var token = readUpperCaseToken();
        int actualLength = token.length();
        if (actualLength != expectedLength) {
            throw new InputVerificationException(line, String.format("token with the length %d", expectedLength), String.format("token with the length %d", actualLength));
        }
        return token;
    }

    public String[] readUpperCaseTokens(int size, char delimiter) throws IOException, InputVerificationException {
        var tokens = new String[size];
        for (int i = 0; i < size; i++) {
            tokens[i] = readUpperCaseToken();
            if (i < size - 1) {
                int b = readByte();
                if (b != delimiter) {
                    throw new InputVerificationException(line, toReadableString(delimiter), toReadableString(b));
                }
            }
        }
        return tokens;
    }

    public long readLong() throws IOException, InputVerificationException {
        var token = readToken();
        try {
            return Long.parseLong(token);
        } catch (NumberFormatException e) {
            throw new InputVerificationException(line, "long", token);
        }
    }

    public long readLong(long minInclusive, long maxInclusive) throws IOException, InputVerificationException {
        if (minInclusive > maxInclusive) {
            throw new IllegalArgumentException("Empty range.");
        }
        long v = readLong();
        if (minInclusive > v || v > maxInclusive) {
            throw new InputVerificationException(line, String.format("long in [%d, %d]", minInclusive, maxInclusive), String.valueOf(v));
        }
        return v;
    }

    public long[] readLongs(int size, long minInclusive, long maxInclusive, char delimiter) throws IOException, InputVerificationException {
        var longs = new long[size];
        for (int i = 0; i < size; i++) {
            longs[i] = readLong(minInclusive, maxInclusive);
            if (i < size - 1) {
                int b = readByte();
                if (b != delimiter) {
                    throw new InputVerificationException(line, toReadableString(delimiter), toReadableString(b));
                }
            }
        }
        return longs;
    }

    public int readInt() throws IOException, InputVerificationException {
        var token = readToken();
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException e) {
            throw new InputVerificationException(line, "int", token);
        }
    }

    public int readInt(int minInclusive, int maxInclusive) throws IOException, InputVerificationException {
        if (minInclusive > maxInclusive) {
            throw new IllegalArgumentException("Empty range.");
        }
        int v = readInt();
        if (minInclusive > v || v > maxInclusive) {
            throw new InputVerificationException(line, String.format("int in [%d, %d]", minInclusive, maxInclusive), String.valueOf(v));
        }
        return v;
    }

    public int[] readInts(int size, int minInclusive, int maxInclusive, char delimiter) throws IOException, InputVerificationException {
        var ints = new int[size];
        for (int i = 0; i < size; i++) {
            ints[i] = readInt(minInclusive, maxInclusive);
            if (i < size - 1) {
                int b = readByte();
                if (b != delimiter) {
                    throw new InputVerificationException(line, toReadableString(delimiter), toReadableString(b));
                }
            }
        }
        return ints;
    }

    public double readDouble() throws IOException, InputVerificationException {
        var token = readToken();
        if (!DOUBLE_PATTERN.matcher(token).matches()) {
            throw new InputVerificationException(line, "double", token);
        }
        return Double.parseDouble(token);
    }

    public double readDouble(double minInclusive, double maxInclusive) throws IOException, InputVerificationException {
        if (minInclusive > maxInclusive) {
            throw new IllegalArgumentException("Empty range.");
        }
        double v = readDouble();
        if (minInclusive > v || v > maxInclusive) {
            throw new InputVerificationException(line, String.format("double in [%f, %f]", minInclusive, maxInclusive), String.valueOf(v));
        }
        return v;
    }

    public double readDoubleStrict(double minInclusive, double maxInclusive, int minAfterPointDigitCountInclusive, int maxAfterPointDigitCountInclusive) throws IOException, InputVerificationException {
        if (minInclusive > maxInclusive || minAfterPointDigitCountInclusive > maxAfterPointDigitCountInclusive) {
            throw new IllegalArgumentException("Empty range.");
        }
        var token = readToken();
        if (!DOUBLE_STRICT_PATTERN_BUILDER.apply(minAfterPointDigitCountInclusive, maxAfterPointDigitCountInclusive).matcher(token).matches()) {
            throw new InputVerificationException(line, String.format("a double value that has [%d, %d] digits after the decimal point and is in [%f, %f]", minAfterPointDigitCountInclusive, maxAfterPointDigitCountInclusive, minInclusive, maxInclusive), token);
        }
        double v = Double.parseDouble(token);
        if (minInclusive > v || v > maxInclusive) {
            throw new InputVerificationException(line, String.format("a double value that has [%d, %d] digits after the decimal point and is in [%f, %f]", minAfterPointDigitCountInclusive, maxAfterPointDigitCountInclusive, minInclusive, maxInclusive), token);
        }
        return v;
    }

    public double[] readDoubles(int size, double minInclusive, double maxInclusive, char delimiter) throws IOException, InputVerificationException {
        var doubles = new double[size];
        for (int i = 0; i < size; i++) {
            doubles[i] = readDouble(minInclusive, maxInclusive);
            if (i < size - 1) {
                int b = readByte();
                if (b != delimiter) {
                    throw new InputVerificationException(line, toReadableString(delimiter), toReadableString(b));
                }
            }
        }
        return doubles;
    }

    public double[] readDoublesStrict(int size, double minInclusive, double maxInclusive, int minAfterPointDigitCountInclusive, int maxAfterPointDigitCountInclusive, char delimiter) throws IOException, InputVerificationException {
        var doubles = new double[size];
        for (int i = 0; i < size; i++) {
            doubles[i] = readDoubleStrict(minInclusive, maxInclusive, minAfterPointDigitCountInclusive, maxAfterPointDigitCountInclusive);
            if (i < size - 1) {
                int b = readByte();
                if (b != delimiter) {
                    throw new InputVerificationException(line, toReadableString(delimiter), toReadableString(b));
                }
            }
        }
        return doubles;
    }

    private static String toReadableString(int codePoint) {
        if (!Character.isDefined(codePoint)) {
            return "undefined";
        }
        if (isNewLine(codePoint)) {
            return "new line";
        }
        if (Character.isWhitespace(codePoint)) {
            return "white space";
        }
        if (Character.isISOControl(codePoint)) {
            return "control character";
        }
        return Character.toString(codePoint);
    }

    private static boolean isNewLine(int codePoint) {
        return codePoint == '\n';
    }

    private static boolean isSpace(int codePoint) {
        return codePoint == ' ';
    }

    private static boolean isLowerCaseAlphabet(int codePoint) {
        return 'a' <= codePoint && codePoint <= 'z';
    }

    private static boolean isUpperCaseAlphabet(int codePoint) {
        return 'A' <= codePoint && codePoint <= 'Z';
    }

    private static boolean isPrintableAsciiCharacter(int codePoint) {
        return 32 < codePoint && codePoint < 127;
    }
}

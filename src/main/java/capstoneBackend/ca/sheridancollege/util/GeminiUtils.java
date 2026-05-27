package capstoneBackend.ca.sheridancollege.util;

/**
 * Shared utilities for working with Gemini API responses.
 */
public final class GeminiUtils {

    private GeminiUtils() {}

    /**
     * Strips Markdown code-block fences that Gemini sometimes wraps around JSON responses.
     * Handles both {@code ```json ... ```} and plain {@code ``` ... ```} variants.
     *
     * @param text raw text from Gemini
     * @return the text with any surrounding code-block fences removed
     */
    public static String stripMarkdownCodeBlock(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline != -1) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.lastIndexOf("```")).trim();
            }
        }
        return trimmed;
    }
}

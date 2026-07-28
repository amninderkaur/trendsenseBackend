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

    /**
     * Extracts a JSON array from text that may contain surrounding prose.
     * Finds the outermost {@code [ ... ]} delimiters and returns that substring.
     * Falls back to the original text if no array delimiters are found.
     *
     * @param text text that should contain a JSON array
     * @return the extracted JSON array string, or the original text unchanged
     */
    public static String extractJsonArray(String text) {
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }
}

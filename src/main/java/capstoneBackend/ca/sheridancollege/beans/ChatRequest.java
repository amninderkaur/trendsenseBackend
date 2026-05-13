package capstoneBackend.ca.sheridancollege.beans;

import java.util.List;

import lombok.Data;

@Data
public class ChatRequest {
    /** The user's latest message */
    private String message;

    /**
     * Previous turns in the conversation (oldest first).
     * Each entry has role ("user" | "model") and text.
     * Send an empty list or omit for the first message.
     */
    private List<ChatMessage> history;
}

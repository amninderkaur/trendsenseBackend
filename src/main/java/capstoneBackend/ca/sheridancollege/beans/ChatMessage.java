package capstoneBackend.ca.sheridancollege.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single turn in the chat history.
 * role must be "user" or "model".
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private String role;   // "user" | "model"
    private String text;
}

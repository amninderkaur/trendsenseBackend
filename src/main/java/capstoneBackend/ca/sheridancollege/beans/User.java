package capstoneBackend.ca.sheridancollege.beans;

import java.util.Collection;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Document(collection = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails{
	@Id
    private String id;
    private String username;
    private String email;
    private String password;
    private Role role;
    private String phoneNumber;
    private boolean hasLoggedInBefore;
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Role effectiveRole = role != null ? role : Role.USER;
        return List.of(new SimpleGrantedAuthority("ROLE_" + effectiveRole.name()));
    }

    @Override
    public String getUsername() {
        return email; 
    }

    @Override
    public String getPassword() {
        return password;
    }

}

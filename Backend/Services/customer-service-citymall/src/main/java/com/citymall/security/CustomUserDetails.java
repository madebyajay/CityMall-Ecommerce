package com.citymall.security;

import com.citymall.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapts our {@link User} to Spring Security. Authority format: ROLE_<NAME>.
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final String loginId;
    private final String passwordHash;
    private final String roleName;
    private final boolean usable;

    public CustomUserDetails(User user) {
        this.userId = user.getUserId();
        this.loginId = user.getUserLoginId();
        this.passwordHash = user.getPassword();
        this.roleName = user.getRole() != null ? user.getRole().getName() : "CUSTOMER";
        this.usable = user.isAccountUsable();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + roleName.toUpperCase()));
    }

    /** SUPERADMIN bypasses every authorization check (mirrors reference role-18 bypass). */
    public boolean isSuperAdmin() {
        return "SUPERADMIN".equalsIgnoreCase(roleName);
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return loginId;
    }

    @Override
    public boolean isAccountNonExpired() {
        return usable;
    }

    @Override
    public boolean isAccountNonLocked() {
        return usable;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return usable;
    }
}

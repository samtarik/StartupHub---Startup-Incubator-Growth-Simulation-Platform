package com.startuphub.security;

import com.startuphub.model.Role;
import com.startuphub.model.Startup;
import com.startuphub.model.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Server-side ownership checks.
 *
 * The frontend hides buttons a role cannot use, but hiding a button is not security -
 * these methods are the real enforcement point, called from the controllers before any
 * state-changing operation runs. ADMIN bypasses ownership; MENTOR is read-only except
 * for milestone progress, which is the mentoring action.
 */
@Component
public class AccessGuard {

    public boolean isAdmin(User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }

    public boolean ownsStartup(User user, Startup startup) {
        return user != null && startup != null && startup.getFounder() != null
                && startup.getFounder().getId().equals(user.getId());
    }

    /** Founder of this startup, or an admin. Anything else is a 403. */
    public void requireOwner(User user, Startup startup) {
        if (isAdmin(user) || ownsStartup(user, startup)) return;
        throw new AccessDeniedException("Only the founder of " + startup.getName() + " can do that.");
    }

    /** Founder, admin, or any mentor - used for roadmap/milestone coaching actions. */
    public void requireOwnerOrMentor(User user, Startup startup) {
        if (isAdmin(user) || ownsStartup(user, startup)) return;
        if (user != null && user.getRole() == Role.MENTOR) return;
        throw new AccessDeniedException("You do not have permission to update this roadmap.");
    }

    /**
     * Editing or deleting an investment: the startup's founder (it is their cap table), an admin,
     * or the investor who made it. Nobody else.
     */
    public void requireOwnerOrInvestor(User user, Startup startup, User investor) {
        if (isAdmin(user) || ownsStartup(user, startup)) return;
        if (user != null && investor != null && investor.getId().equals(user.getId())) return;
        throw new AccessDeniedException("Only the founder or the investor can change this investment.");
    }
}

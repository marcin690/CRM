package wh.plus.crm.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import wh.plus.crm.model.user.User;

/**
 * Centralne miejsce do odczytu zalogowanego użytkownika i decyzji o zakresie widoczności danych.
 * Zasada widoczności: ADMIN widzi wszystko, pozostali — tylko dane swojego zespołu (SalesTeam).
 */
@Service
public class CurrentUserService {

    public static final String PERM_REPORTS = "REPORTS";
    private static final String ROLE_ADMIN = "ADMIN";

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User u) {
            return u;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Brak autoryzacji");
    }

    public boolean isAdmin() {
        return getCurrentUser().getAuthorities().stream()
                .anyMatch(a -> ROLE_ADMIN.equals(a.getAuthority()));
    }

    /** Id zespołu zalogowanego użytkownika lub null, jeśli nie ma przypisanego zespołu. */
    public Long currentTeamId() {
        User u = getCurrentUser();
        return u.getTeam() != null ? u.getTeam().getId() : null;
    }

    /**
     * Filtr zespołu do zapytań analitycznych/list:
     * - ADMIN → null (brak filtra, widzi wszystko),
     * - reszta → id swojego zespołu; brak zespołu → -1 (żaden rekord, bezpieczny domyślny).
     */
    public Long scopeTeamId() {
        if (isAdmin()) return null;
        Long teamId = currentTeamId();
        return teamId != null ? teamId : -1L;
    }

    public boolean canViewReports() {
        User u = getCurrentUser();
        if (u.getAuthorities().stream().anyMatch(a -> ROLE_ADMIN.equals(a.getAuthority()))) return true;
        return u.getPermissions() != null && u.getPermissions().contains(PERM_REPORTS);
    }

    public void requireReports() {
        if (!canViewReports()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Brak dostępu do raportów");
        }
    }

    /**
     * Dostęp do pojedynczego rekordu (oferta/projekt) wg zespołu.
     * ADMIN — zawsze; reszta — tylko gdy rekord należy do jego zespołu.
     */
    public void assertTeamAccess(Long entityTeamId) {
        if (isAdmin()) return;
        Long myTeam = currentTeamId();
        if (myTeam == null || entityTeamId == null || !myTeam.equals(entityTeamId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Brak dostępu do zasobu innego zespołu");
        }
    }
}

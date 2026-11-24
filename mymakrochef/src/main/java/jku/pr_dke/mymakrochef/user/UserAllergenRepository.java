package jku.pr_dke.mymakrochef.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserAllergenRepository
        extends JpaRepository<UserAllergen, UserAllergenId> {

    List<UserAllergen> findAllById_UserId(UUID userId);

    void deleteAllById_UserId(UUID userId);
}

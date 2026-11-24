package jku.pr_dke.mymakrochef.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserPantryRepository
        extends JpaRepository<UserPantry, UUID> {

    List<UserPantry> findAllByUserId(UUID userId);

    Optional<UserPantry> findByIdAndUserId(
            UUID id, UUID userId);

    void deleteAllByUserId(UUID userId);
}
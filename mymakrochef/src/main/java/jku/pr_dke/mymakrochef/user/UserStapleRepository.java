package jku.pr_dke.mymakrochef.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserStapleRepository
        extends JpaRepository<UserStaple, UUID> {

    List<UserStaple> findAllByUserId(UUID userId);

    Optional<UserStaple> findByIdAndUserId(
            UUID id, UUID userId);
}
package jku.pr_dke.mymakrochef.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserProfileRepository
        extends JpaRepository<UserProfile, UUID> {
}

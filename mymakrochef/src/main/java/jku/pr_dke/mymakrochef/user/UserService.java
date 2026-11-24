package jku.pr_dke.mymakrochef.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class UserService {

    private final UserProfileRepository profileRepository;
    private final UserPantryRepository pantryRepository;
    private final UserStapleRepository stapleRepository;
    private final UserAllergenRepository allergenRepository;

    public UserService(
            UserProfileRepository profileRepository,
            UserPantryRepository pantryRepository,
            UserStapleRepository stapleRepository,
            UserAllergenRepository allergenRepository) {

        this.profileRepository = profileRepository;
        this.pantryRepository = pantryRepository;
        this.stapleRepository = stapleRepository;
        this.allergenRepository = allergenRepository;
    }

    // PROFILE

    public UserProfile getProfile(UUID userId) {
        return profileRepository.findById(userId)
                .orElseGet(() -> {
                    UserProfile profile = new UserProfile();
                    profile.setUserId(userId);
                    return profileRepository.save(profile);
                });
    }

    public UserProfile saveProfile(
            UUID userId, UserProfile profile) {

        profile.setUserId(userId);
        return profileRepository.save(profile);
    }

    // PANTRY

    public List<UserPantry> getPantry(UUID userId) {
        return pantryRepository.findAllByUserId(userId);
    }

    public UserPantry addPantryItem(
            UUID userId, UserPantry item) {

        item.setId(null);
        item.setUserId(userId);
        return pantryRepository.save(item);
    }

    public UserPantry updatePantryItem(
            UUID userId, UUID itemId, UserPantry updated) {

        UserPantry item = pantryRepository
                .findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND));

        item.setIngredientName(updated.getIngredientName());
        item.setAmount(updated.getAmount());
        item.setUnit(updated.getUnit());
        item.setSelected(updated.isSelected());

        return pantryRepository.save(item);
    }

    public void deletePantryItem(UUID userId, UUID itemId) {
        pantryRepository.findByIdAndUserId(itemId, userId)
                .ifPresent(pantryRepository::delete);
    }

    public void clearPantry(UUID userId) {
        pantryRepository.deleteAllByUserId(userId);
    }

    // ALWAYS AT HOME

    public List<UserStaple> getStaples(UUID userId) {
        return stapleRepository.findAllByUserId(userId);
    }

    public UserStaple addStaple(
            UUID userId, UserStaple staple) {

        staple.setId(null);
        staple.setUserId(userId);
        return stapleRepository.save(staple);
    }

    public void deleteStaple(UUID userId, UUID stapleId) {
        stapleRepository.findByIdAndUserId(stapleId, userId)
                .ifPresent(stapleRepository::delete);
    }

    // ALLERGENS

    public List<Integer> getAllergens(UUID userId) {
        return allergenRepository.findAllById_UserId(userId)
                .stream()
                .map(allergen -> allergen.getId().getAllergenId())
                .toList();
    }

    public void saveAllergens(
            UUID userId, List<Integer> allergenIds) {

        allergenRepository.deleteAllById_UserId(userId);
        allergenRepository.flush();

        for (Integer allergenId : allergenIds.stream()
                .distinct().toList()) {

            UserAllergen allergen = new UserAllergen();

            allergen.setId(
                    new UserAllergenId(userId, allergenId)
            );

            allergenRepository.save(allergen);
        }
    }
}

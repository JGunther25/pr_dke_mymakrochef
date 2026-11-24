package jku.pr_dke.mymakrochef.user;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(schema = "public", name = "user_allergen")
@Getter
@Setter
@NoArgsConstructor
public class UserAllergen {

    @EmbeddedId
    private UserAllergenId id;
}

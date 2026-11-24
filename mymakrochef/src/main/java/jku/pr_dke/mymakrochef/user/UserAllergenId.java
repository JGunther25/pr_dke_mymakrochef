package jku.pr_dke.mymakrochef.user;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserAllergenId implements Serializable {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "allergen_id")
    private Integer allergenId;
}
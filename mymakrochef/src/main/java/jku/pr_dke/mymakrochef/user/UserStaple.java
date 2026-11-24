package jku.pr_dke.mymakrochef.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(schema = "public", name = "user_staple")
@Getter
@Setter
@NoArgsConstructor
public class UserStaple {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "ingredient_name", nullable = false)
    private String ingredientName;
}

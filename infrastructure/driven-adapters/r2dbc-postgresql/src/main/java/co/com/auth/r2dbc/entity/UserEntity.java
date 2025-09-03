package co.com.auth.r2dbc.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Table("users")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
public class UserEntity {

    @Id
    private UUID userId;
    private String numberDocument;
    private String name;
    private String lastName;
    private LocalDate birthDate;
    private String address;
    private String email;
    private String phone;
    private BigDecimal baseSalary;
    private Long roleId;
}
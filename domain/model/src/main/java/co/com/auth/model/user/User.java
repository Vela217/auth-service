package co.com.auth.model.user;

import co.com.auth.model.role.Role;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class User {
    private String userId;
    private String numberDocument;
    private String name;
    private String lastName;
    private LocalDate birthDate;
    private String address;
    private String email;
    private String phone;
    private BigDecimal baseSalary;
    private Role rol;
}

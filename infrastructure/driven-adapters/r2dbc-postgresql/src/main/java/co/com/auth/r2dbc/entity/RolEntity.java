package co.com.auth.r2dbc.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table("role")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RolEntity {
    @Id
    private Long idRol;
    private String name;
    private String description;
}

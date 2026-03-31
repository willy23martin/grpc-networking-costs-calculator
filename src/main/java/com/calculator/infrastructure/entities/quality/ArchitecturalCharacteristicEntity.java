package com.calculator.infrastructure.entities.quality;

import com.calculator.infrastructure.entities.architecture.ArchitecturalPatternEntity;
import com.calculator.infrastructure.entities.architecture.ArchitecturalTacticEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "architecturalcharacteristic")
public class ArchitecturalCharacteristicEntity { // aka Quality Attribute

    @Id
    @Setter(AccessLevel.NONE)
    private Long id;

    private String name;

    private String description;

    @OneToMany(mappedBy = "architecturalCharacteristic", fetch = FetchType.EAGER)
    @Setter(AccessLevel.NONE)
    @ToString.Exclude
    @Builder.Default
    private List<ArchitecturalPatternEntity> architecturalPatterns = new ArrayList<>();

    @OneToMany(mappedBy = "architecturalCharacteristic", fetch = FetchType.EAGER)
    @Setter(AccessLevel.NONE)
    @ToString.Exclude
    @Builder.Default
    private List<ArchitecturalTacticEntity> architecturalTactics = new ArrayList<>();

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        ArchitecturalCharacteristicEntity that = (ArchitecturalCharacteristicEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}

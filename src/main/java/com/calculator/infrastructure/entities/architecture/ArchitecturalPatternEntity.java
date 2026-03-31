package com.calculator.infrastructure.entities.architecture;

import com.calculator.infrastructure.entities.quality.ArchitecturalCharacteristicEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Entity
@Table(name = "architecturalpattern")
@Builder
public class ArchitecturalPatternEntity {

    @Id
    @Setter(AccessLevel.NONE)
    private Long id;

    private String name;

    private String description;

    @OneToMany(mappedBy = "architecturalPattern", fetch = FetchType.EAGER)
    @Setter(AccessLevel.NONE)
    @ToString.Exclude
    @Builder.Default
    private List<ArchitecturalTacticEntity> architecturalTactics = new ArrayList<>();

    @ManyToOne(fetch = FetchType.EAGER)
    private ArchitecturalCharacteristicEntity architecturalCharacteristic;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        ArchitecturalPatternEntity that = (ArchitecturalPatternEntity) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}

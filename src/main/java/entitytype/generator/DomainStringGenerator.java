package entitytype.generator;

import jakarta.persistence.metamodel.EntityType;

import java.util.Set;

public interface DomainStringGenerator {
    String getDomainString(Set<EntityType<?>> entityTypes);
}

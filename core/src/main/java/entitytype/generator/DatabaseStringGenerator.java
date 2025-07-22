package entitytype.generator;

import jakarta.persistence.metamodel.EntityType;

import java.util.Set;

public interface DatabaseStringGenerator {
    String getDatabaseString(Set<EntityType<?>> entityTypes);
}

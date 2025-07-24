package entitytype.generator;

import jakarta.persistence.metamodel.EntityType;

import java.util.Set;

/**
 * Generates a database-specific string representation for a given set of JPA entity types.
 *
 * <p>
 * Implementations of this interface are responsible for producing a textual description or
 * representation of the specified JPA entity types.
 * </p>
 *
 * @author Felix Seggebäing
 */
public interface DatabaseStringGenerator {
    /**
     * Generates a database string representation for the given set of JPA entity types.
     *
     * @param entityTypes the set of JPA entity types to represent; must not be {@code null}
     * @return a string describing or representing the given entity types
     */
    String getDatabaseString(Set<EntityType<?>> entityTypes);
}

package entitytype.generator;

import jakarta.persistence.metamodel.EntityType;

import java.util.Set;

/**
 * Generates a domain-specific string representation for a given set of JPA entity types.
 *
 * <p>
 * Implementations of this interface are responsible for producing a textual description
 * or representation of the provided JPA entity types, focusing on domain or business aspects.
 * </p>
 *
 * @author Felix Seggebäing
 */
public interface DomainStringGenerator {
    /**
     * Generates a domain-specific string representation for the given set of JPA entity types.
     *
     * @param entityTypes the set of JPA entity types to represent; must not be {@code null}
     * @return a string describing the domain aspects of the given entity types
     */
    String getDomainString(Set<EntityType<?>> entityTypes);
}

package entitytype.generator;

import jakarta.persistence.metamodel.EntityType;

import java.io.InputStream;
import java.util.Set;

/**
 * Generates a UML diagram as an image stream for a given set of JPA entity types.
 *
 * <p>
 * Implementations of this interface are responsible for producing a graphical representation
 * of the specified entity types and their relationships, provided as an {@link InputStream}.
 * </p>
 *
 * @author Felix Seggebäing
 */
public interface UMLGenerator {
    /**
     * Generates a UML diagram image stream for the given set of JPA entity types.
     *
     * @param entityTypes the set of JPA entity types to visualize; must not be {@code null}
     * @return an {@link InputStream} containing the image data for the UML diagram
     */
    InputStream getGraphImageStream(Set<EntityType<?>> entityTypes);
}

package entitytype.generator;

import jakarta.persistence.metamodel.EntityType;

import java.io.InputStream;
import java.util.Set;

public interface UMLGenerator {
    InputStream getGraphImageStream(Set<EntityType<?>> entityTypes);
}

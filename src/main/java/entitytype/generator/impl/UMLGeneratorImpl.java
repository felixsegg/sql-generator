package entitytype.generator.impl;

import entitytype.generator.UMLGenerator;
import jakarta.persistence.Entity;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.PluralAttribute;
import net.sourceforge.plantuml.SourceStringReader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class UMLGeneratorImpl implements UMLGenerator {
    private static final UMLGeneratorImpl instance = new UMLGeneratorImpl();
    
    private UMLGeneratorImpl() {}
    
    public static UMLGeneratorImpl getInstance() {
        return instance;
    }
    
    @Override public InputStream getGraphImageStream(Set<EntityType<?>> entityTypes) {
        String plantUmlSource = generatePlantUmlSource(entityTypes);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        SourceStringReader reader = new SourceStringReader(plantUmlSource);
        try {
            reader.outputImage(os);
        } catch (IOException ex) {
            return null;
        }
        return new ByteArrayInputStream(os.toByteArray());
    }
    
    private String generatePlantUmlSource(Set<EntityType<?>> entityTypes) {
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        
        Map<String, String> inheritance = new HashMap<>();
        Set<String> entities = new HashSet<>();
        Set<String> associations = new HashSet<>();
        
        for (EntityType<?> entity : entityTypes) {
            String entityName = entity.getName();
            entities.add(entityName);
            
            // Vererbung
            Class<?> javaType = entity.getJavaType();
            Class<?> superClass = javaType.getSuperclass();
            if (superClass != null && entityTypes.stream().anyMatch(e -> e.getJavaType().equals(superClass))) {
                inheritance.put(entityName, superClass.getSimpleName());
            }
            
            // Assoziationen
            for (Attribute<?, ?> attr : entity.getAttributes()) {
                switch (attr.getPersistentAttributeType()) {
                    case MANY_TO_ONE:
                    case ONE_TO_ONE:
                    case ONE_TO_MANY:
                    case MANY_TO_MANY:
                        Class<?> targetType = attr.getJavaType();
                        // nur @Entity zulassen, sonst bei Collections Elementtyp prüfen
                        if (!targetType.isAnnotationPresent(Entity.class)) {
                            if (attr instanceof PluralAttribute<?, ?, ?>) {
                                targetType = ((PluralAttribute<?, ?, ?>) attr).getElementType().getJavaType();
                            }
                        }
                        // wenn immer noch kein Entity, überspringen
                        if (!targetType.isAnnotationPresent(Entity.class)) {
                            continue;
                        }
                        // nur Beziehungen zwischen bekannten EntityTypes
                        Class<?> finalTargetType = targetType;
                        boolean known = entityTypes.stream().anyMatch(e -> e.getJavaType().equals(finalTargetType));
                        if (!known) {
                            continue;
                        }
                        String targetName = targetType.getSimpleName();
                        // Duplikate vermeiden
                        String pair = entityName.compareTo(targetName) < 0 ? entityName + "--" + targetName : targetName + "--" + entityName;
                        associations.add(pair);
                        break;
                    default:
                        // andere Attribute ignorieren
                }
            }
        }
        
        // Klassendeklarationen
        for (String entity : entities) {
            sb.append("class ").append(entity).append("\n");
        }
        // Beziehungslinien
        for (String assoc : associations) {
            String[] parts = assoc.split("--");
            sb.append(parts[0]).append(" -- ").append(parts[1]).append("\n");
        }
        // Vererbungspfeile
        for (Map.Entry<String, String> entry : inheritance.entrySet()) {
            sb.append(entry.getValue()).append(" <|-- ").append(entry.getKey()).append("\n");
        }
        
        sb.append("@enduml\n");
        return sb.toString();
    }
}

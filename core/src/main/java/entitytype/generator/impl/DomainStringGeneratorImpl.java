package entitytype.generator.impl;

import com.google.gson.GsonBuilder;
import entitytype.generator.DomainStringGenerator;
import jakarta.persistence.*;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;

import java.lang.reflect.Field;
import java.util.*;

public final class DomainStringGeneratorImpl implements DomainStringGenerator {
    private static final DomainStringGeneratorImpl instance = new DomainStringGeneratorImpl();
    
    private final Map<EntityType<?>, Map<String, Object>> entityTypeInfoMap = new LinkedHashMap<>();
    
    private DomainStringGeneratorImpl() {}
    
    public static DomainStringGeneratorImpl getInstance() {
        return instance;
    }
    
    @Override public String getDomainString(Set<EntityType<?>> entityTypes) {
        List<Map<String, Object>> entities = new ArrayList<>();
        for (EntityType<?> et : entityTypes) {
            if (!entityTypeInfoMap.containsKey(et)) entityTypeInfoMap.put(et, getEntityInfo(et));
            entities.add(entityTypeInfoMap.get(et));
        }
        
        return new GsonBuilder().setPrettyPrinting().create().toJson(entities);
    }
    
    private Map<String, Object> getEntityInfo(EntityType<?> entity) {
        Map<String, Object> entityInfo = new LinkedHashMap<>();
        
        entityInfo.put("entityTypeName", entity.getName());
        
        // Auf Vererbung checken
        if (entity.getJavaType().getSuperclass().isAnnotationPresent(Entity.class)) {
            entityInfo.put("superclassEntity", entity.getJavaType().getSuperclass().getSimpleName());
            entityInfo.put("inheritanceType", getInheritanceType(entity));
        }
        
        // Attribut-Infos hinzufügen, getrennt für einfache Attribute und solche, die eine Beziehung darstellen.
        List<Map<String, Object>> simpleAttributes = new ArrayList<>();
        List<Map<String, Object>> relationshipAttributes = new ArrayList<>();
        for (Attribute<?, ?> attr : entity.getAttributes()) {
            // Wesentliche Informationen extrahieren
            Map<String, Object> attrInfo = getAttributeInfo(attr);
            
            // Fallunterscheidung zwischen einfachen und Beziehungsattributen
            if (attr.isAssociation()) {
                attrInfo.put("isMappingSideInDB", isOwningSide(attr));
                relationshipAttributes.add(attrInfo);
            } else simpleAttributes.add(attrInfo);
        }
        
        entityInfo.put("simpleAttributes", simpleAttributes);
        entityInfo.put("relationshipAttributes", relationshipAttributes);
        
        return entityInfo;
    }
    
    private Map<String, Object> getAttributeInfo(Attribute<?, ?> attr) {
        Map<String, Object> attrInfo = new LinkedHashMap<>();
        attrInfo.put("attrName", attr.getName());
        
        if (attr instanceof SingularAttribute<?, ?> sAttr) {
            attrInfo.put("attrCategory", "singular");
            attrInfo.put("attrType", sAttr.getJavaType().getSimpleName());
        } else if (attr instanceof PluralAttribute<?, ?, ?> pAttr) {
            attrInfo.put("attrCategory", "plural");
            attrInfo.put("attrType", pAttr.getElementType().getJavaType().getSimpleName());
            attrInfo.put("collectionType", pAttr.getCollectionType());
        } else {
            attrInfo.put("attrCategory", attr.getClass().getSimpleName());
            attrInfo.put("INFO", "Attribute appears to be neither singular nor plural. This is not supported yet.");
        }
        return attrInfo;
    }
    
    private boolean isOwningSide(Attribute<?, ?> attr) {
        try {
            Field field = attr.getDeclaringType().getJavaType().getDeclaredField(attr.getName());
            
            JoinTable joinTable = field.getAnnotation(JoinTable.class);
            OneToOne oneToOne = field.getAnnotation(OneToOne.class);
            OneToMany oneToMany = field.getAnnotation(OneToMany.class);
            ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
            ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
            
            // Wenn @ManyToOne vorhanden ist, dann ist es immer die mappende Seite
            if (joinTable != null) return false;
            else if (manyToOne != null) return true;
            else if (oneToOne != null) return oneToOne.mappedBy().isEmpty();
            else if (oneToMany != null) return oneToMany.mappedBy().isEmpty();
            else if (manyToMany != null) return manyToMany.mappedBy().isEmpty();
            else return true;
        } catch (NoSuchFieldException ex) {
            throw new IllegalArgumentException("The attribute did not stem from an existing field: " + ex.getMessage());
        }
    }
    
    private InheritanceType getInheritanceType(EntityType<?> entity) {
        Class<?> entityClass = entity.getJavaType();
        
        // Falls die Klasse eine MappedSuperclass ist, gibt es keine eigene Vererbungsstrategie
        boolean isMappedSuperclass = entityClass.isAnnotationPresent(MappedSuperclass.class);
        
        // Durchlaufe die Klassenhierarchie, um geerbte Strategie zu finden
        while (entityClass != Object.class) {
            if (entityClass.isAnnotationPresent(Entity.class) && entityClass.getAnnotation(Inheritance.class) != null)
                return entityClass.getAnnotation(Inheritance.class).strategy(); // Falls gefunden, direkt zurückgeben
            
            // Beende, falls die Superklasse keine Entity ist
            entityClass = entityClass.getSuperclass();
            if (entityClass == null) return isMappedSuperclass ? null : InheritanceType.SINGLE_TABLE; // Standardwert
        }
        
        // Falls keine Strategie gefunden wurde, aber eine Entity vorhanden ist, verwende Standardwert
        return InheritanceType.SINGLE_TABLE;
    }
}
package entitytype.generator.impl;

import com.google.gson.GsonBuilder;
import access.JpaAccessHolder;
import access.JpaAccess;
import entitytype.generator.DatabaseStringGenerator;
import jakarta.persistence.*;
import jakarta.persistence.metamodel.*;
import org.hibernate.MappingException;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility class for analyzing the JPA metamodel and the underlying database schema.
 *
 * <p>
 * Provides JSON representations of entity and join tables, allowing selection of relevant
 * entity types for SQL SELECT statement generation. This class implements {@link DatabaseStringGenerator}
 * as a singleton.
 * </p>
 *
 * @author Felix Seggebäing
 */
public final class DatabaseStringGeneratorImpl implements DatabaseStringGenerator {
    private static final DatabaseStringGeneratorImpl instance = new DatabaseStringGeneratorImpl();
    
    private final JpaAccess access = JpaAccessHolder.get();
    
    private final Map<EntityType<?>, Map<String, Object>> entityTableInfoMap = new LinkedHashMap<>();
    private final Map<String, Map<String, Object>> joinTableInfoMap = new LinkedHashMap<>();
    private final Map<String, Set<String>> joinTableParticipantMap = new LinkedHashMap<>();
    
    private DatabaseStringGeneratorImpl() {
    }
    
    /**
     * Returns the singleton instance of {@link DatabaseStringGenerator}.
     *
     * @return the singleton instance
     */
    public static DatabaseStringGenerator getInstance() {
        return instance;
    }
    
    /**
     * Generates a JSON string representation of the given set of JPA entity types and their related join tables.
     *
     * <p>
     * The output includes details about entity tables and join tables.
     * </p>
     *
     * @param entityTypes the set of JPA entity types to analyze; must not be {@code null}
     * @return a pretty-printed JSON string describing the relevant tables and their structure
     */
    @Override
    public String getDatabaseString(Set<EntityType<?>> entityTypes) {
        if (entityTypes == null) throw new NullPointerException();
        fillMapsWithRelevantTables(entityTypes);
        
        // collect all table information
        Map<String, Object> all = new LinkedHashMap<>();
        all.put("Entity Type Tables", getRelevantEntityTableInfos(entityTypes));
        all.put("Join Tables", getRelevantJoinTableInfos(entityTypes));
        
        return new GsonBuilder().setPrettyPrinting().create().toJson(all);
    }
    
    private Set<String> getTableNames(Class<?> clazz) {
        if (!clazz.isAnnotationPresent(Entity.class)) return Collections.emptySet();
        
        Set<String> tables = new LinkedHashSet<>();
        Table table = clazz.getAnnotation(Table.class);
        if (table != null && !table.name().isEmpty()) {
            tables.add(table.name());
        }
        SecondaryTables secondaryTables = clazz.getAnnotation(SecondaryTables.class);
        if (secondaryTables != null) for (SecondaryTable sec : secondaryTables.value())
            if (!sec.name().isEmpty()) tables.add(sec.name());
        
        // fallback, if no annotation present, standard table name (class name)
        if (tables.isEmpty()) tables.add(clazz.getSimpleName());
        
        return tables;
    }
    
    private String getJoinTableName(Field joinTableField) {
        if (!joinTableField.isAnnotationPresent(JoinTable.class))
            throw new IllegalArgumentException("Field must be JoinTable-annotated!");
        
        JoinTable jt = joinTableField.getAnnotation(JoinTable.class);
        if (!jt.name().isEmpty()) return jt.name();
        
        // default name as defined by Hibernate
        String owner = joinTableField.getDeclaringClass().getSimpleName();
        String target;
        if (Collection.class.isAssignableFrom(joinTableField.getType())) {
            ParameterizedType pt = (ParameterizedType) joinTableField.getGenericType();
            target = ((Class<?>) pt.getActualTypeArguments()[0]).getSimpleName();
        } else {
            target = joinTableField.getType().getSimpleName();
        }
        if (owner.compareTo(target) > 0) {
            String tmp = owner;
            owner = target;
            target = tmp;
        }
        return owner + "_" + target;
    }
    
    private Set<String> getJoinTableParticipants(Field joinTableField) {
        Set<String> tables = new LinkedHashSet<>(2);
        
        // owner
        Class<?> owner = joinTableField.getDeclaringClass();
        tables.addAll(getTableNames(owner));
        
        // target
        Class<?> target = null;
        if (joinTableField.getType().isAnnotationPresent(Entity.class)) {
            target = joinTableField.getType();
        } else {
            // check type parameter, e.g. to extract from property
            java.lang.reflect.Type genType = joinTableField.getGenericType();
            if (genType instanceof ParameterizedType pt) {
                for (java.lang.reflect.Type arg : pt.getActualTypeArguments()) {
                    if (arg instanceof Class<?> clazz && clazz.isAnnotationPresent(Entity.class)) {
                        target = clazz;
                        break;
                    }
                    // check further nesting
                    if (arg instanceof ParameterizedType nestedPt) {
                        for (java.lang.reflect.Type nestedArg : nestedPt.getActualTypeArguments()) {
                            if (nestedArg instanceof Class<?> nestedClazz && nestedClazz.isAnnotationPresent(Entity.class)) {
                                target = nestedClazz;
                                break;
                            }
                        }
                        if (target != null) break;
                    }
                }
            }
        }
        
        if (target != null) {
            tables.addAll(getTableNames(target));
        }
        return tables;
    }
    
    private Map<String, Object> getTableInfo(EntityType<?> entityType) {
        Class<?> entityClass = entityType.getJavaType();
        String primaryTable = getPrimaryTableName(entityClass);
        Set<String> secondaryTables = getSecondaryTableNames(entityClass);
        
        // Map: table name → table info
        Map<String, Map<String, Object>> tables = new LinkedHashMap<>();
        
        // initialize all possible tables
        tables.put(primaryTable, makeEmptyTableMap(primaryTable));
        for (String secTable : secondaryTables) {
            tables.put(secTable, makeEmptyTableMap(secTable));
        }
        
        // consider all fields including super class fields
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = entityClass; c != null && c != Object.class; c = c.getSuperclass()) {
            fields.addAll(Arrays.asList(c.getDeclaredFields()));
        }
        
        // map fields to tables
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) continue;
            
            // mappedBy = inverse side -> not in db!
            if ((field.isAnnotationPresent(OneToMany.class) && !field.getAnnotation(OneToMany.class).mappedBy().isEmpty()) || (field.isAnnotationPresent(ManyToMany.class) && !field.getAnnotation(ManyToMany.class).mappedBy().isEmpty()) || (field.isAnnotationPresent(OneToOne.class) && !field.getAnnotation(OneToOne.class).mappedBy().isEmpty())) {
                continue;
            }
            
            String tableName = getTableNameForField(field, primaryTable);
            
            Map<String, Object> tableInfo = tables.get(tableName);
            if (tableInfo == null) continue; // Falsche/unerwartete Tabellennamen ignorieren
            
            
            Map<String, Object> colInfo = new LinkedHashMap<>();
            String colName;
            if (field.isAnnotationPresent(Column.class) && !field.getAnnotation(Column.class).name().isEmpty()) {
                colName = field.getAnnotation(Column.class).name();
            } else if (field.isAnnotationPresent(JoinColumn.class) && !field.getAnnotation(JoinColumn.class).name().isEmpty()) {
                colName = field.getAnnotation(JoinColumn.class).name();
            } else {
                colName = field.getName();
            }
            colInfo.put("name", colName);
            colInfo.put("type", getSqlColumnType(entityClass, field.getName()));
            
            // constraints
            Column columnAnn = field.getAnnotation(Column.class);
            if (columnAnn != null) {
                colInfo.put("nullable", columnAnn.nullable());
                colInfo.put("unique", columnAnn.unique());
                colInfo.put("length", columnAnn.length());
            }
            // primary key
            if (field.isAnnotationPresent(Id.class)) {
                colInfo.put("primaryKey", true);
                @SuppressWarnings("unchecked") List<String> primaryKeys = (List<String>) tableInfo.get("primaryKeys");
                primaryKeys.add(colName);
            } else {
                colInfo.put("primaryKey", false);
            }
            
            @SuppressWarnings("unchecked") List<Map<String, Object>> columns = (List<Map<String, Object>>) tableInfo.get("columns");
            columns.add(colInfo);
        }
        // set join keys for secondary tables to map vertical partitioning
        for (String secTable : secondaryTables) {
            Map<String, Object> secInfo = tables.get(secTable);
            if (secInfo != null) {
                secInfo.put("joinKeys", tables.get(primaryTable).get("primaryKeys"));
            }
        }
        return new LinkedHashMap<>(tables);
    }
    
    private String getTableNameForField(Field field, String primaryTable) {
        String tableName = primaryTable; // default: primary table
        
        if (field.isAnnotationPresent(Column.class)) {
            String colTable = field.getAnnotation(Column.class).table();
            if (!colTable.isEmpty()) tableName = colTable;
        } else if (field.isAnnotationPresent(JoinColumn.class)) {
            String joinColTable = field.getAnnotation(JoinColumn.class).table();
            if (!joinColTable.isEmpty()) tableName = joinColTable;
        }
        return tableName;
    }
    
    private static String getSqlColumnType(Class<?> entityClass, String propertyName) {
        try {
            EntityManager em = JpaAccessHolder.get().getEntityManager();
            Session session = em.unwrap(Session.class);
            SessionFactoryImplementor sfi = (SessionFactoryImplementor) session.getSessionFactory();
            MappingMetamodel mapping = sfi.getMappingMetamodel();
            EntityMappingType entityMapping = mapping.getEntityDescriptor(entityClass);
            if (entityMapping == null) return "UNKNOWN";
            AttributeMapping attrMapping = entityMapping.findAttributeMapping(propertyName);
            if (attrMapping == null) return "UNKNOWN";
            AtomicReference<String> sqlType = new AtomicReference<>(null);
            attrMapping.forEachSelectable((idx, selectable) -> {
                if (sqlType.get() == null && selectable != null && selectable.getJdbcMapping() != null) {
                    sqlType.set(selectable.getJdbcMapping().getJdbcType().getFriendlyName());
                }
            });
            return sqlType.get();
        } catch (MappingException | IllegalArgumentException ex) {
            return "UNKNOWN";
        }
    }
    
    private Map<String, Object> makeEmptyTableMap(String tableName) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("tableName", tableName);
        map.put("columns", new ArrayList<Map<String, Object>>());
        map.put("primaryKeys", new ArrayList<String>());
        return map;
    }
    
    private String getPrimaryTableName(Class<?> clazz) {
        Table table = clazz.getAnnotation(Table.class);
        if (table != null && !table.name().isEmpty()) return table.name();
        return clazz.getSimpleName();
    }
    
    private Set<String> getSecondaryTableNames(Class<?> clazz) {
        Set<String> result = new LinkedHashSet<>();
        SecondaryTable sec = clazz.getAnnotation(SecondaryTable.class);
        if (sec != null && !sec.name().isEmpty()) result.add(sec.name());
        SecondaryTables secs = clazz.getAnnotation(SecondaryTables.class);
        if (secs != null) {
            for (SecondaryTable s : secs.value()) {
                if (!s.name().isEmpty()) result.add(s.name());
            }
        }
        return result;
    }
    
    private List<Field> getJoinTableFields(EntityType<?> entityType) {
        List<Field> result = new ArrayList<>();
        Class<?> clazz = entityType.getJavaType();
        
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (field.isAnnotationPresent(JoinTable.class)) {
                    result.add(field);
                }
            }
        }
        return result;
    }
    
    private Map<String, Object> getJoinTableInfo(Field joinTableField) {
        Map<String, Object> info = new LinkedHashMap<>();
        
        JoinTable jt = joinTableField.getAnnotation(JoinTable.class);
        if (jt == null) return info; // keine JoinTable-Annotation
        
        // table name
        String joinTableName = getJoinTableName(joinTableField);
        info.put("tableName", joinTableName);
        
        // participating tables
        Set<String> participants = getJoinTableParticipants(joinTableField);
        info.put("participants", participants);
        
        // all join columns (joinColumns and inverseJoinColumns)
        Set<String> allJoinColumns = new LinkedHashSet<>();
        for (JoinColumn jc : jt.joinColumns()) {
            if (!jc.name().isEmpty()) allJoinColumns.add(jc.name());
        }
        for (JoinColumn ijc : jt.inverseJoinColumns()) {
            if (!ijc.name().isEmpty()) allJoinColumns.add(ijc.name());
        }
        info.put("joinColumns", allJoinColumns);
        
        return info;
    }
    
    private void fillMapsWithRelevantTables(Set<EntityType<?>> entityTypes) {
        for (EntityType<?> et : entityTypes)
            if (!entityTableInfoMap.containsKey(et)) {
                entityTableInfoMap.put(et, getTableInfo(et));
                
                List<Field> joinTableFields = getJoinTableFields(et);
                for (Field jtField : joinTableFields) {
                    String jtName = getJoinTableName(jtField);
                    if (!joinTableInfoMap.containsKey(jtName)) joinTableInfoMap.put(jtName, getJoinTableInfo(jtField));
                    if (!joinTableParticipantMap.containsKey(jtName))
                        joinTableParticipantMap.put(jtName, getJoinTableParticipants(jtField));
                }
            }
    }
    
    private Set<Map<String, Object>> getRelevantEntityTableInfos(Set<EntityType<?>> entityTypes) {
        Set<Map<String, Object>> result = new HashSet<>();
        entityTypes.forEach(et -> result.add(entityTableInfoMap.get(et)));
        return result;
    }
    
    private Set<Map<String, Object>> getRelevantJoinTableInfos(Set<EntityType<?>> entityTypes) {
        Set<String> entityTypeNames = new HashSet<>();
        entityTypes.forEach(et -> entityTypeNames.addAll(getTableNames(et.getJavaType())));
        
        Set<Map<String, Object>> relevantJoinTables = new HashSet<>();
        for (String jtName : joinTableParticipantMap.keySet()) {
            List<String> participants = joinTableParticipantMap.get(jtName).stream().toList();
            if (participants.size() == 1 && entityTypeNames.contains(participants.get(0)) // self joins
                    || participants.size() == 2 && entityTypeNames.contains(participants.get(0)) && entityTypeNames.contains(participants.get(1))) // non self joins
                relevantJoinTables.add(joinTableInfoMap.get(jtName));
        }
        return relevantJoinTables;
    }
}

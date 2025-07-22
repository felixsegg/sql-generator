/*
 * Copyright (c) 2014-2025 Gregor Blasche, Daniel Borgmann, Meike Bruells,
 * Dominik Bunz, Bogdan Deynega, Tamino Elgert, Vincent Engel, Tobias Engels,
 * Stephan Fuhrmann, Katharina Gillig, Daniel Gómez Sandow, Michael Guertler,
 * Ezekiel Jason Hadi, Isis Hassel, Patrice Herstix, Lars-Werner Hoeck,
 * Simon Hofmann, Sandra Hoeltervennhoff, Frederik Hutfless, Benedikt Imbusch,
 * Fabian Keil, Maximilian Kirchner, Julia Koehne, Joris Kutzner,
 * Matthew Lavengood, Laura Mai, Justus Mairböck, Daniel Meyer, Tobias Möller,
 * Tobias Moormann, Kai Müller, Lisa Nick, Elizaveta Orlova, Moritz Rehbach,
 * Ulrich Schermuly, Kian Schmalenbach, Tobias Schneider, Felix Seggebäing,
 * Jonah Sieg, Christian Stark, Thi Minh Tam Truong, Moritz Windoffer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package entitytype.generator.impl;

import com.google.gson.GsonBuilder;
import access.JpaAccessHolder;
import access.JpaAccess;
import entitytype.generator.DatabaseStringGenerator;
import jakarta.persistence.*;
import jakarta.persistence.metamodel.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;

/**
 * Hilfsklasse zur Analyse des JPA‐Metamodels und des zugrunde liegenden Datenbankschemas.
 * Stellt JSON‐Repräsentationen bereit und ermöglicht die Auswahl von Entitätstypen
 * für die Generierung von SQL‐SELECT‐Statements.
 *
 * @author Felix Seggebäing
 */
public final class DatabaseStringGeneratorImpl implements DatabaseStringGenerator {
    private static final DatabaseStringGeneratorImpl instance = new DatabaseStringGeneratorImpl();
    
    private final JpaAccess provider = JpaAccessHolder.get();
    
    private final Map<EntityType<?>, Map<String, Object>> entityTableInfoMap = new LinkedHashMap<>();
    private final Map<String, Map<String, Object>> joinTableInfoMap = new LinkedHashMap<>();
    private final Map<String, Set<String>> joinTableParticipantMap = new LinkedHashMap<>();
    
    private DatabaseStringGeneratorImpl() {
    }
    
    public static DatabaseStringGenerator getInstance() {
        return instance;
    }
    
    @Override
    public String getDatabaseString(Set<EntityType<?>> entityTypes) {
        fillMapsWithRelevantTables(entityTypes);
        
        // Alle Tabelleninformationen sammeln
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
        
        // Fallback: Falls keine Annotation vorhanden, Standard-Tabellenname (Klassenname)
        if (tables.isEmpty()) tables.add(clazz.getSimpleName());
        
        return tables;
    }
    
    private String getJoinTableName(Field joinTableField) {
        if (!joinTableField.isAnnotationPresent(JoinTable.class))
            throw new IllegalArgumentException("Field must be JoinTable-annotated!");
        
        JoinTable jt = joinTableField.getAnnotation(JoinTable.class);
        if (!jt.name().isEmpty()) return jt.name();
        
        // Default-Name nach Hibernate-Konvention
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
        
        // Owner
        Class<?> owner = joinTableField.getDeclaringClass();
        tables.addAll(getTableNames(owner));
        
        // Target
        Class<?> target = null;
        if (joinTableField.getType().isAnnotationPresent(Entity.class)) {
            target = joinTableField.getType();
        } else {
            // Typparameter prüfen, z.B. um aus Property zu extrahieren
            java.lang.reflect.Type genType = joinTableField.getGenericType();
            if (genType instanceof ParameterizedType pt) {
                for (java.lang.reflect.Type arg : pt.getActualTypeArguments()) {
                    if (arg instanceof Class<?> clazz && clazz.isAnnotationPresent(Entity.class)) {
                        target = clazz;
                        break;
                    }
                    // Rekursiv weitere Verschachtelung prüfen
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
        
        // Map: table name → tabellen-info
        Map<String, Map<String, Object>> tables = new LinkedHashMap<>();
        
        // Alle möglichen Tabellen initialisieren
        tables.put(primaryTable, makeEmptyTableMap(primaryTable));
        for (String secTable : secondaryTables) {
            tables.put(secTable, makeEmptyTableMap(secTable));
        }
        
        // Alle Felder inklusive Superklassenfelder betrachten
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = entityClass; c != null && c != Object.class; c = c.getSuperclass()) {
            fields.addAll(Arrays.asList(c.getDeclaredFields()));
        }
        
        // Felder den Tabellen zuordnen
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) continue;
            
            // mappedBy = inverse Seite → nicht in DB!
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
            
            // Constraints
            Column columnAnn = field.getAnnotation(Column.class);
            if (columnAnn != null) {
                colInfo.put("nullable", columnAnn.nullable());
                colInfo.put("unique", columnAnn.unique());
                colInfo.put("length", columnAnn.length());
            }
            // Primary Key
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
        // Join-Keys für sekundäre Tabellen setzen, damit die vertikale Partitionierung abgebildet ist
        for (String secTable : secondaryTables) {
            Map<String, Object> secInfo = tables.get(secTable);
            if (secInfo != null) {
                // Die Primärschlüssel-Spalten der Primärtabelle dienen auch als Join-Schlüssel zu SecondaryTable
                secInfo.put("joinKeys", tables.get(primaryTable).get("primaryKeys"));
            }
        }
        return new LinkedHashMap<>(tables);
    }
    
    private String getTableNameForField(Field field, String primaryTable) {
        String tableName = primaryTable; // Default: Primary Table
        
        // Prüfe auf explizite Angabe einer SecondaryTable bei @Column(table = ...)
        if (field.isAnnotationPresent(Column.class)) {
            String colTable = field.getAnnotation(Column.class).table();
            if (!colTable.isEmpty()) tableName = colTable;
        } else if (field.isAnnotationPresent(JoinColumn.class)) {
            String joinColTable = field.getAnnotation(JoinColumn.class).table();
            if (!joinColTable.isEmpty()) tableName = joinColTable;
        }
        return tableName;
    }
    
    private String getSqlColumnType(Class<?> entityClass, String propertyName) {
        String tableName = entityClass.getSimpleName().toUpperCase(); // besser: aus @Table
        String columnName = propertyName.toUpperCase(); // besser: aus @Column
        
        try {
            Field field = entityClass.getDeclaredField(propertyName);
            
            Table tableAnnotation = entityClass.getAnnotation(Table.class);
            if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
                tableName = tableAnnotation.name().toUpperCase();
            }
            
            Column columnAnnotation = field.getAnnotation(Column.class);
            if (columnAnnotation != null && !columnAnnotation.name().isEmpty()) {
                columnName = columnAnnotation.name().toUpperCase();
            }
            
            try (Connection conn = provider.getConnection()) {
                DatabaseMetaData metaData = conn.getMetaData();
                try (ResultSet rs = metaData.getColumns(null, null, tableName, columnName)) {
                    if (rs.next()) {
                        return rs.getString("TYPE_NAME");
                    }
                }
            }
        } catch (Exception e) {
            return "UNKNOWN";
        }
        
        return null;
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
        
        // Tabellenname
        String joinTableName = getJoinTableName(joinTableField);
        info.put("tableName", joinTableName);
        
        // Teilnehmer-Tabellen
        Set<String> participants = getJoinTableParticipants(joinTableField);
        info.put("participants", participants);
        
        // Alle Join-Spalten (joinColumns + inverseJoinColumns zusammengefasst)
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

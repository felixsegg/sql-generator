package entitytype;

import entitytype.generator.DatabaseStringGenerator;
import entitytype.generator.DomainStringGenerator;
import jakarta.persistence.metamodel.EntityType;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Hilfsklasse, die die Auswahl von Entitätstypen verwaltet und
 * den vollständigen Prompt für die SQL‐Generierung zusammenstellt.
 */
public final class EntityTypeSelectionFX implements EntityTypeSelection {
    // Default values
    private DomainStringGenerator domainStringGenerator = DEF_DOMAIN_STRING_GENERATOR;
    private DatabaseStringGenerator databaseStringGenerator = DEF_DATABASE_STRING_GENERATOR;
    private String promptPrefix = DEF_PROMPT_PREFIX;
    private String promptPostfix = DEF_PROMPT_POSTFIX;
    
    // Dynamically saved to save on resources
    private String domainString;
    private String databaseString;
    
    private final Map<EntityType<?>, BooleanProperty> selectionMap = new LinkedHashMap<>();
    
    /**
     * Erstellt ein neues Auswahl‐Objekt mit allen Entitätstypen vorausgewählt.
     *
     * @param entityTypes die Menge der im zu berücksichtigenden Entitätstypen
     */
    public EntityTypeSelectionFX(Set<EntityType<?>> entityTypes) {
        initializeSelectionMap(entityTypes);
    }
    
    private void initializeSelectionMap(Set<EntityType<?>> entityTypes) {
        entityTypes.forEach(et -> {
            BooleanProperty obv = new SimpleBooleanProperty(true);
            obv.addListener((obs, oldV, newV) -> {
                domainString = null;
                databaseString = null;
            });
            selectionMap.put(et, obv);
        });
    }
    
    @Override
    public void setDomainStringGenerator(DomainStringGenerator domainStringGenerator) {
        this.domainStringGenerator = domainStringGenerator;
    }
    
    @Override
    public void setDatabaseStringGenerator(DatabaseStringGenerator databaseStringGenerator) {
        this.databaseStringGenerator = databaseStringGenerator;
    }
    
    @Override
    public void setPromptPrePostfix(String promptPrefix, String promptPostfix) {
        this.promptPrefix = promptPrefix;
        this.promptPostfix = promptPostfix;
    }
    
    /**
     * Gibt die BooleanProperty zurück, mit der die Selektion eines Entitätstyps
     * beobachtet oder geändert werden kann.
     *
     * @param et der jeweilige {@link EntityType}, darf nicht null sein
     * @return die zugehörige {@link BooleanProperty}
     * @throws IllegalArgumentException falls der Typ nicht verwaltet wird
     */
    public BooleanProperty getBooleanPropertyForEntityType(EntityType<?> et) {
        if (et == null || !selectionMap.containsKey(et))
            throw new IllegalArgumentException("Key is either null or not in map.");
        else return selectionMap.get(et);
    }
    
    /**
     * Gibt zurück, ob ein Entitätstyp ausgewählt ist.
     *
     * @param entityType der jeweilige {@link EntityType}, darf nicht null sein
     * @return {@code true} if selected, {@code false} else.
     * @throws IllegalArgumentException falls der Typ nicht verwaltet wird
     */
    @Override
    public boolean isEntityTypeSelected(EntityType<?> entityType) {
        if (entityType == null)
            throw new NullPointerException("EntityType must not be null!");
        if (!selectionMap.containsKey(entityType))
            throw new IllegalArgumentException("EntityType is not in map!");
        else return selectionMap.get(entityType).get();
    }
    
    @Override
    public void setEntityTypeSelected(EntityType<?> entityType, boolean selected) {
        if (entityType == null)
            throw new NullPointerException("EntityType must not be null!");
        if (!selectionMap.containsKey(entityType))
            throw new IllegalArgumentException("EntityType is not in map!");
        selectionMap.get(entityType).set(selected);
    }
    
    /**
     * Liefert alle Entitätstypen, die grundsätzlich im Auswahl‐Objekt enthalten sind.
     *
     * @return Set aller verwalteten {@link EntityType}
     */
    @Override
    public Set<EntityType<?>> getEntityTypes() {
        return selectionMap.keySet();
    }
    
    /**
     * Erstellt den kompletten Prompt für die SQL‐Generierung, bestehend aus
     * dem benutzerdefinierten Text und den JSON‐Repräsentationen von Metamodel und Schema.
     *
     * @param prompt die menschlich formulierte Abfrage
     * @return vollständiger Prompt für den SQL‐Generator
     */
    @Override
    public String getFullPrompt(String prompt) {
        String context = "\nEine textuelle Repräsentation des JPA-Metamodels folgt, das dir den nötigen Kontext über das Domänenmodell, auf dem die Nutzeranfrage oben formuliert ist, gibt:\n\n" + getDomainString() + "\n\nNun folgt noch die textuelle Repräsentation des Datenbankschemas, das dir die nötigen Bezeichner der Tabellen, Spalten und weitere Informationen mit deren Hilfe du das SQL-Statement verfassen sollst, zur Verfügung stellt:\n\n" + getDatabaseString();
        return promptPrefix + prompt + promptPostfix + context;
    }
    
    /**
     * Prüft, ob alle Entitätstypen aktuell ausgewählt sind.
     *
     * @return true, falls alle ausgewählt sind; false sonst
     */
    @Override
    public boolean areAllSelected() {
        for (EntityType<?> et : selectionMap.keySet())
            if (!selectionMap.get(et).get()) return false;
        
        return true;
    }
    
    
    private String getDomainString() {
        if (domainString == null)
            domainString = domainStringGenerator.getDomainString(getSelectedEntityTypes());
        return domainString;
    }
    
    private String getDatabaseString() {
        if (databaseString == null)
            databaseString = databaseStringGenerator.getDatabaseString(getSelectedEntityTypes());
        return databaseString;
    }
    
    /**
     * Liefert die gerade ausgewählten Entitätstypen.
     *
     * @return Set der ausgewählten {@link EntityType}
     */
    @Override
    public Set<EntityType<?>> getSelectedEntityTypes() {
        return selectionMap.keySet().stream().filter(et -> selectionMap.get(et).get()).collect(Collectors.toSet());
    }
    
    /**
     * Markiert alle Entitätstypen als ausgewählt.
     */
    @Override
    public void selectAll() {
        selectionMap.keySet().forEach(et -> selectionMap.get(et).set(true));
    }
    
    /**
     * Hebt die Auswahl für alle Entitätstypen auf.
     */
    @Override
    public void unselectAll() {
        selectionMap.keySet().forEach(et -> selectionMap.get(et).set(false));
    }
}

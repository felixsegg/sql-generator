package entitytype;

import entitytype.generator.DatabaseStringGenerator;
import entitytype.generator.DomainStringGenerator;
import jakarta.persistence.metamodel.EntityType;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import service.ServiceSqlImpl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JavaFX-aware data structure for managing the selection state of a set of JPA entity types,
 * including configuration for prompt generation and string representation.
 *
 * <p>
 * Uses {@link BooleanProperty} for each entity type to enable property binding and change observation
 * in JavaFX-based user interfaces. Supports the same configuration and prompt assembly features
 * as {@link EntityTypeSelection}, adapted for UI interaction.
 * </p>
 *
 * <p>
 * Provides methods to select and deselect entity types, configure domain and database string generators,
 * and assemble prompts for SQL query generation that include both domain and database context.
 * </p>
 *
 * @author Felix Seggebäing
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
     * Creates a new selection object with all provided entity types initially selected,
     * and sets up observable properties for use in JavaFX user interfaces.
     *
     * @param entityTypes the set of JPA entity types to manage; all will be pre-selected
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
    
    /**
     * Sets the {@link DomainStringGenerator} to use for generating the domain string representation.
     * <p>
     * If not set, the default generator {@link #DEF_DOMAIN_STRING_GENERATOR} is used.
     * </p>
     *
     * @param domainStringGenerator the generator to use
     */
    @Override
    public void setDomainStringGenerator(DomainStringGenerator domainStringGenerator) {
        this.domainStringGenerator = domainStringGenerator;
    }
    
    /**
     * Sets the {@link DatabaseStringGenerator} to use for generating the database string representation.
     * <p>
     * If not set, the default generator {@link #DEF_DATABASE_STRING_GENERATOR} is used.
     * </p>
     *
     * @param databaseStringGenerator the generator to use
     */
    @Override
    public void setDatabaseStringGenerator(DatabaseStringGenerator databaseStringGenerator) {
        this.databaseStringGenerator = databaseStringGenerator;
    }
    
    /**
     * Sets the prefix and postfix used for assembling prompts in SQL query generation.
     * <p>
     * If not set, the defaults {@link #DEF_PROMPT_PREFIX} and {@link #DEF_PROMPT_POSTFIX} are used.
     * </p>
     *
     * @param promptPrefix  the prefix to use in the prompt
     * @param promptPostfix the postfix to use in the prompt
     */
    @Override
    public void setPromptPrePostfix(String promptPrefix, String promptPostfix) {
        this.promptPrefix = promptPrefix;
        this.promptPostfix = promptPostfix;
    }
    
    /**
     * Returns the {@link BooleanProperty} associated with the given entity type,
     * allowing selection state to be observed or modified in JavaFX.
     *
     * @param et the entity type for which the property is requested; must not be {@code null}
     * @return the {@link BooleanProperty} for the given entity type
     * @throws NullPointerException     if {@code et} is {@code null}
     * @throws IllegalArgumentException if {@code et} is not managed by this selection
     */
    public BooleanProperty getBooleanPropertyForEntityType(EntityType<?> et) {
        if (et == null)
            throw new NullPointerException();
        if (!selectionMap.containsKey(et))
            throw new IllegalArgumentException("Key is not in the map.");
        else return selectionMap.get(et);
    }
    
    /**
     * Checks whether the given JPA entity type is currently selected.
     *
     * @param entityType the entity type to check; must not be {@code null}
     * @return {@code true} if the entity type is selected, {@code false} otherwise
     * @throws NullPointerException     if {@code entityType} is {@code null}
     * @throws IllegalArgumentException if {@code entityType} is not managed by this selection
     */
    @Override
    public boolean isEntityTypeSelected(EntityType<?> entityType) {
        if (entityType == null)
            throw new NullPointerException("EntityType must not be null!");
        if (!selectionMap.containsKey(entityType))
            throw new IllegalArgumentException("EntityType is not in map!");
        else return selectionMap.get(entityType).get();
    }
    
    /**
     * Sets the selection state of the given JPA entity type.
     *
     * @param entityType the entity type to modify; must not be {@code null}
     * @param selected   {@code true} to select the entity type, {@code false} to deselect it
     * @throws NullPointerException     if {@code entityType} is {@code null}
     * @throws IllegalArgumentException if {@code entityType} is not managed by this selection
     */
    @Override
    public void setEntityTypeSelected(EntityType<?> entityType, boolean selected) {
        if (entityType == null)
            throw new NullPointerException("EntityType must not be null!");
        if (!selectionMap.containsKey(entityType))
            throw new IllegalArgumentException("EntityType is not in map!");
        selectionMap.get(entityType).set(selected);
    }
    
    /**
     * Returns all JPA entity types managed by this selection object, regardless of their selection state.
     *
     * @return a set of all managed entity types
     */
    @Override
    public Set<EntityType<?>> getEntityTypes() {
        return selectionMap.keySet();
    }
    
    /**
     * Assembles the full prompt for SQL query generation by combining the configured prefix, the user-defined prompt,
     * the postfix, the SQL dialect, the domain string, and the database string.
     *
     * <p>
     * The resulting prompt consists of: the prefix, the user-provided prompt, the postfix, a note on the current SQL dialect,
     * the domain string (from {@link DomainStringGenerator}), and the database string (from {@link DatabaseStringGenerator}).
     * </p>
     *
     * @param prompt the user-defined prompt content
     * @return the complete prompt string including all configured and generated parts
     */
    @Override
    public String getFullPrompt(String prompt) {
        String dynamicContext = "\nThe used SQL dialect is: " + ServiceSqlImpl.getInstance().getDialect() + ".\n\nA textual representation of the JPA metamodel follows, which provides you with the necessary context about the domain model on which the user query above is formulated:\n\n" + getDomainString() + "\n\nNow follows the textual representation of the database schema, which provides you with the necessary identifiers for the tables, columns, and other information you need to write the SQL statement:\n\n" + getDatabaseString();
        String staticContext = promptPrefix + prompt + promptPostfix;
        return staticContext + dynamicContext;
    }
    
    /**
     * Checks whether all managed entity types are currently selected.
     *
     * @return {@code true} if all entity types are selected, {@code false} otherwise
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
     * Returns the set of entity types that are currently selected.
     *
     * @return a set containing all currently selected entity types
     */
    @Override
    public Set<EntityType<?>> getSelectedEntityTypes() {
        return selectionMap.keySet().stream().filter(et -> selectionMap.get(et).get()).collect(Collectors.toSet());
    }
    
    /**
     * Selects all managed entity types.
     */
    @Override
    public void selectAll() {
        selectionMap.keySet().forEach(et -> selectionMap.get(et).set(true));
    }
    
    /**
     * Deselects all managed entity types.
     */
    @Override
    public void deselectAll() {
        selectionMap.keySet().forEach(et -> selectionMap.get(et).set(false));
    }
}

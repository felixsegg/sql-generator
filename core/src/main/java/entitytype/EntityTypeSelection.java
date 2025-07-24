package entitytype;

import entitytype.generator.DatabaseStringGenerator;
import entitytype.generator.DomainStringGenerator;
import entitytype.generator.impl.DatabaseStringGeneratorImpl;
import entitytype.generator.impl.DomainStringGeneratorImpl;
import jakarta.persistence.metamodel.EntityType;

import java.util.Set;

/**
 * Data structure for managing the selection state of JPA entity types and related configuration.
 *
 * <p>
 * Encapsulates methods for selecting and deselecting entity types, configuring prompt generators,
 * and handling prompt formatting used for SQL query generation based on JPA models.
 * </p>
 *
 * <p>
 * Allows storage and retrieval of selection states, prompt templates, and generator strategies.
 * </p>
 *
 * @author Felix Seggebäing
 */
public interface EntityTypeSelection {
    /**
     * Default {@link DomainStringGenerator} instance used for domain string generation.
     */
     DomainStringGenerator DEF_DOMAIN_STRING_GENERATOR = DomainStringGeneratorImpl.getInstance();
    
     /**
     * Default {@link DatabaseStringGenerator} instance used for database string generation.
     */
     DatabaseStringGenerator DEF_DATABASE_STRING_GENERATOR = DatabaseStringGeneratorImpl.getInstance();
    
    /**
     * Default prefix used for assembling prompts in SQL query generation.
     *
     * <p>
     * Introduces the user-defined prompt and provides instructions for generating an SQL SELECT statement
     * based on the JPA metamodel and its mapped SQL schema.
     * </p>
     */
     String DEF_PROMPT_PREFIX = """
             This is followed by a user-defined prompt, a structured string containing a JPA metamodel representation, and a string containing a SQL schema representation to which the JPA implementation maps the metamodel.
             Please generate an SQL SELECT statement that corresponds to the following query written in human language based on the object-oriented domain model.""";
    
    /**
     * Default postfix used for assembling prompts in SQL query generation.
     *
     * <p>
     * Contains instructions for formatting the generated SQL code and for restricting output to a pure SELECT statement
     * without any extra comments or markup.
     * </p>
     */
     String DEF_PROMPT_POSTFIX = """
            Please only output the relevant SQL code, nothing else, no further comments on your part. No Markdown elements to mark the output as SQL or similar.
            Please make sure that it is definitely a SELECT expression, even if the query requires otherwise.
            If appropriate, feel free to use nested query constructs, e.g., for quantifiers. But only if it is really appropriate, not unnecessarily.
            Please always set meaningful aliases, but only for fields if they are meaningful and increase readability.
            Please do not put a semicolon at the end.""";
    
    /**
     * Sets the {@link DomainStringGenerator} used for generating domain-specific string representations.
     *
     * @param domainStringGenerator the generator to use
     */
    void setDomainStringGenerator(DomainStringGenerator domainStringGenerator);
    
    /**
     * Sets the {@link DatabaseStringGenerator} used for generating database string representations.
     *
     * @param databaseStringGenerator the generator to use
     */
    void setDatabaseStringGenerator(DatabaseStringGenerator databaseStringGenerator);
    
    /**
     * Sets the prefix and postfix used for assembling prompts in SQL query generation.
     * <p>
     * For reference, see {@link #DEF_PROMPT_PREFIX} and {@link #DEF_PROMPT_POSTFIX}.
     * </p>
     *
     * @param promptPrefix  the prefix to use in the prompt
     * @param promptPostfix the postfix to use in the prompt
     */
    void setPromptPrePostfix(String promptPrefix, String promptPostfix);
    
    /**
     * Checks whether the given JPA entity type is currently selected.
     *
     * @param entityType the entity type to check
     * @return {@code true} if the entity type is selected, {@code false} otherwise
     */
    boolean isEntityTypeSelected(EntityType<?> entityType);
    
    /**
     * Sets the selection state of the given JPA entity type.
     *
     * @param entityType the entity type to modify
     * @param selected   {@code true} to select the entity type, {@code false} to deselect it
     */
    void setEntityTypeSelected(EntityType<?> entityType, boolean selected);
    
    /**
     * Returns all available JPA entity types managed by this selection.
     *
     * @return a set of all available entity types
     */
    Set<EntityType<?>> getEntityTypes();
    
    /**
     * Assembles the full prompt for SQL query generation by combining the configured prefix, the user-defined prompt,
     * the postfix, the domain string and the database string.
     *
     * <p>
     * The resulting prompt consists of: the prefix, the user-provided prompt, ths postfix, the domain string (from
     * {@link DomainStringGenerator}), and the database string (from {@link DatabaseStringGenerator}).
     * </p>
     *
     * @param prompt the user-defined prompt content
     * @return the complete prompt string including all configured and generated parts
     */
    String getFullPrompt(String prompt);
    
    /**
     * Checks whether all available entity types are currently selected.
     *
     * @return {@code true} if all entity types are selected, {@code false} otherwise
     */
    boolean areAllSelected();
    
    /**
     * Returns the set of currently selected JPA entity types.
     *
     * @return a set of selected entity types
     */
    Set<EntityType<?>> getSelectedEntityTypes();
    
    /**
     * Selects all available JPA entity types.
     */
    void selectAll();
    
    /**
     * Deselects all currently selected JPA entity types.
     */
    void deselectAll();
}

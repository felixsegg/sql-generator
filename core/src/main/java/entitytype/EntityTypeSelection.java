package entitytype;

import entitytype.generator.DatabaseStringGenerator;
import entitytype.generator.DomainStringGenerator;
import entitytype.generator.impl.DatabaseStringGeneratorImpl;
import entitytype.generator.impl.DomainStringGeneratorImpl;
import jakarta.persistence.metamodel.EntityType;

import java.util.Set;

public interface EntityTypeSelection {
    // Default values
     DomainStringGenerator DEF_DOMAIN_STRING_GENERATOR = DomainStringGeneratorImpl.getInstance();
     DatabaseStringGenerator DEF_DATABASE_STRING_GENERATOR = DatabaseStringGeneratorImpl.getInstance();
     String DEF_PROMPT_PREFIX = """
             This is followed by a user-defined prompt, a structured string containing a JPA metamodel representation, and a string containing a SQL schema representation to which the JPA implementation maps the metamodel.
             Please generate an SQL SELECT statement that corresponds to the following query written in human language based on the object-oriented domain model:
             \"""";
     String DEF_PROMPT_POSTFIX = """
            ".
            Please only output the relevant SQL code, nothing else, no further comments on your part. No Markdown elements to mark the output as SQL or similar.
            Please make sure that it is definitely a SELECT expression, even if the query requires otherwise.
            If appropriate, feel free to use nested query constructs, e.g., for quantifiers. But only if it is really appropriate, not unnecessarily.
            Please always set meaningful aliases, but only for fields if they are meaningful and increase readability.
            Please do not put a semicolon at the end.
            
            """;
    
    void setDomainStringGenerator(DomainStringGenerator domainStringGenerator);
    
    void setDatabaseStringGenerator(DatabaseStringGenerator databaseStringGenerator);
    
    void setPromptPrePostfix(String promptPrefix, String promptPostfix);
    
    boolean isEntityTypeSelected(EntityType<?> entityType);
    
    void setEntityTypeSelected(EntityType<?> entityType, boolean selected);
    
    Set<EntityType<?>> getEntityTypes();
    
    String getFullPrompt(String prompt);
    
    boolean areAllSelected();
    
    Set<EntityType<?>> getSelectedEntityTypes();
    
    void selectAll();
    
    void unselectAll();
}

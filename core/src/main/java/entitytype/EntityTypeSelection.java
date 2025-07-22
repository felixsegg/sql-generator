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
            Es folgen ein benutzerdefinierter Prompt, ein strukturierter String, der eine JPA-Metamodell-Repräsentation enthält und ein String, der ein PostgreSQL-Schema-Repräsentation auf die die JPA-Implementierung das Metamodell abbildet enthält.
            Generiere mir bitte ein SQL-SELECT-Statement, das der folgenden, auf dem objektorientierten Domänenmodell in menschlicher Sprache verfassten Abfrage entspricht:
            \"""";
     String DEF_PROMPT_POSTFIX = """
            ".
            Bitte gib ausschließlich den entsprechenden SQL-Code aus, sonst nichts, keine weiteren Kommentare deinerseits. Auch keine Markdownelemente, um den Output als SQL zu kennzeichnen oder Ähnliches.
            Achte bitte darauf, dass es sich garantiert um einen SELECT-Ausdruck handelt, auch wenn es in der Abfrage anders gefordert sein sollte.
            Wenn es angebracht ist, nutze gerne verschachtelte Abfragekonstrukte, z.B. für Quantoren. Aber nur wenn es wirklich angebracht ist, nicht unnötigerweise.
            Setze bitte immer sinnvolle Aliase, für Felder jedoch nur wenn diese sinnvoll sind und die Lesbarkeit erhöhen.
            Setze bitte kein Semikolon am Ende.
            
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

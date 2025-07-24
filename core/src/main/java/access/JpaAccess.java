package access;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Metamodel;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Central access point for JPA-related resources within the library.
 *
 * <p>
 * This class manages a {@link DataSource} and an {@link EntityManager}, providing
 * methods to access the underlying JDBC connection as well as the JPA metamodel.
 * It supports both Spring-based initialization (as a singleton bean via {@code @Component})
 * and manual initialization for non-Spring environments vis {@link #create(DataSource, EntityManager)}.
 * </p>
 *
 * <p>
 * The class is final and designed as a singleton; instances are managed internally via
 * the {@link JpaAccessHolder}.
 * </p>
 *
 * @author Felix Seggebäing
 */
@Component
public final class JpaAccess implements InitializingBean {
    
    private final DataSource dataSource;
    private final EntityManager entityManager;
    
    /**
     * Constructs a new {@code JpaAccess} instance with the given {@link DataSource} and {@link EntityManager}.
     *
     * <p>
     * Both arguments must be non-null. This constructor is typically used by the Spring framework
     * for dependency injection. If you do not intend to use the spring framework, please do not call 
     * this manually but use {@link #create(DataSource, EntityManager)} instead.
     * </p>
     *
     * @param dataSource     the source for obtaining JDBC connections; must not be {@code null}
     * @param entityManager  the entity manager providing access to the JPA metamodel; must not be {@code null}
     * @throws NullPointerException if either {@code dataSource} or {@code entityManager} is {@code null}
     */
    @Autowired
    public JpaAccess(DataSource dataSource, EntityManager entityManager) {
        if (dataSource == null || entityManager == null)
            throw new NullPointerException("Neither DataSource nor EntityManager may be null!");
        this.dataSource = dataSource;
        this.entityManager = entityManager;
    }
    
    /**
     * For spring after instantiation. Don't call this manually, use {@link #create(DataSource, EntityManager)} instead.
     */
    @Override
    public void afterPropertiesSet() {
        JpaAccessHolder.set(this);
    }
    
    /**
     * Returns a new JDBC {@link Connection} from the underlying {@link DataSource}.
     *
     * @return a new JDBC connection
     * @throws SQLException if a database access error occurs
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    /**
     * Returns the JPA {@link Metamodel} associated with the underlying {@link EntityManager}.
     *
     * @return the JPA metamodel
     */
    public Metamodel getMetamodel() {
        return entityManager.getMetamodel();
    }
    
    /**
     * Returns the JPA {@link EntityManager}.
     *
     * @return the JPA {@link EntityManager}
     */
    public EntityManager getEntityManager() {
        return entityManager;
    }
    
    /**
     * Users who do not use the spring framework or don't want to register this bean for some other reason must
     * call this method instead.
     *
     * @param dataSource    the source for the connections fetchable from the provider
     * @param entityManager the source of the metamodel fetchable from the provider
     */
    public static void create(DataSource dataSource, EntityManager entityManager) {
        JpaAccessHolder.set(new JpaAccess(dataSource, entityManager));
    }
}

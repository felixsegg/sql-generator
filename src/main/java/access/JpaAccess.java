package access;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Metamodel;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public final class JpaAccess implements InitializingBean {
    
    private final DataSource dataSource;
    private final EntityManager entityManager;
    
    @Autowired
    public JpaAccess(DataSource dataSource, EntityManager entityManager) {
        if (dataSource == null || entityManager == null)
            throw new NullPointerException("Neither DataSource nor EntityManager may be null!");
        this.dataSource = dataSource;
        this.entityManager = entityManager;
    }
    
    /**
     * For spring after instantiation. Don't call this manually, use {@code create()} instead.
     */
    @Override
    public void afterPropertiesSet() {
        JpaAccessHolder.set(this);
    }
    
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    public Metamodel getMetamodel() {
        return entityManager.getMetamodel();
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

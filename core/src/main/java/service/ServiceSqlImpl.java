package service;

import access.JpaAccessHolder;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class ServiceSqlImpl extends AbstractServiceSql {
    private static final ServiceSql instance = new ServiceSqlImpl();
    
    private ServiceSqlImpl() {
    }
    
    public static ServiceSql getInstance() {
        return instance;
    }
    
    @Override
    protected void testExecuteQuery(String sql) throws SQLException {
        try (Connection con = JpaAccessHolder.get().getConnection(); Statement statement = con.createStatement()) {
            statement.execute(sql);
        }
    }
    
    /**
     * Returns the SQL dialect currently used by the underlying Hibernate SessionFactory.
     * <p>
     * This method attempts to unwrap the Hibernate {@link Session} from the current
     * {@link EntityManager}, retrieve the {@link SessionFactoryImplementor}, and extract the
     * dialect information via {@code getJdbcServices().getDialect()}. The result is the fully
     * qualified class name of the Hibernate dialect in use (e.g., {@code org.hibernate.dialect.PostgreSQLDialect}).
     * </p>
     *
     * <p>
     * If Hibernate is not present, the dialect cannot be determined, or any exception occurs
     * during extraction, this method returns {@code "UNKNOWN"}.
     * </p>
     *
     * @return the Hibernate SQL dialect class name, or {@code "UNKNOWN"} if unavailable
     */
    @Override
    public String getDialect() {
        EntityManager em = JpaAccessHolder.get().getEntityManager();
        try {
            Session session = em.unwrap(Session.class);
            SessionFactoryImplementor sfi = (SessionFactoryImplementor) session.getSessionFactory();
            return sfi.getJdbcServices().getDialect().toString();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}

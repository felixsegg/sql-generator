package service;

import access.JpaAccessHolder;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class ServiceSqlImpl extends AbstractServiceSql {
    private static final ServiceSql instance = new ServiceSqlImpl();
    
    private ServiceSqlImpl() {}
    
    public static ServiceSql getInstance() {
        return instance;
    }
    
    @Override
    protected void testExecuteQuery(String sql) throws SQLException {
        try (Connection con = JpaAccessHolder.get().getConnection(); Statement statement = con.createStatement()) {
            statement.execute(sql);
        }
    }
}

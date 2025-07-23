package service;

import java.sql.SQLException;

public abstract class AbstractServiceSql implements ServiceSql {
    @Override
    public String getErrorsFor(String sql) {
        try {
            testExecuteQuery(sql);
        } catch (SQLException e) {
            StringBuilder messages = new StringBuilder();
            SQLException current = e;
            while (current != null) {
                messages.append(current.getMessage().replace("\n", " ")).append("\n");
                current = current.getNextException();
            }
            return messages.toString();
        }
        return "";
    }
    
    protected abstract void testExecuteQuery(String sql) throws SQLException;
    
}

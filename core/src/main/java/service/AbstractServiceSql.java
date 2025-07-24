package service;

import java.sql.SQLException;

/**
 * Abstract base class for {@link ServiceSql} implementations that validates SQL queries by executing them.
 *
 * <p>
 * Provides a default implementation of {@link #getErrorsFor(String)} that executes the SQL and collects error messages.
 * Concrete subclasses must implement {@link #testExecuteQuery(String)} to define how queries are executed.
 * </p>
 *
 * @author Felix Seggebäing
 */
public abstract class AbstractServiceSql implements ServiceSql {
    
    /**
     * Executes the given SQL query to test its validity.
     * <p>
     * If execution fails, this method should throw a {@link SQLException} containing the relevant error details.
     * </p>
     *
     * @param sql the SQL query to execute
     * @throws SQLException if an error occurs during execution
     */
    protected abstract void testExecuteQuery(String sql) throws SQLException;
    
    /**
     * Executes the given SQL query and returns any error messages if execution fails.
     * <p>
     * If execution is successful, returns an empty string. If execution fails, collects and returns
     * all error messages from the thrown {@link SQLException} and its chained exceptions,
     * separated by line breaks.
     * </p>
     *
     * @param sql the SQL query string to test
     * @return a string containing all error messages if execution fails, or an empty string if the query is valid
     */
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
}

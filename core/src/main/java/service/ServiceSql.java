package service;

/**
 * Service interface for SQL dialect information and query validation.
 *
 * <p>
 * Provides methods to retrieve the current SQL dialect, validate or analyze SQL queries,
 * and perform simple syntax checks on SQL code.
 * </p>
 *
 * @author Felix Seggebäing
 */
public interface ServiceSql {
    /**
     * Returns the name of the SQL dialect currently used.
     * <p>
     * If the dialect cannot be determined, returns {@code "UNKNOWN"}.
     * </p>
     *
     * @return the SQL dialect class name, or {@code "UNKNOWN"} if unavailable
     */
    String getDialect();
    
    /**
     * Executes the given SQL query and collects and returns error messages if
     * execution fails, therefore returns an empty string if execution is
     * successful.
     *
     * <p>
     * If errors occur, returns all error messages concatenated as a single string
     * (with each message on a separate line).
     * </p>
     *
     * @param sql the SQL query string to test
     * @return a string containing error messages if execution fails, or an empty string if the query is valid
     */
    String getErrorsFor(String sql);
    
    /**
     * Checks whether the given string is a valid SQL SELECT query according to simple syntactic rules.
     * <p>
     * Returns {@code true} if the string is at least 8 characters long, starts with "select "
     * or "select\n" (case-insensitive), and does not contain a semicolon.
     * </p>
     *
     * @param sql the SQL string to check
     * @return {@code true} if the string qualifies as a valid SELECT query, {@code false} otherwise
     */
    default boolean isQuery(String sql) {
        if (sql == null || sql.isBlank()) return false;
        boolean longEnough = sql.length() > 7;
        boolean validPrefix = (sql.substring(0, 7).equalsIgnoreCase("select ") || sql.substring(0, 7).equalsIgnoreCase("select\n"));
        boolean noSemicolon = !sql.contains(";");
        return longEnough && validPrefix && noSemicolon;
    }
}

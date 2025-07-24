package service;

public interface ServiceSql {
    String getDialect();
    
    String getErrorsFor(String sql);
    
    default boolean isQuery(String sql) {
        if (sql == null || sql.isBlank())
            return false;
        boolean longEnough = sql.length() >= 8;
        boolean validPrefix = (sql.substring(0, 7).equalsIgnoreCase("select ") || sql.substring(0, 7).equalsIgnoreCase("select\n"));
        boolean noSemicolon = !sql.contains(";");
        return longEnough && validPrefix && noSemicolon;
    }
}

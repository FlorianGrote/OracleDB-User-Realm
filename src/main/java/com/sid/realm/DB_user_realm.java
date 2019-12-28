package com.sid.realm;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import org.apache.catalina.realm.RealmBase;
import org.apache.catalina.realm.GenericPrincipal;
import java.util.logging.Logger;

import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.Properties;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.pool.OracleDataSource;

/**
 *
 * @author Florian Grote
 * @email  Floran.Grote@lobbe.de
 */
public class DB_user_realm extends RealmBase {
    private static final Logger log = Logger.getLogger(DB_user_realm.class.getName());
    private String username;
    private String password;
    private String DBUrl;
    private String DBUserColumn = "USERNAME";
    private String DBRoleColumn = "granted_role";
    private String DBRoleTable = "USER_ROLE_PRIVS";
    private String RoleIncludes = "";
    private String RoleExcludes = "";
    private List<String> UserRoles = new ArrayList<String>();

    private void reset() {
        this.setAllRolesMode("strict");
        UserRoles = new ArrayList<String>();
        this.username = null;
        this.password = null;
    }

    @Override
    public Principal authenticate(String username, String credentials) {
        this.reset();
        this.username = username;
        this.password = credentials;
        try {
            return this.connectProxy();
        }
        catch(SQLException e) {
            return null;
        }
    }
    @Override
    protected String getName() {
        return username;
    }

    @Override
    protected String getPassword(String username) {
        return password;
    }

    @Override
    protected Principal getPrincipal(String string) {
        Principal principal = new GenericPrincipal(this.username, this.password, UserRoles);
        return principal;
    }

    private Principal connectProxy() throws SQLException {
        Properties info = new Properties();
        // Hier wird der Benutzer als Connection gesetzt)
        info.put(OracleConnection.CONNECTION_PROPERTY_USER_NAME, this.username);
        info.put(OracleConnection.CONNECTION_PROPERTY_PASSWORD, this.password); 

        OracleDataSource ods = new OracleDataSource();
        ods.setURL( this.getDBUrl() );    
        ods.setConnectionProperties(info);

        try (OracleConnection connection = (OracleConnection) ods.getConnection()) {
            Statement statement = connection.createStatement();

            // Name ist Case insensitive
            try (ResultSet resultSet = statement.executeQuery(
                "select " + this.getDBRoleColumn() + 
                " from " + this.getDBRoleTable() + " where 1=1" +
                " and lower(" + this.DBUserColumn + ") = '" + this.getName().toLowerCase() + "'"
                + this.getRoleIncludes()
                + this.getRoleExcludes() 
                )) {
                    while (resultSet.next() ) { /* Moves forward */
                        UserRoles.add(resultSet.getString(1) ); // case Sensitive
                        log.fine("Authentication is taking place for the user: " + this.username + " Role(1): " + resultSet.getString(1) );
                    }
                    connection.close();
                    if (UserRoles.isEmpty()) {
                        log.fine("Authentication is taking place for the user: " + this.username + " No Roles found: " + UserRoles.isEmpty());
                        return null;
                    } 
                    return getPrincipal( this.getName() );
            }
            catch(SQLException e2) {
                connection.close();
                log.fine("SQL ERROR: " + e2.getMessage() );
                return null;
                // throw e2;
            }
        }
        catch(SQLException e) {
            log.fine("SQL ERROR: " + e.getMessage() );
            return null;
            // throw e;
        }
    }

    /* Custom Properties */
    public String getDBUrl() {
        return this.DBUrl;
    }
    public void setDBUrl(String DBUrl) {
        this.DBUrl = DBUrl;
    }

    public String getDBRoleTable() {
        return this.DBRoleTable;
    }
    public void setDBRoleTable(String DBRoleTable) {
        this.DBRoleTable = DBRoleTable;
    }

    public String getDBRoleColumn() {
        return this.DBRoleColumn;
    }
    public void setDBRoleColumn(String DBRoleColumn) {
        this.DBRoleColumn = DBRoleColumn;
    }

    public String getDBUserColumn() {
        return this.DBRoleColumn;
    }
    public void setDBUserColumn(String DBUserColumn) {
        this.DBUserColumn = DBUserColumn;
    }

    public String getRoleIncludes() {
        return this.RoleIncludes;
    }
    public void setRoleIncludes(String RoleIncludes) {
        this.RoleIncludes =" and " + this.getDBRoleColumn() + " in ("  + RoleIncludes + ")" ;
    }

    public String getRoleExcludes() {
        return this.RoleExcludes;
    }
    public void setRoleExcludes(String RoleExcludes) {
        this.RoleExcludes = " and " + this.getDBRoleColumn() + " not in ("  + RoleExcludes + ")";
    }
}

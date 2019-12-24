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
    private String DBUser; /* Proxy Datebank Account */
    private String DBPassword; /* Proxy Datanebank account */
    // private String DBRole;  /* Manuelle DBRollen vergabe */
    private String DBUrl;
    private String DBUserColumn = "USERNAME";
    private String DBRoleColumn = "granted_role";
    private String DBRoleTable = "USER_ROLE_PRIVS";
    private String RoleIncludes = "";
    private String RoleExcludes = "";
    private List<String> UserRoles = new ArrayList<String>();

    @Override
    public Principal authenticate(String username, String credentials) {
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
        // Setzt die Rollen manuel
        /*
        List<String> roles = new ArrayList<String>();
        roles.add( role );
        Principal principal = new GenericPrincipal(this.username, this.password, roles);
        */
        Principal principal = new GenericPrincipal(this.username, this.password, UserRoles);
        return principal;
    }

    private Principal connectProxy() throws SQLException {
        Properties info = new Properties();
        // Hier wird der Benutzer als Connection gesetzt)
        info.put(OracleConnection.CONNECTION_PROPERTY_USER_NAME, this.username /*this.getDBUser()*/ );
        info.put(OracleConnection.CONNECTION_PROPERTY_PASSWORD, this.password /*this.getDBPassword()*/ ); 

        // Man könnte weiterhin eine Proxy Connection nutzen um die Rechte einzuschränken?!
        // Das braucht es nicht mehr
        /*
        Properties prop = new Properties();
        prop.put(OracleConnection.PROXY_USER_NAME, this.username); 
        prop.put(OracleConnection.PROXY_USER_PASSWORD, this.password); 
        String[] roles = {this.getDBRole() };
        prop.put(OracleConnection.PROXY_ROLES, roles);
        */
        // Bis hier hin

        OracleDataSource ods = new OracleDataSource();
        ods.setURL( this.getDBUrl() );    
        ods.setConnectionProperties(info);

        try (OracleConnection connection = (OracleConnection) ods.getConnection()) {
            // Man könnte weiterhin eine Proxy Connection nutzen um die Rechte einzuschränken?!
            // Proxy connection braucht es nicht mehr
            /*
            connection.openProxySession(OracleConnection.PROXYTYPE_USER_NAME, prop); 
            if (connection.isProxySession() ) {
                log.info("Authentication is taking place for the user: " + this.username);
                connection.close(OracleConnection.PROXY_SESSION);
                connection.close();    
                return getPrincipal(this.username);
            } else {
                return null;
            }
            */

            //Stattdessen ein SQL um die Rollen abzufragen und diese in den Principle setzen.
            Statement statement = connection.createStatement();

            // Name ist Case insensitive
            try (ResultSet resultSet = statement.executeQuery( "select " + this.getDBRoleColumn() + 
                " from " + this.getDBRoleTable() + " where 1=1 " +
                " and lower(" + this.DBUserColumn + ") = \"" + this.getName().toLowerCase() + "\""
                + this.getRoleIncludes()
                + this.getRoleExcludes() ) ) {
                while (resultSet.next() ) { /* Moves forward */
                    UserRoles.add(resultSet.getString(1) ); // case Sensitive
                    log.finest("Authentication is taking place for the user: " + this.username + " Role(1): " + resultSet.getString(1) );
                }
                connection.close();
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
    public String getDBUser() {
        return this.DBUser;
    }
    public void setDBUser(String DBUser) {
        this.DBUser = DBUser;
    }

    public String getDBPassword() {
        return this.DBPassword;
    }
    public void setDBPassword(String DBPassword) {
        this.DBPassword = DBPassword;
    }

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

    // Nur Nötig bei fester Rollen vergabe
    /*
    public String getDBRole() {
        return this.DBRole;
    }
    public void setDBRole(String DBRole) {
        this.DBRole = DBRole;
    }
    */
}

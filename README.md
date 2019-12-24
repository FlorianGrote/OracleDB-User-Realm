# A Custom OracleDB Database User realm implementation in tomcat

Follow the instruction below:

1) Download this src code and run "mvn clean install" 
2) Place tomcatRealm-1.0-SNAPSHOT.jar at $CATALINA_HOME/lib.
3) Add Realm to Server.xml oor any Context.xml
4) Start tomcat "catalina.sh run"

For successful authentication you just need to Add the DBUrl Parameter. The DBRoleColumn name. And the DBRoleTable name.
You can optionally Whitelist with RoleInclude Parameter.
You can optionally Blacklist with RoleExclude Parameter

Example Realm configuration


```xml
<Realm className="com.sid.realm.DB_user_realm"
    DBRoleColumn="granted_role"
    DBRoleTable="USER_ROLE_PRIVS"
    DBUrl="jdbc:oracle:thin:<DB_USER_NAME>/DB_USER_PWASSWORD>@localhost:1521:xe" 
/>
```
The USER_ROLE_PRIVS are the granted Database User priviliges and roles
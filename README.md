# Etendo
This is the development repository of Etendo ERP. <br>
Etendo is an ERP to manage the business flows, adaptable to the needs of the companies. Our mission is creating an adaptable, flexible, composable and scalable software, able to grows without restrictions from the start and offers an intuitive, customizable and complete solution for the companies.
We are developing an international ERP and a platform that supports business development and scalable growth for our partners and their customers.

To more information visit [etendo.software](https://etendo.software)

### Requirements
In this section you can read the [System Requirements](https://docs.etendo.software/en/technical-documentation/etendo-environment/requirements-and-tools/requirements).

### Etendo Core Instalation

1. To compile and deploy an Etendo Core instance you have to setup the configuration variables, to do that you have to create a copy of `gradle.properties.template` file.
```bash
cp gradle.properties.template gradle.properties
```
2. You can edit `gradle.properties` file updating the variables or use the default values

> To configure GitHub credentials read [Using repositories on Etendo](https://docs.etendo.software/en/technical-documentation/etendo-environment/requirements-and-tools/developer-tools/use-of-repositories-in-etendo)

| Variable                     | Description                                                               | Default value |
|------------------------------|---------------------------------------------------------------------------|---------------|
| githubUser <br> gitHubToken  | GitHub repository credentials (required)                                  |               |
| nexusUser <br> nexusPassword | Nexus repository credentials, for access to commercial modules (optional) |               |
| context.name                 | Environment name                                                          | etendo        |
| bbdd.sid                     | Database name                                                             | etendo        |
| bbdd.port                    | Database port                                                             | 5432          | 
| bbdd.systemUser              | Database system user                                                      | postgres      |
| bbdd.systemPassword          | Database system password                                                  | syspass       |
| bbdd.user                    | Database user                                                             | tad           |
| bbdd.password                | Database password                                                         | tad           |

> Run the gradle tasks with the `--info` or `--debug` flag to log more information.

3. Run setup task to create the configurations files
```
./gradlew setup
```
4. Execute install task to create the initial database and compile the sources
```
./gradlew install
```
5. Deploy Etendo to Tomcat:
```
./gradlew smartbuild
```
This task deploying the webContent folder into the tomcat/webapps directory, for that you must set up $CATALINA_HOME environment variable.

6. Finally, run the tomcat and access to [https://localhost:8080/etendo](https://localhost:8080/etendo)

### Documentation
For  more information you can read documentation in [Etendo Docs](https://docs.etendo.software) .

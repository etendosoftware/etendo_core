# Etendo
This is the development repository of Etendo ERP. <br>
Etendo is an ERP to manage the business flows, adaptable to the needs of the companies. Our mission is creating an adaptable, flexible, composable and scalable software, able to grows without restrictions from the start and offers an intuitive, customizable and complete solution for the companies.
We are developing an international ERP and a platform that supports business development and scalable growth for our partners and their customers.

To more information visit [etendo.software](https://etendo.software)

### Requirements
In this section you can read the [System Requirements](https://docs.etendo.software/getting-started/requirements/).

### Etendo Core Instalation

1. To compile and deploy an Etendo Core instance you have to setup the configuration variables, to do that you have to create a copy of `gradle.properties.template` file.
```bash
cp gradle.properties.template gradle.properties
```
2. You can edit `gradle.properties` file updating the variables or use the default values

> To configure GitHub credentials read [Using repositories on Etendo](https://docs.etendo.software/developer-guide/etendo-classic/getting-started/installation/use-of-repositories-in-etendo/)

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

### License

This project includes components licensed under different terms. For full license texts and third-party license disclosures, please refer to the `/legal` directory:

- **Etendo License**: See [legal/Etendo_licence.txt]( ./legal/Etendo_licence.txt) or [legal/Etendo_licence_es.txt](./legal/Etendo_licence_es.txt)
- **Openbravo Public License (OBPL)**: See [legal/Openbravo_license.txt](./legal/Openbravo_license.txt)
- **Third-party dependency licenses**: See [legal/LEGAL.md](./legal/LEGAL.md)



For any legal or licensing inquiries, please contact:

FUTIT SERVICES, S.L.
Calle Eustasio Amilibia, 10 - piso 7 pta 420011 <br>
Donostia - San Sebastián, Spain  <br>
legal@etendo.software
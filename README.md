Maven Hibernate Plugin
======================

About
-----

This plugin exports/validates Hibernate schema as part of a Maven build process.

Features
--------

* Hibernate 4 support
* Support for multiple cfg.xml
* Mapped classes and hbm.xml can be specified in the cfg.xml and/or as plugin properties
* Hibernate properties can be supplied by one or more .properties files and/or as plugin properties
* Full schema validation including column types, foreign keys, and indices.

Usage
-----

Specifying the Hibernate configuration is fairly straightforward (see below). 

```xml
<plugin>
  <groupId>com.vecna</groupId>
  <artifactId>hibernate-schema-plugin</artifactId>
  <configuration>
    <configFiles>
      <configFile>module1.cfg.xml</configFile>
      <configFile>module2.cfg.xml</configFile>
    </configFiles>
    <additionalMappings>
      <additionalMapping>MyClass.hbm.xml</additionalMapping>
    </additionalMappings>
    <additionalClasses>
      <additionalMapping>org.package.MyAnnotatedClass</additionalMapping>
    </additionalClasses>
    <properties>
      <hibernate.connection.url>jdbc:postgresql://localhost/mydb</hibernate.connection.url>
    </properties>
  </configuration>
  <dependencies>
    <dependency>
      <groupId>postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <version>8.4</version>
    </dependency>
  </dependencies>
</plugin>
```

Key points are:
* Specify connection parameters (inline in the POM or via one or more .properties files) and dialect
* Add the JDBC driver as a plugin dependency (if it's not a project dependency)

Since version 2, this plugin works with Hibernate 4. For Hibernate 3 support, use versions 1.x.

Goals
-----

* __export:__ export the Hibernate schema into a file and/or a live database
* __validate:__ validate the Hibernate configuration against a live database
* __update:__ generate update scripts (this goal is very basic - it runs Hibernate's SchemaExport and only handles new columns)
* __doc:__ generate schema documentation from javadocs


Credits
-------

Originally developed by [Vecna Technologies, Inc.](http://http://www.vecna.com/) and open sourced as part of its community service program. See the LICENSE file for more details.
Vecna Technologies encourages employees to give 10% of their paid working time to community service projects. 
To learn more about Vecna Technologies, its products and community service programs, please visit http://www.vecna.com.

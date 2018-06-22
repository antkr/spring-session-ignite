# Spring Session Ignite

[Spring Session](https://github.com/spring-projects/spring-session) Extension
--------------------------------

Spring Session Ignite is a [Spring Session](https://github.com/spring-projects/spring-session) extension that uses [Apache Ignite](https://ignite.apache.org/) for session storage and replication.

## How to build
--------------------------------
* Clone this repository
* Run ``` ./gradlew clean install ``` in the project folder to build the project and install it to local Maven repo.

## Add `spring-session-ignite` as a dependency to your project using:

##### Maven
```xml
<dependency>
    <groupId>com.apache.ignite</groupId>
    <artifactId>spring-session-ignite</artifactId>
    <version>1.0</version>
</dependency>
```

##### Gradle
```groovy
compile group: 'org.apache.ignite', name: 'spring-session-ignite', version: '1.0'
```



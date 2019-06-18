# Building from the source

1) Install [Oracle Java SE Development Kit (JDK) version 1.8*](http://java.sun.com/javase/downloads/index.jsp) 
and set the JAVA_HOME environment variable.

2) Install latest version of [Maven](https://maven.apache.org/install.html) if you don't have it installed already.

3) Clone the siddi-sdk using the following command.
```
git clone https://github.com/wso2/siddhi-sdk.git
```

4) Navigate to the cloned repo using the following command.
```
cd siddhi-sdk
```

5) Build the siddhi-sdk using the following command.
```
mvn clean install
```

6) Now the Siddhi SDK zip file (ex: siddhi-sdk-4.0.0-SNAPSHOT.zip) will be created inside the 
{siddhi-sdk-home}/modules/siddhi-launcher/target/ . Unzip it.
 

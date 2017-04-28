# Toolkit

Scala Version of Exocute Platform

-------------------------------------------------------------------------------
Building highly concurrent software systems such as those on, grids, clusters, and groups of multicore processing systems, is difficult. As systems tend towards lower clock speeds and greater parallelism, the need to distribute and coordinate work on these systems becomes more of a pressing need.<br />
Exocute is designed to make the job of distributing, coordinating and executing computer software over an arbitrary number of computing nodes simple. We call these processing elements Activities. These can be written in either Java/Scala or a ‘host’ programming language such as C or Fortran. In this introduction we give examples in Scala. <br />
If you have a program written in a ‘host’ language and you don’t know Java/Scala, then a better place to start would be the host bindings information, or talking to the team about the specific host requirements. <br />
We assume only a level of Java skill required to implement your particular application and not any knowledge of distributed Java/Scala programming, or distributed programming in general.<br />

For more information about the Toolkit: http://bit.ly/2mitGuG

## Releases

28/04/2017 <br />
--> **Toolkit v1.2** <br />

10/04/2017 <br />
--> **Toolkit v1.1** <br />

07/03/2017 <br />
--> **Toolkit v1.0** <br />

How to use? <br />
To launch the Toolkit app run StartClientAPI.jar
  ```
java -jar StartClientAPI.jar -jarfile file_name[.jar] [options] file_name[.grp]
```
Options available: <br />



| Option                           | Meaning                                 |
| ---                              | ---                                     |
| `-s`                             | sets signal space                       |
| `-d  `                           | sets data space                         |
| `-j `                            | sets jar space                          |
| `-jarfile file_name[.jar] `       | Sets the jar file containing all classes that will be used in the grp         |
| `-cleanspaces`                   | cleans all the entries form every space |
| `--help    `                     | display help and exit                   |
| `--version`                      | ouput version information and exit      |


After started:<br />

Available commands:<br />

| Command               |               | 
| -----------------     |:-------------:|
| i {input}             | injects {input} as a string into the data space. |
| im {n} {input}        | injects {input} as a string {n} times into the data space. |
| n {input}             | injects {input} as a number (Long) into the data space. |
| file {file_name}      | injects the bytes of the file {file_name} into the data space. |
| filen {n} {file_name} | injects n times the bytes of the file {file_name} into the data space. |
| c                     | collects 1 result |  
| c {n}                 | collects at most n results |  

To launch the LogProcessor app run LogProcessor.jar

  ```
java -jar LogProcessor.jar [signalSpaceIP]
```
  
  

-------------------------------------------------------------------------------

## Files

### GRP Files Structure
Graph {name}<br />
[Import {param}]<br />
[Export {param}]<br />

Activity ID {name}:{params}   
[Import {param}]              
[Export {param}]              

Connection from->to : from->to_1,to_2 

Notes: <br />
1) Connection can be set using the word connection multiple times or separating the connection with ':'<br />
2) ID's should be unique<br />
3) The construction of the graph should be correct. Every graph should have a source and sink<br />

-------------------------------------------------------------------------------

## Specs

###Versions and Programs used 

 
| Software       | Version        | Link /sbt                              |
| ---------------|:--------------:| --------------------------------------:|
| Scala          | 2.12.1         |                                        |
| Java           | 1.8            | http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html   |
| FlyObjectSpace | 2.2.0-SNAPSHOT | https://github.com/fly-object-space/fly-scala   |
| Parboiled      | 2.1.4          | libraryDependencies += "org.parboiled" %% "parboiled" % "2.1.3"   |
| ScalaTest      | 3.0.1          | libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"   |
| Swave          | 0.7.0          | https://github.com/sirthias/swave
| ExoNode        | 1.2            | https://github.com/exocute/ExoNode   |

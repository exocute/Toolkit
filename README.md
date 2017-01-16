# Toolkit

Scala Version of Exocute Plataform 

-------------------------------------------------------------------------------

## News 

26/12/2016 <br />
DEPRECATED <br />
--> Exocute API V1 BETA - Works for simple sequential graphs with random number of activities<br />
--> Allows connections to multiple servers<br />
--> Uses Streams to calculate results<br />


16/01/2017 <br />
--> Exocute API - Works for all types of graphs<br />
--> Allows connections to multiple servers<br />
--> Allows users to launch a grp file into the space <br />

Usage <br />

```
  toolkit [options] file_name[.grp]
```

options:<br />
-j, -jar ip<br />
  Sets the jarHost.<br />
-s, -signal ip<br />
  Sets the signalHost.<br />
-d, -data ip<br />
  Sets the dataHost.<br />
-jarfile file_name[.jar]<br />
  Sets the jar file containing all classes that will be used in the grp.<br />
-cleanspaces<br />
  Clean all information from the spaces<br />
--help<br />
  display this help and exit.<br />
--version<br />
  output version information and exit.<br />
  
After started:<br />

Available commands:<br />
i <input>        -> injects <input> as a string into the data space.<br />
im <n> <input>   -> injects <input> as a string <n> times into the data space.<br />
n <input>        -> injects <input> as a number (Long) into the data space.<br />
c                -> collects 1 result.<br />
c <n>            -> collects at most n results.<br />


  
  

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

 
| Software       | Version       | Link                                   |
| ---------------|:-------------:| --------------------------------------:|
| Scala          | 12.0.2        |                                        |
| Java           | 1.8           |                                        |
| FlyObjectSpace | 2.0.2      |  https://github.com/fly-object-space   |


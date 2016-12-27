# Toolkit

Scala Version of Exocute Plataform 

-------------------------------------------------------------------------------

## News 

26/12/2016 <br />
--> Exocute API V1 BETA - Works for simple sequential graphs with random number of activities<br />
--> Allows connections to multiple servers<br />
--> Uses Streams to calculate results<br />

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


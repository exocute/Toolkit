# Toolkit

Scala Version of Exocute Plataform 

-------------------------------------------------------------------------------

## News 

26/12/2016 --> Exocute API V1 BETA - Works for simple sequential graphs with random number of activities
           --> Allows connections to multiple servers
           --> Uses Streams to calculate results

-------------------------------------------------------------------------------

## Files

### GRP Files Structure
Graph {name}<br />
[Import {param}]<br />
[Export {param}]<br />

Activity ID {name}:{params}    |<br />
[Import {param}]               |   one or more activities<br />
[Export {param}]               |<br />

Connection from->to : from->to_1,to_2  | zero or more<br />

Notes: 
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


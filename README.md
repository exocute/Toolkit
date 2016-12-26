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
Graph <name>
[Import <param>]
[Export <param>]

Activity ID <name>:<params>    |
[Import <param>]               |   one or more activities
[Export <param>]               |

Connection from->to : from->to_1,to_2  | zero or more

Notes: 
1) Connection can be set using the word connection multiple times or separating the connection with ':'
2) ID's should be unique
3) The construction of the graph should be correct. Every graph should have a source and sink

-------------------------------------------------------------------------------

## Specs

###Versions and Programs used 

 
| Software       | Version       | Link                                   |
| ---------------|:-------------:| --------------------------------------:|
| Scala          | 12.0.2        |                                        |
| Java           | 1.8           |                                        |
| FlyObjectSpace | 2.0.2      |  https://github.com/fly-object-space   |


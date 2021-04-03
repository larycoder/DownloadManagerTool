# Purpose
This is simple tool to download data from multiple node

# Philosophy
- Simple in idea (RMI oriented)
- No complex structure
- Easy to add new data node to system
- Client heavy process tools

# Design
## Server
- NameNode: control DataNode + clean dead DataNode (time heartbeat: heartbeat - 2s and after 3 times ~ 6s --> node is dead)
- DataNode: receive request (upload/download chunk/full file)

## Client
- Get list of DataNode from NameNode
- Upload case: open multiple thread do same thing - upload file to all DataNode
- Download case: open multiple thread for downloading each chunk file to temp dir, then merge together into single file

## Protocol:

1. DataNode:
```
##### upload protocol #####
r: action \n
r: filename \n
r: size \n

-- repeat --
s: 200 \n
r: {data - chunk: 5000}
------------

s: 201 \n

##### download protocol #####
r: action \n
r: filename \n
r: offset \n
r: len \n
s: 200 \n

-- repeat --
r: 200 \n
s: {data - chunk: 5000}
------------

```

2. ClientNode:

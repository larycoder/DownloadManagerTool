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
s: 200 \n
s: filesize \n
r: [100 \n -- continues process | 200 \n -- ok now stop process]
IF(200) s: 200 \n
r: offset \n
r: size \n
s: 200 \n

-- repeat --
r: 200 \n
s: {data - chunk: 5000}
------------

```

2. Note:
For Client side:
- Upload: client open multi-thread to upload at same time to all nodes
- Download: since each DataNode could have different data for single filename --> get first getted DataNode as truth ground to compare another DataNode


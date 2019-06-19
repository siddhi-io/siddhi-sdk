# Running a Siddhi file using command line

### For Windows Operating System

1) Navigate to the bin folder located inside the unzipped Siddhi SDK folder using the following command.
```
cd "{siddhi-sdk-home}\bin"
```
* {siddhi-sdk-home} is the path to the siddhi-sdk. ex:C:\siddhi-sdk-5.0.0-SNAPSHOT

2) Run the Siddhi file using the following command.
```
siddhi.bat run "<path for the siddhi file>" "<path for the event input file>"  
```
Ex: siddhi.bat run "C:\TestSiddhi\Test.siddhi" "C:\TestSiddhi\eventInput.txt"  

*NOTE: If you are getting events through a siddhi extension, then you do not need to provide a path for the event input 
file.   

### For Unix-based Operating System (Linux, Solaris and Mac OS X) 

1) Navigate to the bin folder located inside the unzipped Siddhi SDK folder using the following command.
```
cd "{siddhi-sdk-home}/bin"
```
* {siddhi-sdk-home} is the path to the siddhi-sdk. ex:/home/username/siddhi-sdk-5.0.0-SNAPSHOT

2) Run the Siddhi file using the following command.
```
./siddhi run "<path for the siddhi file>" "<path for the event input file>"  
```
Ex: siddhi.bat run "C:\TestSiddhi\Test.siddhi" "C:\TestSiddhi\eventInput.txt"  

*NOTE: If you are getting events through a siddhi extension, then you do not need to provide a path for the event input 
file.

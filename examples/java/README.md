### Java Usage Example

The provided Java class provides several support methods for using the API. This is mainly intented to showcase the usage of the .jar.
The class provides examples for:
* Read feature models in UVL format
* Store feature models in UVL format
* Parse error messages for faulty models
* Use an UVLModel instance with regards to:
  * Get features
  * Add features
  * Read attributes of a feature

If run, the class reads `example.uvl`, adds a feature, and prints the resulting model to Server.uvl.
Furthermore, it prints the error found in `faulty.uvl`.
To compile and use the class, the parser library is required as .jar.
The class can be compiled and used with the following commands (after the building the jar following the given instructions):

*Compile:*`javac -cp ../../target/uvl-parser-0.1.0-SNAPSHOT-standalone.jar UVLExample.java`

*Run:* `java -cp .:../../target/uvl-parser-0.1.0-SNAPSHOT-standalone.jar UVLExample`

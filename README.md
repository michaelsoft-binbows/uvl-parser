# UVL - Universal Variability Language

This is a small default library used to parse and print the Universal Variability Language (UVL).
It is written in Clojure, but can also be used from any JVM language, as it exposes a Java API.

Under the hood it uses [instaparse](https://github.com/Engelberg/instaparse) as the parsing library.
The grammar in EBNF form is located in `resources/uvl.bnf`

## The Language

On a high level, each feature model in UVL consists of four optional separated elements:

1. A namespace which can be used for references
2. A list of imports that can be used to reference external feature models
3. The tree hierarchy consisting of: features, group types, and attributes whose relations are specified using nesting (indendation)
4. Cross-tree constraints



For our tool, we specified the language as context-free grammar (CFG) in EBNF notation as seen below.
```
FeatureModel = Ns? Imports? Features? Constraints?

Ns = <'namespace'> REF
Imports = <'imports'> (<indent> Import+ <dedent>)?
Import = REF (<'as'> ID)? (<'refer'> Refer)?
Refer = (<'['> (ID <','?>)* <']'>) | 'all'

Features = <'features'> Children?
<Children> = <indent> FeatureSpec+ <dedent>
FeatureSpec = REF Attributes? Groups?
Attributes = (<'{'> <'}'>) |
             (<'{'> Attribute (<','> Attribute)* <'}'>)
Attribute = Key Value?
Key = ID
Value = Boolean|Number|String|Attributes|Vector|Constraint
Boolean = 'true' | 'false'
Number = #'[+-]?(0|[1-9]\d*)(\.\d*)?([eE][+-]?\d+)?'
String = #'"(?:[^"\\\n]|\\.)*"'
Vector = <'['> (Value <','?>)* <']'>
Groups = <indent> Group* <dedent>
Group = ('or' | 'alternative' | 'mandatory' | 
            'optional' | Cardinality)
Children?
Cardinality = <'['> (int <'..'>)? (int|'*') <']'>

Constraints = <'constraints'> (<indent> Constraint+ <dedent>)?
<Constraint> = disj-impl | Equiv
Equiv = Constraint <'<=>'> disj-impl
<disj-impl> = disj | Impl
Impl = disj-impl <'=>'> disj
<disj> = conj | Or
Or = disj <'|'> conj
<conj> = term-not | And
And = conj <'&'> term-not
<term-not> = term | Not
Not = <'!'> term
<term> = REF | <'('> Constraint <')'>

indent = '_INDENT_'
dedent = '_DEDENT_'
<strictID> = #'(?!alternative|or|features|constraints|
true|false|as|refer)[a-zA-Z][a-zA-Z_0-9]*'
<ID> = #'(?!true|false)[a-zA-Z][a-zA-Z_0-9]*'
REF = (ID <'.'>)* strictID
<int> = #'0|[1-9]\d*'
```

The following snippet shows a simplified server architecture in UVL. We provide more examples (e.g., to show the composition mechanism) in [examples/language](https://github.com/Universal-Variability-Language/uvl-parser/tree/master/examples/language).

```
features
  Server {abstract}
    mandatory
      FileSystem
        or // with cardinality: [1..*]
          NTFS
          APFS
          EXT4
      OperatingSystem {abstract}
        alternative
          Windows
          macOS
          Debian
    optional
      Logging	{
      default,
      log_level "warn" // Feature Attribute
    }

constraints
  Windows => NTFS
  macOS => APFS
```

In this snippet, we can recognize the following elements:
* The feature `Server` is abstract (i.e., corresponds to no implementation artifact.
* Each `Server` requires a `FileSystem`and an `OperatingSystem` denoted by the *mandatory* group
* The `Server` may have `Logging` denoted by the *optional* group
* A `FileSystem` requires at least one type of `NTFS`, `APFS`, and `Ext4` denoted by the *or* group
* An `OperatingSystem` has exactly one type of `Windows`, `macOS`, and `Debian`denoted by the *alternative* group
* `Logging` has the feature attribute `log_level` attached which is set to "warn"
* `Windows` requires `NTFS` denoted by the first cross-tree constraint
* `macOS`requires `APFS`

## Building a jar

To create a Java-consumable jar, the [Leiningen](https://leiningen.org) build tool is used.
`lein` will compile first the Clojure source, and then the Java source.
Both compiled Java and Clojure source will be packaged into a jar.

This is achieved through the uberjar task:

```
$ lein uberjar
Compiling de.neominik.uvl.ast
Compiling de.neominik.uvl.parser
Compiling de.neominik.uvl.transform
Compiling 1 source files to uvl-clj/target/classes
Created uvl-clj/target/uvl-parser-0.1.0-SNAPSHOT.jar
Created uvl-clj/target/uvl-parser-0.1.0-SNAPSHOT-standalone.jar
```

The `standalone.jar` includes all dependencies, while the other jar ships only the code of the uvl-parser itself, without Clojure or instaparse.

## Usage from Java
The class `de.neominik.uvl.UVLParser` exposes the static method `parse(String)` which will return an instance of a `de.neominik.uvl.UVLModel` on success or a `de.neominik.uvl.ParseError` when the input didn't comply to the grammar.
Printing is implemented in the `toString()`methods of the different model elements in the `UVLModel`.
The following snippet shows a minimal example to read and write UVL models using the jar. More usage examples that also show how to use the acquired UVLModel object can be found in [examples/java](https://github.com/Universal-Variability-Language/uvl-parser/tree/master/examples/java)

```Java
UVLModel model;

// Read
String content = new String(Files.readAllBytes(Paths.get("file")), StandardCharsets.UTF_8);
Object result = UVLParser.parse(content);
if (result instanceof UVLModel) {
	model = (UVLModel) result;
} else {
	System.out.println("Faulty input");
	model = null;
}


// Write
BufferedWriter writer = new BufferedWriter(new FileWriter(model.getNamespace() + ".uvl"));
writer.write(model.toString());
writer.close();

``` 


## License

Copyright © 2020 Dominik Engelhardt

This software is released under the MIT License (see the file LICENSE for details).
Integrating it into your tool by reusing any part of it is strongly encouraged!



# SqlGenerator

**SqlGenerator** is a lightweight Java library for analyzing and serializing the JPA metamodel and the underlying database schema at runtime, designed for context generation in SQL code generation prompts—especially for use with large language models (LLMs).

Originally developed by Felix Seggebäing as part of a Bachelor thesis project for a financial information system, this library has now been extracted, cleaned up, and made available as an open-source tool.

---

## Features

* Runtime analysis of the JPA metamodel and database schema
* Generation of domain and schema representations as JSON
* Creation of prompt context for LLM-based SQL generation
* JavaFX-based GUI for interactive entity selection and prompt assembly
* Spring and manual configuration supported
* Compatible with Hibernate 7.0.7.Final (recommended) and other JPA implementations

---

## Requirements

* **Java 17** or later
* **Hibernate 7.0.7.Final** (for full SQL dialect/type information in prompts)
* **JavaFX 25-ea+24** (for GUI module)
* Other JPA providers can be used, but SQL dialect/type information in prompt context will be limited

---

## Installation

### Maven (planned for Central release)

```xml
<dependency>
  <groupId>de.seggebaeing</groupId>
  <artifactId>sql-generator</artifactId>
  <version>1.0.0</version>
</dependency>
```

Until available on Maven Central, you can use [JitPack](https://jitpack.io/) or build and install locally.

### Using JitPack (temporary)

```xml
<dependency>
  <groupId>com.github.felixsegg</groupId>
  <artifactId>sql-generator</artifactId>
  <version>TAG_OR_COMMIT</version>
</dependency>
```

---

## Usage

### Core Library

Register the JPA access provider using Spring:

```java
// With Spring (as a bean)
@Component
public class JpaAccess implements InitializingBean { //... }
```

or manually:

```java
// Manual setup
JpaAccess.create(dataSource, entityManager);
```

You can now create an entity selection:

```java
EntityTypeSelection selection = new SimpleEntityTypeSelection(entityTypes);
String prompt = selection.getFullPrompt("Show all orders with total above 100 EUR.");
```

### JavaFX GUI

If you want to use the interactive UI:

* Load and display `fx-gui/fxml.generatorWindow` (e.g., via `FXMLLoader`)
* The root node is set up with the correct controller (`GeneratorController`)
* To access the generated SQL, call `.getResult()` on the controller (returns a `Future<String>`)

---

## License

This project is released under the [MIT License](LICENSE), with the additional requirement to provide attribution to the original author, Felix Seggebäing, in any derivative works or distributions.

---

## Attribution

Developed by Felix Seggebäing.
Originally part of a Bachelor thesis at the Institute for Computer Science of the University of Bonn, 2025.

---

## More Information

API documentation for the modules: 

- Core functionality without GUI framework dependency: [core Javadoc](https://felixsegg.github.io/sql-generator/core/)
- Additional JavaFX logic support (bindable BooleanProperties in type selection object): [fx-logic Javadoc](https://felixsegg.github.io/sql-generator/fx-logic/)
- Fully functional JavaFX GUI: [fx-gui Javadoc](https://felixsegg.github.io/sql-generator/fx-gui/)


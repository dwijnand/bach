# JavaFX Demo

https://openjfx.io

## configuration

Edit `bach.properties` to match required natives with your operating system.

```properties
# Help Bach.java to resolve platform-specific modules by their names
# Using `-win` classifier here. Available classifiers are: -linux, -mac, and -win
module.javafx.base=http://central.maven.org/maven2/org/openjfx/javafx-base/11.0.2/javafx-base-11.0.2-win.jar
module.javafx.controls=http://central.maven.org/maven2/org/openjfx/javafx-controls/11.0.2/javafx-controls-11.0.2-win.jar
module.javafx.graphics=http://central.maven.org/maven2/org/openjfx/javafx-graphics/11.0.2/javafx-graphics-11.0.2-win.jar\
```

## build and run

- `cd demo/javafx`
- `jshell launch.jsh`

module fxapp {
  requires java.base;
  requires java.logging;
  requires transitive javafx.base;
  requires transitive javafx.controls;
  requires transitive javafx.graphics;

  exports fxapp;
}

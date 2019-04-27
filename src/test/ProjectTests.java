/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProjectTests {

  private final Bach bach;
  private final List<String> outLines, errLines;

  private ProjectTests() {
    this.bach = new Bach(true, Path.of(""));
    this.outLines = new ArrayList<>();
    this.errLines = new ArrayList<>();
    bach.log.out = outLines::add;
    bach.log.err = errLines::add;
  }

  @Test
  void defaults() {
    assertEquals(Bach.USER_PATH.getFileName().toString(), bach.project.name);
    assertTrue(outLines.isEmpty());
    assertTrue(errLines.isEmpty());
  }

  @Test
  void nameInRootDirectoryIsProject() {
    var root = FileSystems.getDefault().getRootDirectories().iterator().next();
    assertEquals("project", new Bach(true, root).project.name);
  }

  @Test
  void basedAbsolutePath() {
    assertTrue(Bach.USER_PATH.isAbsolute());
    assertSame(Bach.USER_PATH, bach.project.based(Bach.USER_PATH));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", ".", "path/.."})
  void basedRelativePathPointingToUsersCurrentWorkingDirectory(Path path) {
    assertFalse(path.isAbsolute());
    assertSame(path, bach.project.based(path));
  }

  @Test
  @ResourceLock(Resources.SYSTEM_OUT)
  @ResourceLock(Resources.SYSTEM_ERR)
  void buildAndLaunchMinimalProgram(@TempDir Path workspace) throws Exception {
    var demo = Path.of("src", "test-resources", "program", "minimal");
    var base = workspace.resolve(demo.getFileName());
    Bach.Util.treeCopy(demo, base);

    var out = new ArrayList<String>();
    var bach = new Bach(true, base);
    bach.log.out = out::add;
    var project = bach.project;

    // project
    assertEquals("minimal", project.name);
    assertEquals(base.resolve("bin"), project.bin);
    assertEquals(base.resolve(".bach"), project.cache);
    assertEquals(base.resolve(".bach/modules"), project.cachedModules);
    assertEquals(base.resolve("lib"), project.lib);
    // main
    assertEquals(base.resolve("src"), project.main.source);
    assertTrue(Files.exists(project.main.source));
    assertEquals(base.resolve("bin/realm/main"), project.main.target);
    assertEquals(
        Bach.Util.join(base.resolve("lib"), base.resolve(".bach/modules")),
        project.main.modulePath);
    // test
    assertEquals(base.resolve("bin/realm/test"), project.test.target);
    assertEquals(base.resolve("src/test/java"), project.test.source);
    assertFalse(Files.exists(project.test.source));
    assertEquals(
        Bach.Util.join(project.main.target, base.resolve("lib"), base.resolve(".bach/modules")),
        project.test.modulePath);
    bach.build();
    bach.launch();
    assertLinesMatch(
        List.of(
            "build()",
            "assemble()",
            ">> ASSEMBLE >>",
            "main.compile()",
            ">> BUILD >>",
            "launch()",
            "Launching minimal/modular.Program...",
            ">> LAUNCH >>"),
        out);
    // link
    out.clear();
    project.link();
    assertLinesMatch(
        List.of(
            "Creating custom runtime image...",
            ">> JLINK >>",
            "Running provided tool in-process.+"),
        out);

    Files.delete(base.resolve("src/minimal/modular/Program.java"));
    out.clear();
    bach.launch();
    assertLinesMatch(List.of("launch()", "No <module>[/<main-class>] supplied, no launch."), out);
  }

  @Test
  @DisabledIfSystemProperty(named = "bach.offline", matches = "true")
  void programExternals(@TempDir Path workspace) throws Exception {
    var demo = Path.of("src", "test-resources", "program", "externals");
    var base = workspace.resolve(demo.getFileName());
    Bach.Util.treeCopy(demo, base);

    var bach = new Bach(true, base);
    assertNotNull(bach.properties.getProperty("module.junit3"));

    var out = new ArrayList<String>();
    var err = new ArrayList<String>();
    bach.log.out = out::add;
    bach.log.err = err::add;

    bach.project.assemble();
    assertLinesMatch(
        List.of(
            "assemble()",
            ">> FORMAT >>",
            "External module names: [junit3]",
            ">> RESOLVE >>",
            "Downloaded junit-3.7.jar successfully.",
            "Resolved " + base.resolve(Path.of(".bach/modules/junit-3.7.jar"))),
        out);
    assertTrue(err.isEmpty());

    out.clear();
    bach.properties.remove("module.junit3");
    bach.project.assemble();
    assertLinesMatch(
        List.of("assemble()", ">> FORMAT >>", "External module names: [junit3]", ">> RESOLVE >>"),
        out);
    assertLinesMatch(List.of("External module not mapped: junit3"), err);
  }
}

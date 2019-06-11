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
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentest4j.TestAbortedException;

@ResourceLock(Resources.SYSTEM_OUT)
@ResourceLock(Resources.SYSTEM_ERR)
class DemoTests {

  @Nested
  @DisplayName("jigsaw-quick-start")
  class JigsawQuickStart {

    @ParameterizedTest
    @ValueSource(strings = {"greetings", "greetings-world", "greetings-world-with-main-and-test"})
    void greetings(String name, @TempDir Path workspace) throws Exception {
      var demo = Path.of("demo", "jigsaw-quick-start", name);
      var base = workspace.resolve(demo.getFileName());
      Bach.Util.treeCopy(demo, base);

      var log = new ArrayList<String>();
      var bach = new Bach(true, base);
      bach.log.out = log::add;
      bach.properties.setProperty(Bach.Property.RUN_REDIRECT_TYPE.key, "FILE");

      assertEquals(base, bach.base);
      assertEquals(name, bach.project.name);
      assertEquals("1.0.0-SNAPSHOT", bach.project.version);
      var program = Bach.ModuleInfo.findProgram(base.resolve("src"));
      assertEquals("com.greetings/com.greetings.Main", program);
      var resources = Path.of("src", "test-resources");
      var cleanTreeWalk = resources.resolve(demo.resolveSibling(name + ".clean.txt"));
      assertLinesMatch(Files.readAllLines(cleanTreeWalk), Util.treeWalk(base));

      bach.build();

      if (Files.exists(resources.resolve(demo.resolveSibling(name + ".run.txt")))) {
        assertLinesMatch(
            Files.readAllLines(resources.resolve(demo.resolveSibling(name + ".run.txt"))),
            Files.readAllLines(Path.of(bach.get(Bach.Property.RUN_REDIRECT_FILE))));
      }

      var buildTreeWalk = resources.resolve(demo.resolveSibling(name + ".build.txt"));
      assertLinesMatch(Files.readAllLines(buildTreeWalk), Util.treeWalk(base));
      var logLines = resources.resolve(demo.resolveSibling(name + ".log.txt"));
      assertLinesMatch(Files.readAllLines(logLines), log);

      bach.erase();

      assertLinesMatch(Files.readAllLines(cleanTreeWalk), Util.treeWalk(base));
    }
  }

  @Test
  void scaffold(@TempDir Path workspace) throws Exception {
    var name = "scaffold";
    var demo = Path.of("demo", name);
    var base = workspace.resolve(demo.getFileName());
    Bach.Util.treeCopy(demo, base);

    var log = new ArrayList<String>();
    var bach = new Bach(true, base);
    bach.log.out = log::add;
    bach.properties.setProperty(Bach.Property.RUN_REDIRECT_TYPE.key, "FILE");
    var expected = Path.of("src", "test-resources");
    assertEquals(base, bach.base);
    assertEquals(name, bach.project.name);
    assertEquals("1.0.0-SNAPSHOT", bach.project.version);
    var cleanTreeWalk = expected.resolve(demo.resolveSibling(name + ".clean.txt"));
    // Files.write(cleanTreeWalk, bach.utilities.treeWalk(base));
    assertLinesMatch(Files.readAllLines(cleanTreeWalk), Util.treeWalk(base));
    if (Boolean.getBoolean("bach.offline")) {
      // TODO Better check for unresolvable external modules.
      throw new TestAbortedException("Online mode is required");
    }
    bach.build();
    var buildTreeWalk = expected.resolve(demo.resolveSibling(name + ".build.txt"));
    // Files.write(buildTreeWalk, bach.utilities.treeWalk(base));
    assertLinesMatch(Files.readAllLines(buildTreeWalk), Util.treeWalk(base));
    bach.erase();
    assertLinesMatch(Files.readAllLines(cleanTreeWalk), Util.treeWalk(base));
    var logLines = expected.resolve(demo.resolveSibling(name + ".log.txt"));
    assertLinesMatch(Files.readAllLines(logLines), log);
  }
}

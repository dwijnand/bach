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

package de.sormuras.bach;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BuildTests {

  @Test
  void buildBach(@TempDir Path work) {
    var home = Path.of("");
    var test = new TestRun(home, work);
    var bach = new Bach(test);

    try {
      bach.build();
    } catch (Throwable t) {
      fail(t);
    }

    assertLinesMatch(List.of(), test.errLines());
    assertLinesMatch(
        List.of(
            ">> INIT >>",
            "Compiling main modules: [de.sormuras.bach]",
            ">> BUILD MAIN MODULES >>",
            "Compiling test modules: [integration]",
            ">> BUILD TEST MODULES >>",
            "Build successful."),
        test.outLines());
    assertLinesMatch(
        List.of(
            "target",
            ">>>>",
            "target/bach/main/classes/de.sormuras.bach/de/sormuras/bach/Bach.class",
            ">>>>",
            "target/bach/main/classes/de.sormuras.bach/module-info.class",
            ">>>>",
            "target/bach/main/modules/de.sormuras.bach-" + Bach.VERSION + ".jar",
            ">>>>",
            "target/bach/test/modules/integration-" + Bach.VERSION + ".jar"),
        TestRun.treeWalk(work));
  }
}

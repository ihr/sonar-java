/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.resolve;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.fest.assertions.ObjectAssert;
import org.junit.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

public class MethodJavaSymbolTest {

  @Test
  public void test() {
    File bytecodeDir = new File("target/test-classes");
    JavaAstScanner.scanSingleFileForTests(new File("src/test/java/org/sonar/java/resolve/targets/MethodSymbols.java"), new VisitorsBridge(Collections.singletonList(new MethodVisitor()), Lists.newArrayList(bytecodeDir), null));
  }

  private static class MethodVisitor extends SubscriptionVisitor {

    private static final Set<Integer> overrides = Sets.newHashSet(28, 32, 40, 44, 46, 56, 72, 76, 84, 89, 91, 98, 100, 102);

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return ImmutableList.of(Tree.Kind.METHOD);
    }

    @Override
    public void visitNode(Tree tree) {
      int line = ((JavaTree) tree).getLine();
      JavaSymbol.MethodJavaSymbol symbol = (JavaSymbol.MethodJavaSymbol) ((MethodTree) tree).symbol();
      ObjectAssert assertion = assertThat(symbol.overriddenSymbol()).as("Method at line " + line);
      if (overrides.contains(line)) {
        assertion.isNotNull();
      } else {
        assertion.isNull();
      }
    }
  }

}

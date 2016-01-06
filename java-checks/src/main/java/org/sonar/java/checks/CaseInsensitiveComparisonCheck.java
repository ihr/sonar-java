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
package org.sonar.java.checks;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "S1157",
  name = "Case insensitive string comparisons should be made without intermediate upper or lower casing",
  priority = Priority.MAJOR,
  tags = {Tag.CLUMSY})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.CPU_EFFICIENCY)
@SqaleConstantRemediation("5min")
public class CaseInsensitiveComparisonCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    if (tree.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) tree.methodSelect();
      boolean issue = ("equals".equals(memberSelect.identifier().name()))
        && (isToUpperCaseOrToLowerCase(memberSelect.expression()) || (tree.arguments().size() == 1 && isToUpperCaseOrToLowerCase(tree.arguments().get(0))));
      if (issue) {
        context.addIssue(tree, this, "Replace these toUpperCase()/toLowerCase() and equals() calls with a single equalsIgnoreCase() call.");
      }
    }

    super.visitMethodInvocation(tree);
  }

  private static boolean isToUpperCaseOrToLowerCase(ExpressionTree expression) {
    if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree methodInvocation = (MethodInvocationTree) expression;
      if (methodInvocation.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) methodInvocation.methodSelect();
        String name = memberSelect.identifier().name();
        return "toUpperCase".equals(name) || "toLowerCase".equals(name);
      }
    }
    return false;
  }

}

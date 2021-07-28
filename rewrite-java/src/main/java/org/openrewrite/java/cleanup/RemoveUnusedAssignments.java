/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.cleanup;

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.DeleteStatement;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.*;

/**
 * Determines if an assignment to an identifier has any read operations performed on it.
 * Any read operations performed on an assignment LHS (left-hand side) means the assignment RHS (right-hand side) is used.
 * <p>
 * If an assignment is overwritten by a reassignment later, the previous assignment may need to be removed.
 * In that case, we need to look at whether there are read operations performed on the LHS assignment
 * in the scope between the assignment and the reassignment.
 * <p>
 * If there are read operations, we should not remove the assignment. But if there are not any LHS read operations
 * between the first assignment and the reassignment, then we can prune the original assignment. It's dead code.
 */
@Incubating(since = "7.11.0")
@SuppressWarnings("AnonymousInnerClassMayBeStatic")
public class RemoveUnusedAssignments extends Recipe {
    @Override
    public String getDisplayName() {
        return "Remove unused assignments";
    }

    @Override
    public String getDescription() {
        return "An assignment is unused when a local variable is assigned a value that is not read by any subsequent instruction. " +
                "Calculating a value followed by overwriting it could indicate a coding error, or at least a waste of resources. " +
                "Assignments without a subsequent read which are overwritten by a reassignment are removed.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1854");
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new FindUnusedAssignmentsVisitor();
    }

    private static class FindUnusedAssignmentsVisitor extends JavaIsoVisitor<ExecutionContext> {
        Stack<Tree> currentNameScope;
        Map<Tree, Map<String, Deque<J.Assignment>>> nameScopeToVariableValues;
        Set<J.Assignment> redundantAssignments;
        boolean inForLoopControl = false;

        /**
         * Returns either the current block or a J.Type that may create a reference to a variable.
         * I.E. for(int target = 0; target < N; target++) creates a new name scope for `target`.
         * The name scope in the next J.Block `{}` cannot create new variables with the name `target`.
         * <p>
         * J.* types that may only reference an existing name and do not create a new name scope are excluded.
         */
        private static Cursor getCursorToParentScope(Cursor cursor) {
            return cursor.dropParentUntil(is ->
                    is instanceof J.CompilationUnit ||
                            is instanceof J.Block ||
                            is instanceof J.MethodDeclaration ||
                            is instanceof J.ForLoop ||
                            is instanceof J.ForEachLoop ||
                            is instanceof J.ForLoop.Control ||
                            is instanceof J.Case ||
                            is instanceof J.Try ||
                            is instanceof J.Try.Catch ||
                            is instanceof J.MultiCatch ||
                            is instanceof J.Lambda
            );
        }

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            currentNameScope = new Stack<>();
            nameScopeToVariableValues = new LinkedHashMap<>();
            redundantAssignments = new LinkedHashSet<>();

            super.visitCompilationUnit(cu, ctx);
            for (J.Assignment assignment : redundantAssignments) {
                // remove ... 1 or in bulk.
                doAfterVisit(new DeleteStatement<>(assignment));
            }

            return cu;
        }

        @Nullable
        @Override
        public J postVisit(J tree, ExecutionContext ctx) {
            maybeChangeNameScope(tree);
            return super.postVisit(tree, ctx);
        }

        /**
         * Update the parent scope if the cursor exists in a different name scope.
         *
         * @param parentScope parent of cursor position.
         */
        private void maybeUpdateParentScope(Tree parentScope) {
            if (currentNameScope.size() == 0 || !currentNameScope.peek().equals(parentScope)) {
                currentNameScope.add(parentScope);
            }
        }

        /**
         * Used to check if the name scope has changed.
         * Pops the stack if the tree element is at the top of the stack.
         */
        private void maybeChangeNameScope(Tree tree) {
            // May update updated variables that weren't shadowed.
            if (currentNameScope.size() > 0 && currentNameScope.peek().equals(tree)) {
                if (tree instanceof J.ForLoop.Control) {
                    inForLoopControl = true;
                } else {
                    Map<String, Deque<J.Assignment>> nameScopeVariables = nameScopeToVariableValues.getOrDefault(currentNameScope.peek(), new LinkedHashMap<>());
                    for (String key : nameScopeVariables.keySet()) {
                        Deque<J.Assignment> deque = nameScopeVariables.get(key);
                        while (deque.size() > 1) {
                            redundantAssignments.add(deque.pollFirst());
                        }
                        nameScopeVariables.put(key, deque);
                    }
                    nameScopeToVariableValues.put(currentNameScope.peek(), nameScopeVariables);
                    currentNameScope.pop();

                    if (inForLoopControl &&
                            currentNameScope.size() > 0 && currentNameScope.peek() instanceof J.ForLoop.Control) {
                        currentNameScope.pop();
                        inForLoopControl = false;
                    }
                }
            }
        }

        @Override
        public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
            Cursor parentScope = getCursorToParentScope(getCursor());

            if ((parentScope.getParent() != null &&
                    !(parentScope.getValue() instanceof J.CompilationUnit) &&
                    !(parentScope.getParent().getValue() instanceof J.ClassDeclaration) &&
                    // Does not apply for instance variables of anonymous inner classes
                    !(parentScope.getParent().getValue() instanceof J.NewClass))) {

                maybeUpdateParentScope(parentScope.getValue());

                Map<String, Deque<J.Assignment>> nameScopeVariables = nameScopeToVariableValues.getOrDefault((Tree) parentScope.getValue(), new LinkedHashMap<>());
                String variable = assignment.getVariable().printTrimmed();
                Deque<J.Assignment> queue = nameScopeVariables.getOrDefault(variable, new LinkedList<>());
                queue.add(assignment);
                nameScopeVariables.put(variable, queue);
                nameScopeToVariableValues.put(parentScope.getValue(), nameScopeVariables);
            }

            return super.visitAssignment(assignment, ctx);
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
            Cursor parentScope = getCursorToParentScope(getCursor());
            J parentCheck = getCursor().dropParentUntil(J.class::isInstance).getValue();

            if ((parentScope.getParent() != null &&
                    !(parentScope.getValue() instanceof J.CompilationUnit) &&
                    !(parentScope.getParent().getValue() instanceof J.ClassDeclaration) &&
                    // Does not apply for instance variables of anonymous inner classes.
                    !(parentScope.getParent().getValue() instanceof J.NewClass))) {

                maybeUpdateParentScope(parentScope.getValue());

                J parent = getCursor().dropParentUntil(J.class::isInstance).getValue();
                if (isIdentifierRead(parent, identifier) && !(parent instanceof J.FieldAccess)) {
                    Map<String, Deque<J.Assignment>> nameScopeVariables = nameScopeToVariableValues.getOrDefault((Tree) parentScope.getValue(), new LinkedHashMap<>());
                    Deque<J.Assignment> deque = nameScopeVariables.getOrDefault(identifier.getSimpleName(), new LinkedList<>());
                    if (!deque.isEmpty()) {
                        deque.pollLast();
                    }
                    nameScopeVariables.put(identifier.getSimpleName(), deque);
                    nameScopeToVariableValues.put(parentScope.getValue(), nameScopeVariables);
                }
            }

            return super.visitIdentifier(identifier, ctx);
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            // reset dynamic J.VarDec.Variable boolean states.
            // if variable is read in a MethodInvocation change the state to false.
            // state may be set to true for specific variable declarations if the namespace is shadowed.
            // state must be false if there is no actionable work for the recipe.
            return super.visitMethodDeclaration(method, ctx);
        }

        // note: z J.Binary is technically a read, but happens in a J.For.Control.
        // binary may be parented to control parent => if - else if follows same structure. or else => if
        // or ... => whiteLoop.
        private boolean isIdentifierRead(Tree parent, J.Identifier identifier) {
            return parent instanceof J.MethodInvocation && ((J.MethodInvocation) parent).getArguments().contains(identifier) ||
                    parent instanceof J.Assignment && !((J.Assignment) parent).getVariable().equals(identifier) ||
                    parent instanceof J.Binary ||
                    parent instanceof J.Ternary ||
                    parent instanceof J.AssignmentOperation ||
                    parent instanceof J.Return ||
                    parent instanceof J.FieldAccess;
            // J.FieldAccess is an odd case, since it may reference a variable from a different scope.
            // this.field may exist directly on the class or from an inherited field.
            // may ref a static or constant value, etc.
        }
    }
}

package org.jetbrains.dummy.lang

import org.jetbrains.dummy.lang.tree.*
import java.util.*
import kotlin.collections.HashSet
import kotlin.collections.LinkedHashSet

class VariableInitializationChecker(private val reporter: DiagnosticReporter) : AbstractChecker() {
    override fun inspect(file: File) {
        file.accept(VariableInitializationCheckVisitor(this::reportAccessBeforeInitialization), LinkedHashSet())
    }

    class VariableInitializationCheckVisitor(var reportMethod: (VariableAccess) -> Unit) : DummyLangVisitor<Set<String>?, LinkedHashSet<String>>() {
        override fun visitElement(element: Element, data: LinkedHashSet<String>): Set<String>? {
            element.acceptChildren(this, data)
            return null
        }

        override fun visitVariableDeclaration(variableDeclaration: VariableDeclaration, data: LinkedHashSet<String>): Set<String>? {
            if (variableDeclaration.initializer != null) {
                data.add(variableDeclaration.name)
            }
            super.visitVariableDeclaration(variableDeclaration, data)
            return null
        }

        override fun visitAssignment(assignment: Assignment, data: LinkedHashSet<String>): Set<String>? {
            data.add(assignment.variable)
            super.visitAssignment(assignment, data)
            return null
        }

        override fun visitVariableAccess(variableAccess: VariableAccess, data: LinkedHashSet<String>): Set<String>? {
            if (variableAccess.name !in data) {
                reportMethod(variableAccess)
            }
            super.visitVariableAccess(variableAccess, data)
            return null
        }

        override fun visitIfStatement(ifStatement: IfStatement, data: LinkedHashSet<String>): Set<String>? {
            ifStatement.condition.accept(this, data)
            val thenBlockInits = ifStatement.thenBlock.accept(this, data) ?: Collections.emptySet<String>()
            val elseBlockInits = ifStatement.elseBlock?.accept(this, data) ?: Collections.emptySet<String>()

            if (ifStatement.condition is BooleanConst) {
                if (ifStatement.condition.value) {
                    data.addAll(thenBlockInits)
                } else {
                    data.addAll(elseBlockInits)
                }
            } else {
                data.addAll(thenBlockInits.intersect(elseBlockInits))
            }
            return null
        }

        override fun visitBlock(block: Block, data: LinkedHashSet<String>): Set<String>? {
            val preInitializedVariables = data.size
            super.visitBlock(block, data)
            val result = HashSet<String>(data.reversed().take(data.size - preInitializedVariables).reversed())
            data.removeAll(result)
            return result
        }

        override fun visitFunctionDeclaration(functionDeclaration: FunctionDeclaration, data: LinkedHashSet<String>): Set<String>? {
            data.clear()
            data.addAll(functionDeclaration.parameters)
            super.visitFunctionDeclaration(functionDeclaration, data)
            data.clear()
            return null
        }
    }

    // Use this method for reporting errors
    private fun reportAccessBeforeInitialization(access: VariableAccess) {
        reporter.report(access, "Variable '${access.name}' is accessed before initialization")
    }
}

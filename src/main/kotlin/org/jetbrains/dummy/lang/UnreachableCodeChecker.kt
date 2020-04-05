package org.jetbrains.dummy.lang

import org.jetbrains.dummy.lang.tree.*

class UnreachableCodeChecker(private val reporter: DiagnosticReporter) : AbstractChecker() {
    override fun inspect(file: File) {
        file.accept(UnreachableCodeCheckVisitor(this::reportUnreachableCode), State())
    }

    class UnreachableCodeCheckVisitor(var reportMethod: (Statement) -> Unit) : DummyLangVisitor<Unit, State>() {
        override fun visitElement(element: Element, data: State) {
            element.acceptChildren(this, data)
        }

        override fun visitStatement(statement: Statement, data: State) {
            if (!data.reachable) {
                reportMethod(statement)
            }
            super.visitStatement(statement, data)
        }

        override fun visitReturnStatement(returnStatement: ReturnStatement, data: State) {
            super.visitReturnStatement(returnStatement, data)
            data.reachable = false
        }

        // Checks if all reachable blocks have "return" statement
        override fun visitIfStatement(ifStatement: IfStatement, data: State) {
            if (data.reachable) {
                ifStatement.condition.accept(this, data)

                data.reachable = if (ifStatement.condition is BooleanConst) ifStatement.condition.value else true
                ifStatement.thenBlock.accept(this, data)
                val thenBlockState = data.reachable || (if (ifStatement.condition is BooleanConst) !ifStatement.condition.value else false)

                data.reachable = if (ifStatement.condition is BooleanConst) !ifStatement.condition.value else true
                ifStatement.elseBlock?.accept(this, data)
                val elseBlockState = data.reachable || (if (ifStatement.condition is BooleanConst) ifStatement.condition.value else false)

                data.reachable = thenBlockState || elseBlockState
            } else {
                super.visitIfStatement(ifStatement, data)
            }
        }

        override fun visitFunctionDeclaration(functionDeclaration: FunctionDeclaration, data: State) {
            data.reachable = true
            super.visitFunctionDeclaration(functionDeclaration, data)
        }
    }

    class State(var reachable: Boolean = true)

    // Use this method for reporting errors
    private fun reportUnreachableCode(statement: Statement) {
        reporter.report(statement, "Unreachable code")
    }
}
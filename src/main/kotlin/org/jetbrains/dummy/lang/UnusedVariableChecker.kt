package org.jetbrains.dummy.lang

import org.jetbrains.dummy.lang.tree.*

class UnusedVariableChecker(private val reporter: DiagnosticReporter) : AbstractChecker() {
    override fun inspect(file: File) {
        file.accept(UnreachableCodeCheckVisitor(this::reportUnusedVariable), LinkedHashMap())
    }

    class UnreachableCodeCheckVisitor(var reportMethod: (String, LinkedHashMap<String, MutableList<Element>>) -> Unit) : DummyLangVisitor<Unit, LinkedHashMap<String, MutableList<Element>>>() {
        override fun visitElement(element: Element, data: LinkedHashMap<String, MutableList<Element>>) {
            element.acceptChildren(this, data)
        }

        override fun visitVariableDeclaration(variableDeclaration: VariableDeclaration, data: LinkedHashMap<String, MutableList<Element>>) {
            addToUnusedVariables(variableDeclaration.name, variableDeclaration, data)
            super.visitVariableDeclaration(variableDeclaration, data)
        }

        override fun visitAssignment(assignment: Assignment, data: LinkedHashMap<String, MutableList<Element>>) {
            addToUnusedVariables(assignment.variable, assignment, data)
            super.visitAssignment(assignment, data)
        }

        private fun addToUnusedVariables(name: String, element: Element, data: LinkedHashMap<String, MutableList<Element>>) {
            if (data[name] == null) {
                data[name] = mutableListOf()
            }
            data[name]!!.add(element)
        }

        override fun visitVariableAccess(variableAccess: VariableAccess, data: LinkedHashMap<String, MutableList<Element>>) {
            if (data[variableAccess.name] != null) data[variableAccess.name]!!.clear()
            super.visitVariableAccess(variableAccess, data)
        }

        override fun visitBlock(block: Block, data: LinkedHashMap<String, MutableList<Element>>) {
            val preInitializedVariables = HashSet(data.keys)
            super.visitBlock(block, data)
            val unusedVariables = data.keys.filter { it !in preInitializedVariables }
            unusedVariables.forEach { reportMethod(it, data); data.remove(it) }
        }

        override fun visitFunctionDeclaration(functionDeclaration: FunctionDeclaration, data: LinkedHashMap<String, MutableList<Element>>) {
            data.clear()
            functionDeclaration.parameters.forEach { addToUnusedVariables(it, functionDeclaration, data) }
            super.visitFunctionDeclaration(functionDeclaration, data)
            functionDeclaration.parameters.forEach { reportMethod(it, data); data.remove(it) }
        }
    }

    // Use this method for reporting errors
    private fun reportUnusedVariable(name: String, data: LinkedHashMap<String, MutableList<Element>>) {
        for (element: Element in data[name]!!) {
            reporter.report(element, "${if (element is FunctionDeclaration) "Parameter" else "Variable"} '${name}' is never used")
        }
    }
}
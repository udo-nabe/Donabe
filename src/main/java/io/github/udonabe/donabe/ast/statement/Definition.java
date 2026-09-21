package io.github.udonabe.donabe.ast.statement;

import io.github.udonabe.donabe.ast.expr.Identifier;
import io.github.udonabe.donabe.ast.type.TypeAnnotation;

public sealed interface Definition
		extends Statement
		permits LetDeclaration, VarDeclaration, FunctionDefineStatement {
	Identifier name();
        TypeAnnotation type();
}

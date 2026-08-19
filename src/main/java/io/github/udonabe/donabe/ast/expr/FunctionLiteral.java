package io.github.udonabe.donabe.ast.expr;

import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.SourceFileLocation;
import io.github.udonabe.donabe.ast.statement.BlockStatement;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class FunctionLiteral implements Expression {
    private final List<Identifier> args;
    private final BlockStatement block;
    private final SourceFileLocation location;
    private Set<Integer> locals;

    public FunctionLiteral(List<Identifier> args, BlockStatement block, SourceFileLocation location) {
        this.args = args;
        this.block = block;
        this.location = location;
    }

    public List<Identifier> args() {
        return args;
    }

    public BlockStatement block() {
        return block;
    }

    @Override
    public SourceFileLocation location() {
        return location;
    }

    public Set<Integer> locals() {
        if (locals == null) {
            throw new IllegalStateException("'locals' has not yet been set.");
        }
        return locals;
    }

    public void setLocals(Set<Integer> locals) {
        if (locals == null) {
            throw new IllegalStateException("The argument 'locals' must not be null.");
        }
        if (this.locals != null) {
            throw new IllegalStateException("'locals' has already been set. value: " + this.locals + " location: " + location);
        }
        this.locals = locals;
    }

    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitFunctionLiteral(this);
    }

    @Override
    public String display() {
        return "<function(" + args.stream().map(Identifier::name).toList() + "->?)>";
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FunctionLiteral literal)) return false;
        return Objects.equals(args, literal.args) && Objects.equals(block, literal.block) && Objects.equals(location, literal.location) && Objects.equals(locals, literal.locals);
    }

    @Override
    public int hashCode() {
        return Objects.hash(args, block, location, locals);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("FunctionLiteral{");
        sb.append("args=").append(args);
        sb.append(", block=").append(block);
        sb.append(", location=").append(location);
        sb.append(", locals=").append(locals);
        sb.append('}');
        return sb.toString();
    }
}

package io.github.udonabe.donabe.ast.statement;

import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.SourceFileLocation;
import io.github.udonabe.donabe.ast.expr.Expression;
import io.github.udonabe.donabe.ast.expr.Identifier;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class FunctionDefineStatement implements Statement {
    private final Identifier identifier;
    private final List<Identifier> args;
    private final BlockStatement block;
    private final SourceFileLocation location;
    private Set<Integer> locals;

    public FunctionDefineStatement(Identifier identifier, List<Identifier> args, BlockStatement block, SourceFileLocation location) {
        this.identifier = identifier;
        this.args = args;
        this.block = block;
        this.location = location;
    }

    public Identifier identifier() {
        return identifier;
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
        return visitor.visitFunctionDefineStatement(this);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FunctionDefineStatement that)) return false;
        return Objects.equals(identifier, that.identifier) && Objects.equals(args, that.args) && Objects.equals(block, that.block) && Objects.equals(location, that.location) && Objects.equals(locals, that.locals);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, args, block, location, locals);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("FunctionDefineStatement{");
        sb.append("identifier=").append(identifier);
        sb.append(", args=").append(args);
        sb.append(", block=").append(block);
        sb.append(", location=").append(location);
        sb.append(", locals=").append(locals);
        sb.append('}');
        return sb.toString();
    }
}

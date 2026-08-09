package io.github.udonabe.donabe.ast.expr;

import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.SourceFileLocation;

import java.util.Objects;

public final class Identifier implements Expression {
    public static final int UNRESOLVED_ID = -1;

    private final String name;
    private final SourceFileLocation location;
    private int id;

    public Identifier(String name, SourceFileLocation location) {
        this.name = name;
        this.location = location;
        this.id = UNRESOLVED_ID;
    }

    public void resolve(int id) {
        if (id < 0) {
            throw new IllegalArgumentException("The argument 'id' must not be a negative value.");
        }
        if (this.id != UNRESOLVED_ID) {
            throw new IllegalStateException("This identifier has already been resolved.");
        }
        this.id = id;
    }

    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitIdentifier(this);
    }

    @Override
    public String display() {
        return name;
    }

    public String name() {
        return name;
    }

    @Override
    public SourceFileLocation location() {
        return location;
    }

    public int id() {
        if (id == UNRESOLVED_ID) {
            throw new IllegalStateException("This identifier has not yet been resolved.");
        }
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Identifier) obj;
        return Objects.equals(this.name, that.name) &&
               Objects.equals(this.location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, location);
    }

    @Override
    public String toString() {
        return "Identifier[" +
               "name=" + name + ", " +
               "location=" + location + ']';
    }

}

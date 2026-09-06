package io.github.udonabe.donabe.compile.code;

import io.github.udonabe.donabe.compile.code.section.Section;
import java.util.HashSet;
import java.util.Set;

public record ByteCode(Set<Section> sections) {
    public ByteCode {
        sections = Set.copyOf(sections);
    }
    
    public ByteCode addSection(Section section) {
        Set<Section> result = new HashSet<>(sections);
        result.add(section);
        return new ByteCode(result);
    }
}

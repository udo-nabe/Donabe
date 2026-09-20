package io.github.udonabe.donabe.compile.code.section;

import io.github.udonabe.donabe.compile.code.EndianUtil;

public record IdentifiersSection(int resolutionMax) implements Section {

    public IdentifiersSection {
        if (resolutionMax > 0xFFFF) {
            throw new IllegalArgumentException("Identifiers are too many.");
        }
    }

    @Override
    public byte type() {
        return IDENTIFIERS_SECTION_TYPE;
    }

    @Override
    public byte[] content() {
        return EndianUtil.to2BytesLittleEndian(resolutionMax);
    }

}

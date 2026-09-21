package io.github.udonabe.donabe.compile.code.constant;

public interface ConstantPoolEntry<T> {
    byte METHOD_REF_TYPE = 0x01;
    byte VALUE_TYPE = 0x02;
    byte TOPLEVEL_REF_TYPE = 0x03;
    
    T value();
    byte type();
    byte[] content();
}

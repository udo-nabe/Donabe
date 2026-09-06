package io.github.udonabe.donabe.compile.code.value;

public sealed interface CodeValue
        permits BoolCodeValue, FunctionCodeValue, IntCodeValue, ListCodeValue, StringCodeValue {

    byte BOOL_TYPE = 0x01;
    byte INT_TYPE = 0x02;
    byte STRING_TYPE = 0x03;
    byte FUNCTION_TYPE = 0x04;
    byte LIST_TYPE = 0x05;

    byte type();

    byte[] content();
}

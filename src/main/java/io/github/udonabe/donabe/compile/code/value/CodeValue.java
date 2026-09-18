package io.github.udonabe.donabe.compile.code.value;

public sealed interface CodeValue
        permits BoolCodeValue, FunctionCodeValue, IntCodeValue, Int64CodeValue, ListCodeValue, StringCodeValue {

    byte BOOL_TYPE = 0x01;
    byte INT_TYPE = 0x02;
    byte INT64_TYPE = 0x03;
    byte STRING_TYPE = 0x04;
    byte FUNCTION_TYPE = 0x05;
    byte LIST_TYPE = 0x06;

    byte type();

    byte[] content();
}

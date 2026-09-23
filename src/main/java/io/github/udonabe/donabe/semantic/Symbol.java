package io.github.udonabe.donabe.semantic;

public sealed interface Symbol
		permits LocalSymbol, GlobalSymbol, CaptureSymbol {

}

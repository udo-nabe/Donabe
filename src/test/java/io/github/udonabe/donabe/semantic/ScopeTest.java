package io.github.udonabe.donabe.semantic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScopeTest {
    @Test
    void get() {
        //宣言したものを正常に取得できるか
        Scope normality = Scope.generateRoot();
        normality.put("foo", new SymbolInformation(true, false));
        assertEquals(new SymbolInformation(true, false), normality.get("foo"));
        //親に宣言がある場合、親のものが取得されるか
        Scope child = normality.newChild();
        assertEquals(new SymbolInformation(true, false), child.get("foo"));
        //親と自分自身両方に宣言がある場合、自分自身が優先されるか
        child.put("foo", new SymbolInformation(false, false));
        assertEquals(new SymbolInformation(false, false), child.get("foo"));

        //宣言されていないかつ親がnullの場合、nullが返るか
        assertNull(normality.get("undefined"));

        //keyがnullの場合、NullPointerExceptionとなるか
        assertThrows(NullPointerException.class, () -> normality.get(null));
    }

    @Test
    void put() {
        //まだ宣言されていない場合、正常に宣言できるか
        Scope normality = Scope.generateRoot();
        assertTrue(normality.put("foo", new SymbolInformation(true, false)));
        assertEquals(new SymbolInformation(true, false), normality.get("foo"));
        //既に宣言されている場合、宣言が上書きされないか
        assertFalse(normality.put("foo", new SymbolInformation(false, false)));
        assertEquals(new SymbolInformation(true, false), normality.get("foo"));
        //仮宣言の場合、上書きが可能か
        assertTrue(normality.put("temp", new SymbolInformation(true, true)));
        assertTrue(normality.put("temp", new SymbolInformation(false, false)));
        assertEquals(new SymbolInformation(false, false), normality.get("temp"));

        //keyがnullの場合、NullPointerExceptionとなるか
        assertThrows(NullPointerException.class, () -> normality.put(null, new SymbolInformation(false, false)));

        //valueがnullの場合、NullPointerExceptionとなるか
        assertThrows(NullPointerException.class, () -> normality.put("null", null));
    }

    @Test
    void symbolTable() {
        //変更不可のマップが返されるか
        var map = Scope.generateRoot().symbolTable();
        assertThrows(UnsupportedOperationException.class, () -> map.put("exception", new SymbolInformation(false, false)));
    }
}
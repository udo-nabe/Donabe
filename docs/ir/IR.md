# Donabe IRの仕様
## 目的
バイトコードの前段階となる、プログラム内で扱いやすい方式を提供すること。

## 基本モデル
スタックマシンであり、ほぼ全ての命令はスタックを使用して処理を行う。
ASTで文だったものは必ずスタックを空にし、式だったものは必ず1つスタックに値を残す。

## オペランド
`load_captured`、`store_captured`、`load_local`、`store_local`、`push`、`jmp`、`jmpfalse`、`jmptrue`、`label_nop`以外は命令自体にオペランドを持たず、スタックからオペランドを取得する。必要数のオペランドがない場合は実行時エラーとする。

## 識別子スロット
識別子スロットとは、名前解決された結果の識別子番号である。
連続している必要はなく、プログラム全体で重複が無ければ問題ない。

## 定義
### TOS
Top Of Stack: スタックトップ。

### TOS-N
TOSのN個下のデータ。
例：
```
<上>
[TOS]
[TOS-1]
[TOS-2]
...
[TOS-N]
<下>
```

### スタック効果
命令の実行によって生じるスタックの変化。左をTOSとして扱う。
`unchanged`は、それより下の要素が変更されないことを意味する。

## 命令セット
### add
スタックから値を二つ消費し、`TOS-1 + TOS`を計算して結果をスタックに積む。

スタック効果:
`a, b, unchanged` → `b + a, unchanged`


### call
スタックから値を`呼び出し対象の引数個数 + 1`個消費し、次の規則に従って呼び出す。
- TOSを呼び出し対象とみなす。関数型でない場合は実行時エラーとする。
- それ以降は、`TOS-1`を第一引数として、`TOS-N`を第N引数とみなす。足りない場合は実行時エラーとし、必要より多い場合は必要なだけ消費する。
- 呼び出しは、関数値をもとに新しいスタックフレームを作り、それを現在のスタックフレームへ設定することで実現する。

スタック効果:
`function, first, second, unchanged` → `return, unchanged`

### div
スタックから値を二つ消費し、`(TOS-1) / TOS`を計算して結果をスタックに積む。
ゼロ除算の場合は実行時エラーとする。

スタック効果:
`a, b, unchanged` → `b / a, unchanged`

### equal
スタックから値を二つ消費し、二つの型と値が等しければ`true`を、そうでなければ`false`をスタックに積む。

スタック効果:
`a, b, unchanged` → `b == a, unchanged`

### greater
スタックから値を二つ消費し、`(TOS-1) > TOS`であれば`true`を、そうでなければ`false`をスタックに積む。二つの型がともに`int`でなければ実行時エラーとする。

スタック効果:
`a, b, unchanged` → `b > a, unchanged`

### greater_equal
スタックから値を二つ消費し、`(TOS-1) >= TOS`であれば`true`を、そうでなければ`false`をスタックに積む。二つの型がともに`int`でなければ実行時エラーとする。

スタック効果:
`a, b, unchanged` → `b >= a, unchanged`

### jmp <ジャンプ先のラベル>
<ジャンプ先のラベル>へ移動する。ラベルが存在しなければ実行時エラーとする。

スタック効果:
`unchanged` → `unchanged`

### jmp_false <ジャンプ先のラベル>
TOSを消費し、その値が`false`であれば<ジャンプ先のラベル>へ移動する。ラベルが存在しなければ実行時エラーとする。また、TOSの値が`boolean`以外の型であれば実行時エラーとする。

スタック効果:
`condition, unchanged` → `unchanged`

### jmp_true <ジャンプ先のラベル>
TOSを消費し、その値が`true`であれば<ジャンプ先のラベル>へ移動する。ラベルが存在しなければ実行時エラーとする。また、TOSの値が`boolean`以外の型であれば実行時エラーとする。

スタック効果:
`condition, unchanged` → `unchanged`

### label_nop <ラベル名>
この命令をジャンプ先として登録する。ラベル名に制限はないが、コード全体で一意である必要がある。

スタック効果:
`unchanged` → `unchanged`

### less
スタックから値を二つ消費し、`(TOS-1) < TOS`であれば`true`を、そうでなければ`false`
をスタックに積む。二つの型がともに`int`でなければ実行時エラーとする。

スタック効果:
`a, b, unchanged` → `b < a, unchanged`

### less_equal
スタックから値を二つ消費し、`(TOS-1) <= TOS`であれば`true`を、そうでなければ`false`をスタックに積む。二つの型がともに`int`でなければ実行時エラーとする。

スタック効果:
`a, b, unchanged` → `b <= a, unchanged`

### load_captured <識別子番号>
親スタックフレームから再帰的に<識別子番号>の識別子の値を取得し、スタックへ積む。ルートまで行っても存在しない場合は実行時エラーとする。

スタック効果:
`unchanged` → `identifierValue, unchanged`

### load_local <識別子番号>
現在のスタックフレームから<識別子番号>の識別子の値を取得し、スタックへ積む。存在しない場合は実行時エラーとする。

スタック効果:
`unchanged` → `identifierValue, unchanged`

### minus
TOSの値の符号を反転する。TOSが`int`型でなければ実行時エラーとする。

スタック効果:
`a, unchanged` → `-a, unchanged`

### mul
スタックから値を二つ消費し、`(TOS-1) * TOS`を計算して結果をスタックに積む。

スタック効果:
`a, b, unchanged` → `b * a, unchanged`

### nop
何もせず次の命令へ移る。

スタック効果:
`unchanged` → `unchanged`

### not
TOSの値の真偽を反転する。TOSが`boolean`型でなければ実行時エラーとする。

スタック効果:
`bool, unchanged` → `!bool, unchanged`

### plus
TOSの値の符号を保ったままにする。TOSが`int`型でなければ実行時エラーとする。

スタック効果:
`a, unchanged` → `a, unchanged`

### pop
TOSの値を破棄する。スタックが空なら実行時エラーとする。

スタック効果:
`discarded, unchanged` → `unchanged`

### push <値>
スタックに<値>を積む。

スタック効果:
`unchanged` → `value, unchanged`

### return
TOSを消費し、スタックフレームを親へ戻し、スタックへその値を積んでから戻り先アドレスへ戻る。つまり、TOSを呼び出し元へ返す。スタックが空なら実行時エラーとする。

スタック効果:

呼び出し先スタック:
`return, unchanged` → `unchanged`

呼び出し元スタック:
`unchanged` → `return, unchanged`

### store_captured <識別子番号>
TOSを消費し、その値を次の規則に従って保存する。
- <識別子番号>をローカル変数として持つスタックフレームを親から再帰的に探し、見つけたらそこへ保存する。
- ルートまで行っても存在しない場合は実行時エラーとする。

スタック効果:
`stored, unchanged` → `unchanged`

### store_local <識別子番号>
TOSを消費し、その値をを現在のスタックフレームの<識別子番号>の識別子へ保存する。存在しない場合は実行時エラーとする。

スタック効果:
`stored, unchanged` → `unchanged`

### sub
スタックから値を二つ消費し、`(TOS-1) - TOS`を計算して結果をスタックに積む。

スタック効果:
`a, b, unchanged` → `b - a, unchanged`

### vreturn
スタックフレームを親へ戻し、スタックに`Void`を積んで戻り先アドレスへ移動する。つまり、呼び出し元に`Void`を返す。

スタック効果:

呼び出し先スタック:
`unchanged` → `unchanged`

呼び出し元スタック:
`unchanged` → `Void, unchanged`

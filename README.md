# Donabe

Donabeは、学習を目的として開発しているプログラミング言語です。

## 概要

Donabeは、今のところ動的型付けのインタプリタ言語として実装しています。
静的型付けのVM言語へ徐々に転換していく予定です。

書きやすいC系文法の言語を目指しています。

## 特徴

- 制御構文の条件式の括弧が不要
- 関数を第一級オブジェクトとして扱える
- 定数を`let`で短く表せる

## サンプル

以下は、現在の簡単なサンプルプログラムです。
言語機能の拡充次第、変更していく予定です。
```donabe
// Fibonacci
// 生成する数字の数
print("How many do you want to generate?> ");
let limit = int(input());
var a = 1;
var b = 1;
var result = "[1, 1";

for var i = 0; i < limit - 2; i++ {
  let next = a + b;
  a = b;
  b = next;
  result += ", " + next;
}
print(result + "]");
```

実行結果:

```text
How many do you want to generate?> 
10
[1, 1, 2, 3, 5, 8, 13, 21, 34, 55]
```

## インストール

### 必要なもの

- Gitを使えるソフトウェア

### ビルド

以下のコマンドでビルドできます。

```bash
./gradlew build
```

## 使い方

以下のように実行できます。
jpackageなどにはまだ未対応ですので、gradle経由で実行してください。
```bash
./gradlew run --args="<実行したいソースファイル>"
```

例えば、次のプログラムを `example.dnb` として保存します。

```donabe
print("Hello, World!");
```

その後、以下のコマンドを実行します。

```bash
./gradlew run --args="example.dnb"
```
実行結果:
```text
Hello, World!
```
## 言語仕様

### 変数
letが定数、varが変数です。宣言と同時に初期化をする必要があります。
```donabe
let foo = 1;
var bar = "Hello";
```

### 型
今のところ動的型付けです。次の型があります。
- int: 整数
- string: 文字列
- boolean: 真偽値
- void: 関数の戻り値が無い場合の特殊な値。変数へ代入したり、関数の引数にしたりは可能ですが、演算には使えません。
- list: リスト
- function: 関数。組み込み関数と通常の関数がありますが、型の名前上区別はつきません。print()で表示すると組み込み関数は"<builtin-function>"に、通常の関数は"<function([引数名のリスト]->?)>"になります。


### 関数
関数は、Donabeでは値として扱われます。関数自体は名前を持たず、引数を受け取って値を返すものとして扱われます。

しかし、
```donabe
let add = func(a, b) {
  return a + b;
}
```
と
```donabe
func add(a, b) {
  return a + b;
}
```
は少し違ったものとして扱われます。前者は文を実行した瞬間に定義され、後者は定義されているブロックの実行を開始した時点で定義されるのです。

### 制御構文
制御構文の条件式の括弧は不要で、波括弧は省略不可です。
条件がfalseなど、到達不可な制御構文はエラーになりません。
- if-else if-else文
  
  単純なif文:
  ```donabe
  if foo < 10 {
    print("foo < 10");
  }
  ```
  if-else文:
  ```donabe
  if foo < 10 {
    print("foo < 10");
  } else {
    print("foo >= 10);
  }
  ```
  if-else if-else文:
  ```donabe
  if foo < 10 {
    print("foo < 10");
  } else if foo < 20 {
    print("10 < foo < 20");
  } else {
    print("foo >= 20");
  }
  ```
  なお、上のelse ifは次の糖衣構文として実装されます。
  ```donabe
  if foo < 10 {
    print("foo < 10");
  } else {
    if foo < 20 {
      print("10 < foo < 20");
    } else {
      print("foo >= 20");
    }
  }
  ```
  else-ifを複数連ねることも可能です。
  ```donabe
  if foo < 10 {
    print("foo < 10");
  } else if foo < 20{
    print("10 < foo < 20");
  } else if foo < 30 {
    print("20 < foo < 30");
  }
  ```
- while文
  ```donabe
  while foo < 10 {
    print(foo++);
  } 
  ```
- for文
  
  while文の糖衣構文として実装されます。

  例えば、
  ```donabe
  for var i = 0; i < 10; i++ {
    print("i: " + i);
  }
  ```
  は
  ```donabe
  {
    var i = 0;
    while i < 10 {
      print("i: " + i);
      i++;
    }
  }
  ```
  に脱糖されます。
- for-each文
  
  糖衣構文ではなく特殊な文として実装されます。
  
  構文:
  ```donabe
  let list = ["Hello", "World"];
  for let l in list {
    print(l);
  }
  ```
  実行結果:
  ```text
  Hello
  World
  ```

## 処理系の構成

[言語名] の処理系は、現在以下のような流れでプログラムを処理します。

```text
ソースコード
    ↓
 字句解析
    ↓
 構文解析
    ↓
   AST
    ↓
名前解決・意味解析
    ↓
インタプリタ
    ↓
   実行
```

### 各処理の説明

#### 字句解析
ソースコードをトークン列に変換します。
#### 構文解析
トークン列をASTに変換します。構文エラーはこの段階でエラーとなりますが、名前解決などは行われません。
#### AST
ソースコードを木構造として保持します。
#### 名前解決・意味解析
##### 名前解決
プログラム中の識別子を固有のIDへ変換します。
##### 意味解析
プログラムの構文エラー以外のエラー(識別子が存在しない、定数へ代入しているなど)をチェックします。
#### インタプリタ
木構造をたどり、プログラムを実行します。
## ディレクトリ構成

現在のディレクトリ構成は以下のようになっています。

```text
Donabe/
├── src/main/java
│   └── ソースコード
├── src/test/java
│   └── テストコード
└── README.md
```

## 開発状況

現在の実装状況です。

- [x] 字句解析
- [x] 構文解析
- [x] 名前解決
- [x] 意味解析
- [x] AST
- [x] インタプリタ
- [x] 関数
- [ ] VM
- [ ] 型検査
- [ ] 標準ライブラリ
- [ ] エラーメッセージの改善
- [ ] 最適化
- [ ] ドキュメントの整備

## 今後の予定

今後は以下の機能を実装する予定です。

- メンバ参照
- 型検査
- 静的型付け
- コンパイラ
- VM

## 開発

### リポジトリの取得

```bash
git clone https://github.com/udo-nabe/Donabe.git
cd Donabe
```

### テスト

```bash
./gradlew test
```

### 開発用ビルド

```bash
./gradlew build
```

## 既知の問題

現在、以下の問題があります。

- クロージャがおかしい
- 関数をネストするとreturnできなくなる

## ライセンス
MIT License

詳しくは [LICENSE](LICENSE) を参照してください。

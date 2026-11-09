# リソースの手管理・データ生成方針

標準的な定義をコードで表現できるリソースは datagen を原則とし、人が形や文章を編集する制作物は手管理する。ファイル数や JSON
の短さでは分けず、1 件でも同じ基準を適用する。

## 判断基準

| リソース                                           | 原則                         | 理由・扱い                                                                  |
|----------------------------------------------------|------------------------------|-----------------------------------------------------------------------------|
| レシピ・採掘を含むタグ・ルートテーブル             | datagen                      | 登録内容やゲーム仕様に対応させ、標準 Provider で出力する。                  |
| configured feature・placed feature・biome modifier | datagen                      | 登録キー・参照関係・生成条件をまとめて管理する。                            |
| 通常の blockstate・立方体モデル                    | datagen                      | 標準形状とテクスチャの組み合わせで表現できる。                              |
| 平面アイテム・通常の手持ち道具モデル               | datagen                      | 親モデルとテクスチャを指定する定型データ。                                  |
| Blockbench 等で制作する独自モデル・アニメーション  | 制作元と書き出した資産を管理 | 形状や動きは制作ツールで編集し、書き出した JSON を datagen で再構築しない。 |
| テクスチャ・音声・構造物 NBT                       | 制作物として管理             | 画像・音・ゲーム内配置が編集の中心になる。                                  |
| 翻訳・テクスチャのアニメーション設定 `.mcmeta`     | 当面は手管理                 | 文章や再生タイミングを直接編集する。                                        |

独自モデルの形状本体は制作物として保持し、それを参照するモデルや blockstate を datagen
で生成してよい。ブロック・アイテム単位ですべてを同じ方式に揃える必要はない。独自形式や標準 Provider
で扱いづらい定義も、制作・編集のしやすさを理由として手管理を選んでよい。

## 配置と担当

- 手管理の配布リソースは `src/main/resources`、生成される配布リソースは `src/generated/resources` に置く。
- 生成処理は `jp.aquafactory.artificesassorite.datagen` 配下に種類別の Provider として配置し、データ生成イベントへ接続する。現在は Provider 未実装であり、以下は追加時の配置方針とする。
- ブロックモデル・blockstate・対応する BlockItem モデルは `BlockModelProvider`、通常アイテムのモデルは
  `ItemModelProvider` が担当する。
- アイテムモデルは対象ごとに平面・手持ち・既存モデル参照等の種類を明示する。全登録アイテムへ一律に平面モデルを適用せず、BlockItem
  の生成をブロック側と重複させない。
- バニラの作業台・不定形クラフト・精錬・鍛冶台等のレシピは `VanillaRecipeProvider` にまとめる。
- 同じ出力先のファイルを複数の Provider、または手管理と datagen の両方で管理しない。制作元の形式と配布用の書き出しファイルは用途が異なるため、この重複には含めない。

## 編集・移行・検証

1. 生成 JSON は直接編集せず、Provider を修正して `runData` で再生成する。生成 JSON も Git 管理し、レビューでは生成元と出力の両方を確認する。生成用
   `.cache` は Git 管理しない。
2. 手管理から datagen への移行では、元ファイルを削除して重複をなくす。管理方法だけを変える場合は、生成前後で JSON
   の意味・参照先・表示設定が同じであることを確認する。
3. 改名・削除・生成先変更では、旧ファイルの削除も確認する。`runData` が不要ファイルをすべて削除するとは扱わず、特に生成用
   `.cache` に記録されていない旧出力の残存に注意する。
4. 再生成後に手管理との出力パス重複、意図しない差分、参照切れを確認し、`build` を実行する。データ読込やサーバー登録等への変更では
   GameTest も実行する。必要なクライアント確認は `AGENTS.md` に従って引き継ぐ。

## 参照元リソースの取り込み

他リポジトリから取り込む場合も Minecraft 1.21.1 / NeoForge の Provider と schema に合わせる。名前空間だけの一括置換では完了とせず、参照 ID、依存 MOD、出力パスと旧出力の残存を確認する。現在は他バージョンの保守・backport 運用を設けない。

## 参考

- [NeoForge 1.21.1: Resources](https://docs.neoforged.net/docs/1.21.1/resources/)
- [NeoForge 1.21.1: Model Datagen](https://docs.neoforged.net/docs/1.21.1/resources/client/models/datagen/)
- [Minecraft Java Edition 1.21: Directory renames](https://feedback.minecraft.net/hc/en-us/articles/27547857163917-Minecraft-Java-Edition-1-21-Tricky-Trials)

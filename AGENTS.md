# AGENTS.md

このリポジトリで作業する人間・AI エージェント向けの共通ルール。

## 言語・文字コード

- やり取り、文書、レビュー、コミットメッセージは原則日本語。
- プレイヤー向けテキストは lang を使う。config の comment、Issue フォームなど言語選択できない部分は英語。GameTest の assertion、例外、ログなど診断文字列も検索性のため英語にする。
- コメントは「なぜ」を日本語で説明する。UTF-8（BOM なし）を使用する。日本語・翻訳・Markdown・Skill の編集前に `.codex/skills/text-encoding-hygiene/SKILL.md` を読む。

## 対象と環境

- Artifices Assortie: Minecraft 1.21.1 / NeoForge / Java 21。バージョンの正は `gradle.properties`。
- MOD ID は `artificesassortie`、package は `jp.aquafactory.artificesassorite`。
- `main` が開発基準。1.20.1 / Forge の保守・backport 運用は設けない。
- Iron's Arms 'n Artifice / GeckoLib / Iron's Lib が必須依存。Create のみ任意依存。Iron's Spells 'n Spellbooks / Iron's Gems 'n Jewelry は開発用 Irons / Magic 構成へだけ追加し、本 MOD の必須・任意依存には含めない。
- Magic 構成は Irons 構成一式に Malum / Lodestone を加える。Malum の前提 Curios は Irons 構成で導入し、本 MOD の必須・任意依存には追加しない。
- client / GameTest の共通構成は必須依存に JEI / Jade / Target Dummy と前提の MezzConfig / Moonlight Lib を加える。追加構成とバージョンは `build.gradle`、`gradle.properties`、README を確認する。開発用 MOD を配布 jar へ同梱しない。
- 実行構成のロード・混入確認は `gametest.runtime` に置く。機能・連携の追加時には、対応する処理結果や状態遷移を確認する挙動テストも追加する。
- IntelliJ IDEA では JetBrains MCP を優先してシンボル・型・依存・Inspection・実行構成を確認する。利用できない場合は代替手段で続行し報告する。

## 実行と検証

```powershell
.\scripts\use-java.ps1
./gradlew.bat build
./gradlew.bat runGameTestServer
# 対象構成を1回ずつ直列実行
./gradlew.bat runGameTestServerIrons
./gradlew.bat runGameTestServerMagic
./gradlew.bat runGameTestServerCreate
# データ生成を変更したとき
# 公式 Builder を使うため、runData は必須依存と Create を読み込む
./gradlew.bat runData
# 人間によるクライアント確認用
./gradlew.bat runClient
# IntelliJ IDEA 実行構成の同期
./gradlew.bat neoForgeIdeSync
```

- Java スクリプトは `JDK21_HOME` またはユーザーの `.jdks` / `.gradle/jdks` を使う。CI は workflow で Java 21 を設定する。
- 通常確認に `clean` は付けない。コード・依存・リソース・ビルド設定変更では `build`、サーバー登録・データ読込・GameTest 変更では `runGameTestServer` も実行する。文書だけならビルド省略理由を報告してよい。
- `build` は `checkTextEncodingHygiene` を含む。同タスクは単独実行も可能。
- テストの追加・整理では [テスト設計方針](docs/testing-policy.md) に従う。設定値の転記より、実際の処理結果・状態遷移・依存との接続を優先して検証する。
- GameTest は専用 world `run/artifices_gametest_clean`（Irons / Magic / Create は末尾に `irons` / `magic` / `create`）を毎回初期化する。手動用 `run/world` は削除しない。同じ run ディレクトリの GameTest は直列実行し、1 回の Gradle 呼び出しにつき GameTest task は 1 つとする。先行プロセスの終了を待ってから次を起動する。
- 実行構成・共通依存変更では標準 / Irons / Magic / Create の GameTest を確認する。`runClientGraphic` は client 専用で GameTest を設けない。描画構成変更時は依存解決と IDE 同期・Sodium 補助 jar の生成を確認し、表示確認は人間へ引き継ぐ。
- CI の GameTest は通常構成の `runGameTestServer` のみとする。Irons / Magic / Create はローカル検証用であり、実行構成の追加だけを理由に CI 対象を増やさない。
- 実行ツールの GameTest 強制終了 timeout は初回から最低 120 秒。yield は終了ではなく wait で追跡する。ツール timeout なら記録を保持し、前回の 2 倍以上で追加実行する。GameTest 自身の test timeout とは区別する。
- 一度でも GameTest が失敗した場合は `report-gametest-failure` Skill で初回失敗と追加実行を最終報告する。
- Codex の通常自動検証は build、必要な GameTest / datagen / Inspection まで。`runClient` や同等の GUI 起動・操作は通常行わない。ユーザーが明示した場合はその範囲で実施できる。起動依頼は自動操作の許可とみなさない。
- client の表示・入力・描画・音声等を変更した場合は `report-client-verification` で必要な手動確認シナリオと確認状態を引き継ぐ。無関係な変更に定型確認を追加しない。人間確認結果の未把握だけで作業を継続しない。

## 実装規約

- クラスは PascalCase、メソッド・変数は camelCase、定数は UPPER_SNAKE_CASE、リソース ID は snake_case。
- Java の型参照は import と単純名を使う。名前衝突で必要な場合だけ FQCN を使う。変更した Java の未使用 import を整理し、無関係な一括整形をしない。
- 不要な FQCN は共有 IDEA Inspection の `UnnecessaryFullyQualifiedName` で ERROR として確認する。Gradle による強制は行わない。
- 登録は既存の DeferredRegister / EventBus と整合させる。client 専用参照は共通コードから隔離する。外部仕様依存・回避策・順序依存は理由をコメントする。
- データ生成先は `src/generated/resources`。生成 JSON は provider を編集して再生成する。手置きとの重複と旧出力の削除を確認する。Git 管理外 `.cache` にない旧 JSON は `runData` で削除されない場合がある。
- 標準 Provider で表現できる定型リソースは原則 datagen とし、制作物・翻訳等の手管理との判断基準は [リソース管理方針](docs/resource-generation-policy.md) に従う。生成処理は `datagen` パッケージに種類別の Provider として配置する。
- 依存バージョンは `gradle.properties` に集約。必須依存追加時は `src/main/templates/META-INF/neoforge.mods.toml` も更新し、`THIRD_PARTY_NOTICES.md` の追記要否を確認する。
- レビュー重大度、保存済み資産・管理設定の保護、意図したリソース変更の扱いは [レビュー方針](docs/review-policy.md) に従う。

## 変更・PR フロー

1. 変更目的と影響範囲を定め、実装する。
2. staged / unstaged / untracked を確認し、依頼外の整形・削除・文字化けを混ぜない。
3. 影響に合う検証を行い、`review-local-change` でコミット前差分を読み取り専用レビューする。
4. 通常の実装・修正は検証とローカルレビューまで行い、未コミットで人間へ引き渡す。対象差分の人間確認後、コミット実行の明示指示を受けるまで commit / amend を行わない。引き渡した差分は確認・修正・コミットが完了するまで保持する。
5. 機能ブランチ全体と PR 状態は `review-feature-branch`、C2S 入力の変更は `review-client-server-authority` も使う。
6. `main` へはブランチ + PR。バージョン更新を含め merge commit のみ使用する。rebase merge と squash merge は使わない。
7. `PR CI / build` と `PR CI / gametest`、人間の判断を確認する。Codex Cloud は設定・実行を確認できた場合の補助であり、CI を代替しない。

リモート操作は共通ルールと作業時の指示に従う。通常は読み取りとローカル作業までとし、個別作業で許可されたリモート書き込みも、その対象・目的・範囲に限定する。個別の許可を以後の作業の恒常的な許可とみなさない。リポジトリは private を維持する。

GitHub の設定とブランチ保護は [GitHub 設定](docs/github-pr-protection.md) を確認する。required check の変更時は対象 workflow と check context の整合を確認する。実行手順変更ではこのファイル、GitHub 運用変更では設定文書も更新する。事前合意なしの大規模リファクタ、機密情報のコミットは禁止する。

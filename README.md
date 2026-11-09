# Artifices Assortie

## 概要

Iron's Arms 'n Artifice用の小さなアドオンMODです.

### ブランチとサポート方針

- 開発ブランチ `main` では Java 21 を使用します。
- `main` は Minecraft 1.21.1 / NeoForge の主要開発ブランチです。
- 現在、Minecraft 1.20.1 / Forge の保守・backport 運用は設けていません。
- Minecraft 26.1.2 / NeoForge の対応予定はありません。
- `build.gradle` では Java toolchain を 21 に固定していますが、`gradlew.bat` 自体が使う JVM は `JAVA_HOME` または `PATH` に依存します。

## 導入方法

- `mods`配下に`jar`を入れればOKです。
- 現時点でIron's Arms 'n Artifice及びそれの前提MOD以外の前提MODはありません.

## 開発環境

- Minecraft 1.21.1 / NeoForge 21.1.265 / Java 21
- MOD ID: `artificesassortie`
- バージョン・依存の正は `gradle.properties` と `build.gradle` です。

PowerShell では `JDK21_HOME` を設定するか、JDK 21 を `%USERPROFILE%\.jdks` または `%USERPROFILE%\.gradle\jdks` に配置します。

```powershell
.\scripts\use-java.ps1
./gradlew.bat build
./gradlew.bat runGameTestServer
```

データ生成は `./gradlew.bat runData`、人間による client 確認は `./gradlew.bat runClient`、IDE 実行構成の同期は `./gradlew.bat neoForgeIdeSync` を使います。client の表示・操作は GameTest では検証できません。

現在はサンプルプロジェクト段階で、Iron's Arms 'n Artifice の依存宣言、GameTest の実装と専用 world の初期化、datagen Provider は未整備です。上記の実行構成が存在することと、アドオン機能を検証済みであることは区別してください。Iron's Spells 'n Spellbooks は現時点で依存に含めません。

`build` では UTF-8・BOM・明らかな文字化けの検査も行います。文書・翻訳の変更時は `./gradlew.bat checkTextEncodingHygiene` で単独実行できます。

開発時は [AGENTS.md](AGENTS.md)、[テスト設計・整理方針](docs/testing-policy.md)、[リソース管理方針](docs/resource-generation-policy.md)、[レビュー方針](docs/review-policy.md) を参照してください。テストは処理結果・状態遷移・依存との接続を優先し、機能単位に配置します。

### MOD を追加した実行構成

現在、optional MOD を追加する専用構成はありません。Iron's Arms 'n Artifice は必須依存として標準構成へ組み込む方針です。追加構成は連携機能を実装した時点で整備します。

## PR 運用

`main` への取り込みはブランチと PR を使い、バージョン更新を含め merge commit に統一します。rebase merge と squash merge は使用しません。

通常の実装・修正は自動検証とローカルレビューを終えた未コミット差分で人間へ引き渡し、人間確認後の明示指示を受けてコミットします。このローカル環境からリモートへの書き込みは行いません。

CI は `PR CI / build` と `PR CI / gametest` を確認する方針です。GitHub 側の設定・適用状況は [GitHub PR・Actions 設定](docs/github-pr-protection.md) を参照してください。

## ライセンスや使用について

### 許可(Permissions)

- modpackにはご自由にどうぞ
- 前提modにするのもご自由にどうぞ
- リソースパックもご自由にどうぞ
- スクリーンショット、プレイ動画、動画配信も収益化含めご自由にどうぞ

### 禁止(Restrictions)

- `THIRD_PARTY_NOTICES.md`にある素材のライセンスを破るのはやめてください

### License

- Code: MIT (see `LICENSE`).
- Original assets created for this project: CC0-1.0 (free to use).
- Third-party assets: see `THIRD_PARTY_NOTICES.md` for the applicable licenses.

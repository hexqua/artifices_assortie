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

必須依存は Iron's Arms 'n Artifice / GeckoLib / Iron's Lib、任意依存は Create / Iron's Spells 'n Spellbooks / Malum です。実行構成の MOD ロードと別構成からの混入は GameTest で確認し、機能・連携の検証は対象の挙動テストで行います。

`build` では UTF-8・BOM・明らかな文字化けの検査も行います。文書・翻訳の変更時は `./gradlew.bat checkTextEncodingHygiene` で単独実行できます。

開発時は [AGENTS.md](AGENTS.md)、[テスト設計・整理方針](docs/testing-policy.md)、[リソース管理方針](docs/resource-generation-policy.md)、[レビュー方針](docs/review-policy.md) を参照してください。テストは処理結果・状態遷移・依存との接続を優先し、機能単位に配置します。

### MOD を追加した実行構成

| client | GameTest | 追加する開発用 MOD |
| --- | --- | --- |
| `runClient` | `runGameTestServer` | 共通構成のみ |
| `runClientGraphic` | なし | Sodium / Sodium Dynamic Lights / Chloride / Reese's Sodium Options |
| `runClientIrons` | `runGameTestServerIrons` | Iron's Spells 'n Spellbooks / Iron's Gems 'n Jewelry / Curios / playerAnimator / Atlas API |
| `runClientMagic` | `runGameTestServerMagic` | Irons 構成一式 + Malum 1.8.2 / Lodestone 1.8.2（Curios は Irons 構成で導入） |
| `runClientCreate` | `runGameTestServerCreate` | Create（Registrate / Flywheel / Ponder は配布 jar に同梱） |

すべての client と GameTest の共通構成は、必須依存3件に JEI / Jade / MmmMmmMmmMmm（Target Dummy）と前提の MezzConfig / Moonlight Lib を加えたものです。JEI・Jade・Target Dummy は開発用であり、本 MOD の任意依存宣言には含めません。通常の `runServer` は必須依存のみ、`runData` は必須依存と Create / Irons 構成を読み込んで起動します。

レシピは通常用と連携用を `runData` でまとめて生成し、導入されている MOD に応じてロード条件で切り替えます。

Iron's Spells 'n Spellbooks は Irons / Magic、Malum は Magic 構成で任意連携を確認します。Iron's Gems 'n Jewelry と開発用の前提 MOD は本 MOD の依存宣言には含めません。Create は任意依存として扱います。

描画構成は Constellation 'n Spells と同じ Sodium 0.8.13+mc1.21.1 と補助 MOD の組み合わせを使用します。開発環境で Sodium 本体を正しく探索するため補助 jar を生成し、`neoForgeIdeSync` と起動準備から参照します。旧 Sodium Options API は追加しません。

GameTest は専用 world `run/artifices_gametest_clean`、`run/artifices_gametest_cleanirons`、`run/artifices_gametest_cleanmagic`、`run/artifices_gametest_cleancreate` を各実行前に初期化します。手動確認用の `run/world` は保持します。同じ run ディレクトリを使うため、GameTest は次のように1回ずつ直列に実行してください。

```powershell
./gradlew.bat runGameTestServer
./gradlew.bat runGameTestServerIrons
./gradlew.bat runGameTestServerMagic
./gradlew.bat runGameTestServerCreate
```

テストクラスと template は配布 jar から除外します。client の表示・操作・描画確認は人間が対応する構成を起動して行います。

## PR 運用

`main` への取り込みはブランチと PR を使い、バージョン更新を含め merge commit に統一します。rebase merge と squash merge は使用しません。

通常の実装・修正は自動検証とローカルレビューを終えた未コミット差分で人間へ引き渡し、人間確認後の明示指示を受けてコミットします。リモート操作の可否は開発環境の共通ルールと作業時の指示に従います。

CI は `PR CI / build` と `PR CI / gametest` を確認する方針です。GameTest の CI 対象は通常構成の `runGameTestServer` のみで、Irons / Magic / Create の追加構成はローカル検証用です。GitHub 側の設定・適用状況は [GitHub PR・Actions 設定](docs/github-pr-protection.md) を参照してください。

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

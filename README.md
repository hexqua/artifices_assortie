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

### MOD を追加した実行構成

(対応予定)

## PR 運用

(対応予定)

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

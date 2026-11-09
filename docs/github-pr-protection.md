# GitHub PR・Actions 設定

対象は `hexqua/artifices_assortie`。リポジトリは private を維持する。設定は GitHub API で照合し、運用を変更した際は本書も更新する。

## リポジトリ設定

- 開発基準・既定ブランチは `main`。
- merge commit のみ許可する。バージョン更新を含め rebase merge と squash merge は使用しない。
- マージ後の作業ブランチ自動削除は有効、auto merge は無効。

## Actions と CI

- Actions は有効。GitHub 所有 action と `gradle/actions/wrapper-validation@50e97c2cd7a37755bbfafc9c5b7cafaece252f6e` を許可し、verified creator の一括許可は無効とする。
- action は完全 SHA 固定を必須とする。wrapper 検証 action の SHA を更新するときは、workflow と GitHub 側の許可リストを同時に更新する。
- 既定 workflow 権限は read。Actions による PR 作成・承認は許可しない。
- `.github/workflows/pr-ci.yml` は `main` 向けの `pull_request` で `build` と `gametest` を独立実行する。`pull_request_target` は使用しない。
- Java 21、secrets 不使用、`contents: read`、checkout 認証情報の非永続化、Gradle wrapper 検証を維持する。
- `gametest` は通常構成の `runGameTestServer` のみ実行する。Irons / Create はローカル検証用、Graphic は client 専用とする。
- `gametest.runtime` のロード・混入確認と、機能・連携の挙動テストは保証範囲を区別する。

## main の保護ルール

- branch ruleset `main-pr-ci-protection` を `refs/heads/main` へ適用し、enforcement を Active とする。
- 削除・force push を禁止し、取り込みには PR を必須とする。
- required checks は `build` / `gametest`、実行元は GitHub Actions。表示名の `PR CI /` は check context に含めない。最新 base への追従を必須とする。
- PR の許可マージ方式は merge commit のみ。必須 approval 数は 0 とし、人間の判断は運用で確認する。
- stale approval の破棄、code owner review、last push approval、conversation resolution は要求しない。
- 管理者バイパスは For pull requests only に限定し、直接 push のバイパスを許可しない。通常の取り込みではバイパスを使わず、CI と人間確認を終える。
- linear history、更新者の制限、署名・deployment・merge queue の必須化は行わない。

## PR の取り込み

1. `main` への取り込みは PR 経由とし、直接 push・force push・削除を行わない。
2. PR の最新コミットについて `PR CI / build` と `PR CI / gametest` の成功を確認する。過去のコミットの成功を代用しない。
3. 最新の `main` との整合と人間の確認を終えてから merge commit で取り込む。CI の失敗・中断・待機中を成功と扱わない。

required checks の追加・改名時は、対象 workflow が `main` に存在し、その context を実際に報告することを確認してから保護設定を変更する。

## 設定変更とレビュー

設定の確認には GitHub 連携または `gh api` を使う。`gh` がサンドボックス内で利用できない場合は、操作内容と目的を示して権限昇格を要求する。書き込みは共通ルールと作業時の指示で許可された対象・範囲に限定する。

Codex Cloud 等の外部レビューは、対象リポジトリへのアクセスと実行結果を確認したうえで CI と人間の判断の補助として使う。レビューの優先度はそのまま採用せず、[レビュー方針](review-policy.md) の具体的な実害に基づいて再評価する。

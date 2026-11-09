# GitHub PR・Actions 設定

Constellation 'n Spells の運用方針を基に、Artifices Assortie 向けに整理する。本書の「方針」と GitHub 側の「適用済み設定」は区別する。

## マージと CI の方針

- `main` への取り込みは PR を使い、バージョン更新を含め merge commit のみとする。rebase merge と squash merge は使用しない。
- `.github/workflows/pr-ci.yml` は `main` 向けの `pull_request` で `build` と `gametest` を独立実行する。`pull_request_target` は使用しない。
- Java 21、secrets 不使用、`contents: read`、checkout 認証情報の非永続化、Gradle wrapper 検証、action の完全 SHA 固定を維持する。
- GameTest 未実装の間は、`gametest` ジョブが存在することを機能の検証済みという意味にしない。GameTest の実装・専用 world の設定後に、その実行対象と結果を確認する。

## GitHub 側で確認する設定

以下は目標とする設定であり、今回 GitHub API で適用状況は確認していない。

- merge commit のみ許可、マージ後の作業ブランチ自動削除を有効、auto merge を無効。
- Actions は必要な GitHub 所有 action と Gradle wrapper 検証 action に限定し、完全 SHA 固定を必須とする。
- 既定 workflow 権限は read、Actions による PR 作成・承認は許可しない。
- `main` の削除・force push を禁止し、PR と最新 base への追従を必須にする。
- required checks は GitHub Actions の `build` / `gametest`。表示名の `PR CI /` は context に含めない。
- 必須 approval 数は 0 を基準とし、人間の判断を運用で確認する。管理者のバイパスを設定する場合も PR 経由に限定する。

参照元でのプラン制約・403 応答は本リポジトリの状態を示さない。実際に設定できない場合は、その制約を確認したうえで PR・CI の確認を運用で守る。リポジトリの公開範囲を変更する根拠にはしない。

## workflow 導入と保護設定

設定の反映は人間が行う。このローカル環境では push、PR 作成・更新、GitHub 設定変更、CI の手動実行を行わない。

1. 人間が workflow を含む `main` 向け PR を反映し、`PR CI / build` と `PR CI / gametest` の実行結果・検証範囲を確認する。
2. merge commit で取り込み、`main` 上に workflow が存在することを確認する。
3. 保護機能の利用可否と check context を確認してから required checks 等を設定する。

workflow の導入・check の報告前に required checks を有効化すると、未報告の check 待ちでマージできなくなる場合がある。

## 外部レビュー

Codex Cloud のアクセス・レビュー設定・実行結果は未確認。利用する場合も CI と人間の判断の補助とし、未確認を実施済みと扱わない。外部レビューの優先度はそのまま採用せず、[レビュー方針](review-policy.md) の具体的な実害に基づいて再評価する。

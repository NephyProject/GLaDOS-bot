# GLaDOS-bot
<center>
    <img src="https://cdn.discordapp.com/avatars/292673941057568769/360959a4a7af21cdfe4dc30b6767c915.png?size=128" alt="GLaDOS#0316">
    <p>GLaDOS-Bot は <a href="https://nephy.jp/discord" target="_blank">かいげんDiscord</a> 等で稼働している Discord Botです。</p>
</center>

## 実装済み機能

- [x] MusicBot
    - [x] NowPlaying機能
    - [x] 絵文字で直感的なプレイヤー操作
    - [x] 再生可能動画ソース: YouTube, ニコニコ動画, SoundCloud, Twitch, Vimeo, Bandcamp, Beam, ...
    - [x] ニコ動, SoundCloudのランキングを自動再生 / オートプレイリストの実装
    - [x] 動画URLを検出して再生するか尋ねるプロンプト
- [x] コマンド
    - [x] play
        - 検索ワードまたはURLを指定することで再生します。
    - [x] soundcloud (sc), nico
        - SoundCloudまたはニコニコ動画のランキングをまとめて再生キューに追加します。
    - [x] summon
        - 参加中のVCにGLaDOSを呼びます。
    - [x] 管理用コマンド (eval, evalk, icon, ping, purge, stop, ...)
- [x] 身内ネタ
    - [x] 誕生日お祝い機能
    - [x] Steamの所有ゲーム別のゲームロールの付与 / 同期
    - [x] `#こんなさめかんはいやだ` との連携 / TwitterからGLaDOSの操作
    - [x] No Mute (ミュート禁止) ボイスチャンネル
    - [x] 陰キャ (マイクミュート者に自動付与) ロール
    - [x] `xxx #とは` でニコニコ大百科の記事を参照
- [x] その他
    - [x] プレイ中の表示は Test Chamber 1 ~ 21でランダムに変わります
- [x] 拡張性の高いプラグインベースの機能追加
    - `jp.nephy.glados.feature` 以下のクラスを動的にロードし機能追加できます。

## TODO

- [ ] Google Cloud Speech APIを使って いろいろしたい (OK, GLaDOS!)
- [ ] Steamキーの配布機能, Giveaway

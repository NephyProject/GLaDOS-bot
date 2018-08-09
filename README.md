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

- [ ] Google Cloud Speech APIを使って いろいろしたい (Ok, GLaDOS!)
- [ ] Steamキーの配布機能, Giveaway

## 使用法
GLaDOSをご利用いただいているサーバでは 以下のようなConfigを提供していただければ機能を追加できます。

```kotlin
val parameter = GLaDOSParameter(
        ownerId = 10000000000000, // Your discord id
        primaryCommandPrefix = "!!",
        secondaryCommandPrefix = "!",

        // 0 ~ 100%の間で設定します
        defaultPlayerVolume = 40,
        // NowPlayingメッセージを更新する間隔(ミリ秒)
        nowPlayingUpdateMs = 5000L
)

val secret = SecretConfig(
        niconicoLoginEmail = "user@example.com",
        niconicoLoginPassword = "your niconico password",

        soundCloudClientId = "***",
        googleApiKey = "***",
        steamApiKey = "***"
)

val config = GLaDOSConfig(
        token = "your bot token",
        clientId = "your bot client id",

        guilds = listOf(
                GuildConfig(
                        // Kaigen Discord
                        id = 187578406940966912,
                        isMain = true,

                        role = RoleConfig(
                                admin = 310600829000220673,
                                inkya = 332881733437620224
                        ),
                        voiceChannel = VoiceChannelConfig(
                                general = 187578406940966913,
                                noMute = 431457655865868288
                        ),
                        textChannel = TextChannelConfig(
                                general = 187578406940966912,
                                bot = 294505863631077379,
                                log = 294373627837808640,
                                rules = 211665577989636097,
                                iHateSuchKashiwa = 367991669032747010,
                                giveaway = 278454146867134464
                        ),
                        option = GuildOption(
                                useCommand = true,
                                useFindVideoURL = true,
                                useNiconicoDict = true,
                                clientToken = "mfa.***",
                                steamGameRoles = listOf(
                                        SteamGameRoleConfig(
                                                appId = 578080,
                                                name = "[Game] PUBG"
                                        ),
                                        SteamGameRoleConfig(
                                                appId = 730,
                                                name = "[Game] CS:GO"
                                        ),
                                        SteamGameRoleConfig(
                                                appId = 282800,
                                                name = "[Game] 100% Orange Juice"
                                        ),
                                        SteamGameRoleConfig(
                                                appId = 528510,
                                                name = "[Game] Turbo Pug 3D"
                                        ),
                                        SteamGameRoleConfig(
                                                appId = 495890,
                                                name = "[Game] Montaro"
                                        )
                                )
                        )
                )
        )
)

GLaDOS(config, parameter, secret)
```

# GLaDOS-bot
<p align="center">
  <img src="https://cdn.discordapp.com/avatars/292673941057568769/360959a4a7af21cdfe4dc30b6767c915.png?size=128" alt="GLaDOS#0316">
</p>

GLaDOS-Bot は [かいげん Discord](https://nephy.jp/discord) 等で稼働している Discord Bot です。


独自のクラスローダを内蔵しており, 容易に機能の追加を行えるようにしています。


このリポジトリには プラグインを含んでいません。プラグイン集は [こちら](https://github.com/NephyProject/GLaDOS-bot-plugins) からどうぞ。

## Plugin API
[HackMD](https://hackmd.io/5URKBTt6Q02L3FneACyH9A) で公開しています。

誰でもプラグインを作成して機能を追加できます。

## Build
Gradle のマルチプロジェクトに対応しているため 依存関係を分離できます。

Gradle の subproject に `fatJar` タスクが追加されるので これを実行することでプラグイン同梱のJarを作成できます。

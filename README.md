# 日本語日付（JpDate Overlay）

中華タブレットなどでシステム日付が英語表示のとき、画面の好きな位置に **日本語の年月日** を常時表示する小さなアプリです。

## できること

- 透明〜半透明のオーバーレイで日付を常時表示
- **ドラッグで任意の位置へ移動**（位置は保存される）
- 表示例: `2026年10月28日(水)` / `10月28日(水)` / `10月28日`
- 文字サイズ・文字の濃さ・背景の濃さを調整可能
- 端末再起動後の自動開始（任意）
- LogTelop とは独立した別アプリ

## 使い方

1. APK をインストール
2. アプリを開く
3. 「他のアプリの上に表示する許可」をON
4. 「日付を表示する」をON
5. 画面上の日付を指でドラッグして位置を決める

通知の「停止」から表示を止められます。

## ビルド

Android Studio または:

```bash
./gradlew :app:assembleDebug
```

生成物: `app/build/outputs/apk/debug/app-debug.apk`

## 権限

- `SYSTEM_ALERT_WINDOW` … 他アプリの上に表示
- `FOREGROUND_SERVICE` … 常駐表示の維持
- `POST_NOTIFICATIONS` … 常駐通知（Android 13+）
- `RECEIVE_BOOT_COMPLETED` … 起動時の自動開始（任意）

## バージョン

- 1.0.0 (build 1)

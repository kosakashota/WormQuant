# WormQuant

指定ディレクトリ内のTIFF画像を一括で計測し、ファイル名・LeicaメタデータXML由来のパラメータとともに結果をCSVへ出力するImageJプラグイン。

## 使い方

Fiji / ImageJ のメニューからは Plugins > WormQuant > Quantification Helper で起動する。

追記予定

## インストール

### 自分でビルド

```bash
./gradlew jar         # JAR のみ出力（build/libs/ 直下に出力される）
./gradlew copyToFiji  # ビルドして Fiji の plugins へコピー（コピー先をgradle.propertiesのfiji.dirで指定すること）
./gradlew runFiji     # ビルド・コピー後に Fiji を起動
./gradlew runPlugin   # ImageJ と本プラグインを直接起動（開発用）
```
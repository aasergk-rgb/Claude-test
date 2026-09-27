# Room / Compose は各ライブラリが consumer rules を同梱している。

# ウィジェットのボタン処理（ActionCallback）は Glance がクラス名から生成するため、縮小で消さない
-keep class * implements androidx.glance.appwidget.action.ActionCallback { <init>(); }

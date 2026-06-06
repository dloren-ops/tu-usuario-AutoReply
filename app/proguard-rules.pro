# Keep Room entities and generated code
-keep class com.autoreply.bot.data.local.** { *; }

# Keep the NotificationListenerService (referenced from manifest)
-keep class com.autoreply.bot.service.AutoReplyNotificationListener { *; }

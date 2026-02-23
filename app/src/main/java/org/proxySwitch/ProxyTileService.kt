package org.proxySwitch

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.io.DataOutputStream

class ProxyTileService : TileService() {

	companion object {
		const val NOTIF_CHANNEL_ID = "proxy_status_channel"
		const val NOTIF_ID = 999
	}

	override fun onStartListening() {
		super.onStartListening()

		val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
		val enabled = prefs.getBoolean(PREF_PROXY_ENABLED, false)

		qsTile?.let {
			it.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
			it.label = if (enabled) "代理开启" else "代理关闭"
			it.updateTile()
		}
	}

	override fun onClick() {
		super.onClick()

		val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
		val ip = prefs.getString(PREF_IP, "")
		val port = prefs.getInt(PREF_PORT, 0)

		val tile = qsTile
		val isActive = tile.state == Tile.STATE_ACTIVE

		try {
			if (isActive) {
				// === 关闭代理 ===
				runShell("settings put global http_proxy :0")

				prefs.edit().putBoolean(PREF_PROXY_ENABLED, false).apply()

				tile.state = Tile.STATE_INACTIVE
				tile.label = "代理关闭"
				tile.updateTile()

				cancelProxyNotification()

			} else {
				if (!ip.isNullOrEmpty() && port != 0) {
					// === 开启代理 ===
					runShell("settings put global http_proxy $ip:$port")

					prefs.edit().putBoolean(PREF_PROXY_ENABLED, true).apply()

					tile.state = Tile.STATE_ACTIVE
					tile.label = "代理开启"
					tile.updateTile()

					showProxyNotification(ip, port)
				} else {
					Toast.makeText(this, "请先在主界面配置 IP 和端口", Toast.LENGTH_SHORT).show()
				}
			}
		} catch (e: Exception) {
			e.printStackTrace()
			Toast.makeText(this, "执行失败，请确认 Root 权限", Toast.LENGTH_SHORT).show()
		}
	}

	private fun runShell(cmd: String) {
		val su = Runtime.getRuntime().exec("su")
		val os = DataOutputStream(su.outputStream)
		os.writeBytes("$cmd\n")
		os.writeBytes("exit\n")
		os.flush()
		su.waitFor()
	}

	// ===== 通知 =====

	private fun showProxyNotification(ip: String, port: Int) {
		val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val channel = NotificationChannel(
				NOTIF_CHANNEL_ID,
				"代理状态",
				NotificationManager.IMPORTANCE_LOW
			)
			nm.createNotificationChannel(channel)
		}

		val intent = Intent(this, MainActivity::class.java)
		val pendingIntent = PendingIntent.getActivity(
			this, 0, intent,
			PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		)

		val notif = NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
			.setSmallIcon(android.R.drawable.ic_dialog_alert)
			.setContentTitle("代理已开启")
			.setContentText("$ip:$port")
			.setOngoing(true)
			.setContentIntent(pendingIntent)
			.build()

		nm.notify(NOTIF_ID, notif)
	}

	private fun cancelProxyNotification() {
		val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		nm.cancel(NOTIF_ID)
	}
}

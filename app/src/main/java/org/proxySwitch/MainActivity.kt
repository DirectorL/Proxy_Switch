package org.proxySwitch

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.io.DataOutputStream

const val PREF_NAME = "proxy_prefs"
const val PREF_IP = "ip"
const val PREF_PORT = "port"
const val PREF_PROXY_ENABLED = "proxy_enabled"

class MainActivity : ComponentActivity() {

	private lateinit var prefs: SharedPreferences

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)

		setContent {

			var ip by remember { mutableStateOf(prefs.getString(PREF_IP, "") ?: "") }
			var port by remember { mutableStateOf(prefs.getInt(PREF_PORT, 0).takeIf { it != 0 }?.toString() ?: "") }

			val context = this

			ProxyAppUI(
				ip = ip,
				port = port,
				onIpChange = { ip = it },
				onPortChange = { port = it },
				onSave = {

					if (ip.isBlank() || port.isBlank()) {
						Toast.makeText(context, "请填写完整 IP 和端口", Toast.LENGTH_SHORT).show()
						return@ProxyAppUI
					}

					val portNum = port.toIntOrNull()
					if (portNum == null) {
						Toast.makeText(context, "端口必须是数字", Toast.LENGTH_SHORT).show()
						return@ProxyAppUI
					}

					prefs.edit()
						.putString(PREF_IP, ip)
						.putInt(PREF_PORT, portNum)
						.apply()

					Toast.makeText(context, "配置已保存，下拉按钮可直接启用代理", Toast.LENGTH_SHORT).show()
				},
				onForceDisable = {

					disableProxyByRoot()

					prefs.edit().putBoolean(PREF_PROXY_ENABLED, false).apply()

					val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
					nm.cancel(ProxyTileService.NOTIF_ID)

					Toast.makeText(context, "代理已关闭", Toast.LENGTH_SHORT).show()
				}
			)
		}
	}
}

/**
 * 使用 root 强制关闭系统代理
 */
fun disableProxyByRoot() {
	try {
		val su = Runtime.getRuntime().exec("su")
		val os = DataOutputStream(su.outputStream)
		os.writeBytes("settings put global http_proxy :0\n")
		os.writeBytes("exit\n")
		os.flush()
		su.waitFor()
	} catch (e: Exception) {
		e.printStackTrace()
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxyAppUI(
	ip: String,
	port: String,
	onIpChange: (String) -> Unit,
	onPortChange: (String) -> Unit,
	onSave: () -> Unit,
	onForceDisable: () -> Unit
) {

	Surface(
		modifier = Modifier.fillMaxSize(),
		color = MaterialTheme.colorScheme.background
	) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(24.dp),
			verticalArrangement = Arrangement.Top
		) {

			Text(
				text = "代理配置",
				style = MaterialTheme.typography.titleLarge,
				modifier = Modifier.padding(bottom = 20.dp)
			)

			OutlinedTextField(
				value = ip,
				onValueChange = onIpChange,
				label = { Text("代理 IP") },
				modifier = Modifier
					.fillMaxWidth()
					.padding(bottom = 12.dp)
			)

			OutlinedTextField(
				value = port,
				onValueChange = onPortChange,
				label = { Text("端口") },
				singleLine = true,
				modifier = Modifier
					.fillMaxWidth()
					.padding(bottom = 24.dp)
			)

			Button(
				onClick = onSave,
				modifier = Modifier.fillMaxWidth()
			) {
				Text("保存配置")
			}

			Spacer(modifier = Modifier.height(16.dp))

			Button(
				onClick = onForceDisable,
				modifier = Modifier.fillMaxWidth(),
				colors = ButtonDefaults.buttonColors(
					containerColor = MaterialTheme.colorScheme.error
				)
			) {
				Text("强制关闭代理")
			}
		}
	}
}

@Preview(showBackground = true)
@Composable
fun ProxyPreview() {
	ProxyAppUI(
		ip = "192.168.1.1",
		port = "8080",
		onIpChange = {},
		onPortChange = {},
		onSave = {},
		onForceDisable = {}
	)
}
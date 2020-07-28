package id.galeka.sms

import EventChannel.StreamHandler
import android.Manifest
import android.content.pm.PackageManager
import id.galeka.sms.permisions.Permissions
import id.galeka.sms.telephony.TelephonyManager
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal class SimCardsHandler(registrar: PluginRegistry.Registrar, result: MethodChannel.Result) : PluginRegistry.RequestPermissionsResultListener {
    private val permissionsList = arrayOf(Manifest.permission.READ_PHONE_STATE)
    private val registrar: PluginRegistry.Registrar
    private val result: MethodChannel.Result
    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>?, grantResults: IntArray): Boolean {
        if (requestCode != Permissions.READ_PHONE_STATE) {
            return false
        }
        var isOk = true
        for (res in grantResults) {
            if (res != PackageManager.PERMISSION_GRANTED) {
                isOk = false
                break
            }
        }
        if (isOk) {
            simCards
            return true
        }
        result.error("#01", "permission denied", null)
        return false
    }

    fun handle(permissions: Permissions) {
        if (permissions.checkAndRequestPermission(permissionsList, Permissions.READ_PHONE_STATE)) {
            simCards
        }
    }

    private val simCards: Unit
        private get() {
            val simCards = JSONArray()
            try {
                val telephonyManager = TelephonyManager(registrar.context())
                val phoneCount = telephonyManager.simCount
                for (i in 0 until phoneCount) {
                    val simCard = JSONObject()
                    simCard.put("slot", i + 1)
                    simCard.put("imei", telephonyManager.getSimId(i))
                    simCard.put("state", telephonyManager.getSimState(i))
                    simCards.put(simCard)
                }
            } catch (e: JSONException) {
                e.printStackTrace()
                result.error("2", e.message, null)
                return
            }
            result.success(simCards)
        }

    init {
        this.registrar = registrar
        this.result = result
    }
}

internal class SimCardsProvider(registrar: PluginRegistry.Registrar) : MethodChannel.MethodCallHandler {
    private val permissions: Permissions
    private val registrar: PluginRegistry.Registrar
    fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        if (!call.method.equals("getSimCards")) {
            result.notImplemented()
        } else {
            getSimCards(result)
        }
    }

    private fun getSimCards(result: MethodChannel.Result) {
        val handler = SimCardsHandler(registrar, result)
        registrar.addRequestPermissionsResultListener(handler)
        handler.handle(permissions)
    }

    init {
        this.registrar = registrar
        permissions = Permissions(registrar.activity())
    }
}
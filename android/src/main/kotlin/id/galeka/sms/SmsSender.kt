package id.galeka.sms

import EventChannel.StreamHandler
import android.Manifest
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import id.galeka.sms.permisions.Permissions
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.PluginRegistry.Registrar
import io.flutter.plugin.common.PluginRegistry.RequestPermissionsResultListener
import java.util.*

/**
 * Created by babariviere on 08/03/18.
 */
@TargetApi(Build.VERSION_CODES.DONUT)
internal class SmsSenderMethodHandler(registrar: Registrar, result: MethodChannel.Result, address: String, body: String, sentId: Int, subId: Int?) : RequestPermissionsResultListener {
    private val permissionsList = arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE)
    private val result: MethodChannel.Result
    private val address: String
    private val body: String
    private val sentId: Int
    private val subId: Int?
    private val registrar: Registrar
    fun handle(permissions: Permissions) {
        if (permissions.checkAndRequestPermission(permissionsList, Permissions.SEND_SMS_ID_REQ)) {
            sendSmsMessage()
        }
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>?, grantResults: IntArray): Boolean {
        if (requestCode != Permissions.SEND_SMS_ID_REQ) {
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
            sendSmsMessage()
            return true
        }
        result.error("#01", "permission denied for sending sms", null)
        return false
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun sendSmsMessage() {
        val sentIntent = Intent("SMS_SENT")
        sentIntent.putExtra("sentId", sentId)
        val sentPendingIntent = PendingIntent.getBroadcast(
                registrar.context(),
                0,
                sentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        )
        val deliveredIntent = Intent("SMS_DELIVERED")
        deliveredIntent.putExtra("sentId", sentId)
        val deliveredPendingIntent = PendingIntent.getBroadcast(
                registrar.context(),
                UUID.randomUUID().hashCode(),
                deliveredIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        )
        val sms: SmsManager
        sms = if (subId == null) {
            SmsManager.getDefault()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                SmsManager.getSmsManagerForSubscriptionId(subId)
            } else {
                result.error("#03", "this version of android does not support multicard SIM", null)
                return
            }
        }
        sms.sendTextMessage(address, null, body, sentPendingIntent, deliveredPendingIntent)
        result.success(null)
    }

    companion object {
        private val sms = SmsManager.getDefault()
    }

    init {
        this.registrar = registrar
        this.result = result
        this.address = address
        this.body = body
        this.sentId = sentId
        this.subId = subId
    }
}

@TargetApi(Build.VERSION_CODES.DONUT)
internal class SmsSender(registrar: Registrar) : MethodCallHandler {
    private val registrar: Registrar
    private val permissions: Permissions
    fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        if (call.method.equals("sendSMS")) {
            val address: String = call.argument("address").toString()
            val body: String = call.argument("body").toString()
            val sentId: Int = call.argument("sentId")
            val subId: Int = call.argument("subId")
            if (address == null) {
                result.error("#02", "missing argument 'address'", null)
            } else if (body == null) {
                result.error("#02", "missing argument 'body'", null)
            } else {
                val handler = SmsSenderMethodHandler(registrar, result, address, body, sentId, subId)
                registrar.addRequestPermissionsResultListener(handler)
                handler.handle(permissions)
            }
        } else {
            result.notImplemented()
        }
    }

    init {
        this.registrar = registrar
        permissions = Permissions(registrar.activity())
    }
}
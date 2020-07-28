package id.galeka.sms

import EventChannel.StreamHandler
import android.Manifest
import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.util.Log
import id.galeka.sms.permisions.Permissions
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler
import io.flutter.plugin.common.PluginRegistry.Registrar
import io.flutter.plugin.common.PluginRegistry.RequestPermissionsResultListener
import org.json.JSONObject
import java.util.*

/**
 * Created by babariviere on 08/03/18.
 */
internal class SmsReceiver(registrar: Registrar) : StreamHandler, RequestPermissionsResultListener {
    private val registrar: Registrar
    private var receiver: BroadcastReceiver? = null
    private val permissions: Permissions
    private val permissionsList = arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
    private var sink: EventSink? = null

    @TargetApi(Build.VERSION_CODES.KITKAT)
    fun onListen(arguments: Any?, events: EventSink) {
        receiver = createSmsReceiver(events)
        registrar.context().registerReceiver(receiver, IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION))
        sink = events
        permissions.checkAndRequestPermission(permissionsList, Permissions.RECV_SMS_ID_REQ)
    }

    fun onCancel(o: Any?) {
        registrar.context().unregisterReceiver(receiver)
        receiver = null
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun readMessages(intent: Intent): Array<SmsMessage> {
        return Telephony.Sms.Intents.getMessagesFromIntent(intent)
    }

    private fun createSmsReceiver(events: EventSink): BroadcastReceiver {
        return object : BroadcastReceiver() {
            @TargetApi(Build.VERSION_CODES.KITKAT)
            override fun onReceive(context: Context, intent: Intent) {
                try {
                    val msgs = readMessages(intent) ?: return
                    val obj = JSONObject()
                    obj.put("address", msgs[0].originatingAddress)
                    obj.put("date", Date().time)
                    obj.put("date_sent", msgs[0].timestampMillis)
                    obj.put("read", if (msgs[0].statusOnIcc == SmsManager.STATUS_ON_ICC_READ) 1 else 0)
                    obj.put("thread_id", TelephonyCompat.getOrCreateThreadId(context, msgs[0].originatingAddress))
                    var body = ""
                    for (msg in msgs) {
                        body = body + msg.messageBody
                    }
                    obj.put("body", body)
                    events.success(obj)
                } catch (e: Exception) {
                    Log.d("SmsReceiver", e.toString())
                }
            }
        }
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>?, grantResults: IntArray): Boolean {
        if (requestCode != Permissions.RECV_SMS_ID_REQ) {
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
            return true
        }
        sink.endOfStream()
        return false
    }

    init {
        this.registrar = registrar
        permissions = Permissions(registrar.activity())
        registrar.addRequestPermissionsResultListener(this)
    }
}
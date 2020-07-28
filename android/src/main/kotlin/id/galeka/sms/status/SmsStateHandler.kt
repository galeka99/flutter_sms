package id.galeka.sms.status

import EventChannel.StreamHandler
import android.Manifest
import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import id.galeka.sms.permisions.Permissions
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.PluginRegistry

/**
 * Created by Joan Pablo on 4/17/2018.
 */
class SmsStateHandler(registrar: PluginRegistry.Registrar) : StreamHandler, PluginRegistry.RequestPermissionsResultListener {
    private var smsStateChangeReceiver: BroadcastReceiver? = null
    private val registrar: PluginRegistry.Registrar
    private val permissions: Permissions
    var eventSink: EventChannel.EventSink? = null
    fun onListen(o: Any?, eventSink: EventChannel.EventSink) {
        this.eventSink = eventSink
        smsStateChangeReceiver = SmsStateChangeReceiver(eventSink)
        if (permissions.checkAndRequestPermission(arrayOf(Manifest.permission.RECEIVE_SMS),
                        Permissions.BROADCAST_SMS)) {
            registerDeliveredReceiver()
            registerSentReceiver()
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun registerDeliveredReceiver() {
        registrar.context().registerReceiver(
                smsStateChangeReceiver,
                IntentFilter("SMS_DELIVERED"))
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun registerSentReceiver() {
        registrar.context().registerReceiver(
                smsStateChangeReceiver,
                IntentFilter("SMS_SENT"))
    }

    fun onCancel(o: Any?) {
        registrar.context().unregisterReceiver(smsStateChangeReceiver)
        smsStateChangeReceiver = null
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>?, grantResults: IntArray): Boolean {
        if (requestCode != Permissions.BROADCAST_SMS) {
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
            registerDeliveredReceiver()
            registerSentReceiver()
            return true
        }
        eventSink.error("#01", "permission denied", null)
        return false
    }

    init {
        this.registrar = registrar
        permissions = Permissions(registrar.activity())
        registrar.addRequestPermissionsResultListener(this)
    }
}
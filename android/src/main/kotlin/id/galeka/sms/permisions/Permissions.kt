package id.galeka.sms.permisions

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import io.flutter.plugin.common.PluginRegistry

class Permissions(private val activity: Activity) {
    private fun hasPermission(permission: String): Boolean {
        return (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED)
    }

    private fun hasPermissions(permissions: Array<String>): Boolean {
        for (perm in permissions) {
            if (!hasPermission(perm)) {
                return false
            }
        }
        return true
    }

    fun checkAndRequestPermission(permissions: Array<String>, id: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }
        if (!hasPermissions(permissions)) {
            PermissionsRequestHandler.requestPermissions(
                    PermissionsRequest(id, permissions, activity)
            )
            return false
        }
        return true
    }

    companion object {
        const val RECV_SMS_ID_REQ = 1
        const val SEND_SMS_ID_REQ = 2
        const val READ_SMS_ID_REQ = 3
        const val READ_CONTACT_ID_REQ = 4
        const val BROADCAST_SMS = 5
        const val READ_PHONE_STATE = 6
        private val requestsListener = PermissionsRequestHandler()
        @JvmStatic
        val requestsResultsListener: PluginRegistry.RequestPermissionsResultListener
            get() = requestsListener
    }

}
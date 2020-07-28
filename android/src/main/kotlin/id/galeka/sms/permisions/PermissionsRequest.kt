package id.galeka.sms.permisions

import EventChannel.StreamHandler
import android.annotation.TargetApi
import android.app.Activity
import android.os.Build

internal class PermissionsRequest(val id: Int, private val permissions: Array<String>, private val activity: Activity) {

    @TargetApi(Build.VERSION_CODES.M)
    fun execute() {
        activity.requestPermissions(permissions, id)
    }

}
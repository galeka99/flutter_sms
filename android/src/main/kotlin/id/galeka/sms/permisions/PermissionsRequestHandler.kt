package id.galeka.sms.permisions

import EventChannel.StreamHandler
import android.annotation.TargetApi
import android.os.Build
import io.flutter.plugin.common.PluginRegistry
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

/**
 * Created by Joan Pablo on 4/17/2018.
 */
internal class PermissionsRequestHandler : PluginRegistry.RequestPermissionsResultListener {
    @TargetApi(Build.VERSION_CODES.M)
    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>?, grantResults: IntArray?): Boolean {
        isRequesting = requests.size > 0
        if (isRequesting) {
            requests.poll().execute()
        }
        return false
    }

    companion object {
        private val requests: Queue<PermissionsRequest> = LinkedBlockingQueue()
        private var isRequesting = false

        @TargetApi(Build.VERSION_CODES.M)
        fun requestPermissions(permissionsRequest: PermissionsRequest) {
            if (!isRequesting) {
                isRequesting = true
                permissionsRequest.execute()
            } else {
                requests.add(permissionsRequest)
            }
        }
    }
}
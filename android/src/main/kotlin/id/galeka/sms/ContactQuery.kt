package id.galeka.sms

import EventChannel.StreamHandler
import android.Manifest
import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import id.galeka.sms.permisions.Permissions
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.RequestPermissionsResultListener
import org.json.JSONException
import org.json.JSONObject

/**
 * Created by babariviere on 10/03/18.
 */
internal class ContactQueryHandler(registrar: PluginRegistry.Registrar, result: MethodChannel.Result, contactAddress: String) : RequestPermissionsResultListener {
    private val permissionsList = arrayOf(Manifest.permission.READ_CONTACTS)
    private val registrar: PluginRegistry.Registrar
    private val result: MethodChannel.Result
    private val contactAddress: String
    fun handle(permissions: Permissions) {
        if (permissions.checkAndRequestPermission(permissionsList, Permissions.READ_CONTACT_ID_REQ)) {
            queryContact()
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private fun queryContact() {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(contactAddress))
        val projection = arrayOf(
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup.PHOTO_URI,
                ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI
        )
        val obj = JSONObject()
        val cursor: Cursor = registrar.context().getContentResolver().query(uri, projection, null, null, null)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                try {
                    obj.put("name", cursor.getString(0))
                    obj.put("photo", cursor.getString(1))
                    obj.put("thumbnail", cursor.getString(2))
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
            cursor.close()
        }
        result.success(obj)
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>?, grantResults: IntArray): Boolean {
        if (requestCode != Permissions.READ_CONTACT_ID_REQ) {
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
            queryContact()
            return true
        }
        result.error("#01", "permission denied", null)
        return false
    }

    init {
        this.registrar = registrar
        this.result = result
        this.contactAddress = contactAddress
    }
}

internal class ContactQuery(registrar: PluginRegistry.Registrar) : MethodCallHandler {
    private val permissions: Permissions
    private val registrar: PluginRegistry.Registrar
    fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        if (!call.method.equals("getContact")) {
            result.notImplemented()
        } else if (!call.hasArgument("address")) {
            result.error("#02", "missing argument 'address'", null)
        } else {
            val contactAddress: String = call.argument("address")
            val handler = ContactQueryHandler(registrar, result, contactAddress)
            registrar.addRequestPermissionsResultListener(handler)
            handler.handle(permissions)
        }
    }

    init {
        this.registrar = registrar
        permissions = Permissions(registrar.activity())
    }
}
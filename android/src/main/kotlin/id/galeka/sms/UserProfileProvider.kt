package id.galeka.sms

import EventChannel.StreamHandler
import android.Manifest
import android.annotation.TargetApi
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import id.galeka.sms.permisions.Permissions
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Created by Joan Pablo on 4/11/2018.
 */
internal class UserProfileHandler(registrar: PluginRegistry.Registrar, result: MethodChannel.Result) : PluginRegistry.RequestPermissionsResultListener {
    private val permissionsList = arrayOf(Manifest.permission.READ_CONTACTS)
    private val registrar: PluginRegistry.Registrar
    private val result: MethodChannel.Result
    fun handle(permissions: Permissions) {
        if (permissions.checkAndRequestPermission(permissionsList, Permissions.READ_CONTACT_ID_REQ)) {
            queryUserProfile()
        }
    }

    private fun queryUserProfile() {
        try {
            val obj = profileObject
            obj?.put("addresses", getProfileAddresses(obj.getString("id")))
            result.success(obj)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    @get:TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private val profileObject: JSONObject?
        private get() {
            var obj: JSONObject? = null
            val projection = arrayOf(
                    ContactsContract.Profile._ID,
                    ContactsContract.Profile.DISPLAY_NAME,
                    ContactsContract.Profile.PHOTO_URI,
                    ContactsContract.Profile.PHOTO_THUMBNAIL_URI)
            val cursor = contentResolver.query(ContactsContract.Profile.CONTENT_URI, projection, null, null, null)
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    obj = JSONObject()
                    try {
                        obj.put("id", cursor.getString(0))
                        obj.put("name", cursor.getString(1))
                        obj.put("photo", cursor.getString(2))
                        obj.put("thumbnail", cursor.getString(3))
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
                cursor.close()
            }
            return obj
        }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private fun getProfileAddresses(profileId: String?): JSONArray {
        val addressCollection = JSONArray()
        if (profileId != null) {
            val contentUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, profileId)
            val uri = Uri.withAppendedPath(contentUri, ContactsContract.Contacts.Entity.CONTENT_DIRECTORY)
            val projection = arrayOf(
                    ContactsContract.Contacts.Entity.DATA1,
                    ContactsContract.Contacts.Entity.MIMETYPE
            )
            val cursor = contentResolver.query(uri, projection, null, null, null)
            if (cursor != null) {
                cursor.moveToFirst()
                do {
                    if (cursor.getString(1) == "vnd.android.cursor.item/phone_v2") {
                        addressCollection.put(cursor.getString(0))
                    }
                } while (cursor.moveToNext())
                cursor.close()
            }
        }
        return addressCollection
    }

    private val contentResolver: ContentResolver
        private get() = registrar.context().getContentResolver()

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
            queryUserProfile()
            return true
        }
        result.error("#01", "permission denied", null)
        return false
    }

    init {
        this.registrar = registrar
        this.result = result
    }
}

internal class UserProfileProvider(registrar: PluginRegistry.Registrar) : MethodChannel.MethodCallHandler {
    private val permissions: Permissions
    private val registrar: PluginRegistry.Registrar
    fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        if (!call.method.equals("getUserProfile")) {
            result.notImplemented()
        } else {
            getUserProfile(result)
        }
    }

    private fun getUserProfile(result: MethodChannel.Result) {
        val handler = UserProfileHandler(registrar, result)
        registrar.addRequestPermissionsResultListener(handler)
        handler.handle(permissions)
    }

    init {
        this.registrar = registrar
        permissions = Permissions(registrar.activity())
    }
}
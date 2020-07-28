package id.galeka.sms

import EventChannel.StreamHandler
import android.Manifest
import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
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
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Created by joanpablo on 18/03/18.
 */
internal class ContactPhotoQueryHandler(registrar: PluginRegistry.Registrar, result: MethodChannel.Result, photoUri: String, fullSize: Boolean) : RequestPermissionsResultListener {
    private val registrar: PluginRegistry.Registrar
    private val permissionsList = arrayOf(Manifest.permission.READ_CONTACTS)
    private val result: MethodChannel.Result
    private val photoUri: String
    private val fullSize: Boolean
    fun handle(permissions: Permissions) {
        if (permissions.checkAndRequestPermission(permissionsList, Permissions.READ_CONTACT_ID_REQ)) {
            if (fullSize) {
                queryContactPhoto()
            } else {
                queryContactThumbnail()
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.ECLAIR)
    private fun queryContactThumbnail() {
        val uri = Uri.withAppendedPath(ContactsContract.AUTHORITY_URI, photoUri)
        val cursor: Cursor = registrar.context().getContentResolver().query(uri, arrayOf(ContactsContract.CommonDataKinds.Photo.PHOTO), null, null, null)
                ?: return
        try {
            if (cursor.moveToFirst()) {
                result.success(cursor.getBlob(0))
            }
        } finally {
            cursor.close()
        }
    }

    @TargetApi(Build.VERSION_CODES.ECLAIR)
    private fun queryContactPhoto() {
        val uri = Uri.withAppendedPath(ContactsContract.AUTHORITY_URI, photoUri)
        try {
            val fd: AssetFileDescriptor = registrar.context().getContentResolver().openAssetFileDescriptor(
                    uri, "r")
            if (fd != null) {
                val stream: InputStream = fd.createInputStream()
                val bytes = getBytesFromInputStream(stream)
                stream.close()
                result.success(bytes)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
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
            queryContactPhoto()
            return true
        }
        result.error("#01", "permission denied", null)
        return false
    }

    companion object {
        @Throws(IOException::class)
        fun getBytesFromInputStream(`is`: InputStream): ByteArray {
            val os = ByteArrayOutputStream()
            val buffer = ByteArray(0xFFFF)
            var len = `is`.read(buffer)
            while (len != -1) {
                os.write(buffer, 0, len)
                len = `is`.read(buffer)
            }
            return os.toByteArray()
        }
    }

    init {
        this.registrar = registrar
        this.result = result
        this.photoUri = photoUri
        this.fullSize = fullSize
    }
}

internal class ContactPhotoQuery(registrar: PluginRegistry.Registrar) : MethodCallHandler {
    private val permissions: Permissions
    private val registrar: PluginRegistry.Registrar
    fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        if (!call.method.equals("getContactPhoto")) {
            result.notImplemented()
            return
        }
        if (!call.hasArgument("photoUri")) {
            result.error("#02", "missing argument 'photoUri'", null)
            return
        }
        val photoUri: String = call.argument("photoUri")
        val fullSize = call.hasArgument("fullSize") && call.argument("fullSize")
        val handler = ContactPhotoQueryHandler(registrar, result, photoUri, fullSize)
        registrar.addRequestPermissionsResultListener(handler)
        handler.handle(permissions)
    }

    init {
        this.registrar = registrar
        permissions = Permissions(registrar.activity())
    }
}
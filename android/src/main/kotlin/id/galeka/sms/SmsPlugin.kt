package id.galeka.sms

import EventChannel.StreamHandler
import id.galeka.sms.permisions.Permissions.Companion.requestsResultsListener
import id.galeka.sms.status.SmsStateHandler
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.JSONMethodCodec
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry.Registrar
import io.flutter.plugin.common.StandardMethodCodec

/**
 * SmsPlugin
 */
object SmsPlugin {
    private const val CHANNEL_RECV = "plugins.babariviere.com/recvSMS"
    private const val CHANNEL_SMS_STATUS = "plugins.babariviere.com/statusSMS"
    private const val CHANNEL_SEND = "plugins.babariviere.com/sendSMS"
    private const val CHANNEL_QUER = "plugins.babariviere.com/querySMS"
    private const val CHANNEL_QUER_CONT = "plugins.babariviere.com/queryContact"
    private const val CHANNEL_QUER_CONT_PHOTO = "plugins.babariviere.com/queryContactPhoto"
    private const val CHANNEL_USER_PROFILE = "plugins.babariviere.com/userProfile"
    private const val CHANNEL_SIM_CARDS = "plugins.babariviere.com/simCards"

    /**
     * Plugin registration.
     */
    fun registerWith(registrar: Registrar) {
        registrar.addRequestPermissionsResultListener(requestsResultsListener)

        // SMS receiver
        val receiver = SmsReceiver(registrar)
        val receiveSmsChannel = EventChannel(registrar.messenger(),
                CHANNEL_RECV, JSONMethodCodec.INSTANCE)
        receiveSmsChannel.setStreamHandler(receiver)

        // SMS status receiver
        EventChannel(registrar.messenger(), CHANNEL_SMS_STATUS, JSONMethodCodec.INSTANCE)
                .setStreamHandler(SmsStateHandler(registrar))

        /// SMS sender
        val sender = SmsSender(registrar)
        val sendSmsChannel = MethodChannel(registrar.messenger(),
                CHANNEL_SEND, JSONMethodCodec.INSTANCE)
        sendSmsChannel.setMethodCallHandler(sender)

        /// SMS query
        val query = SmsQuery(registrar)
        val querySmsChannel = MethodChannel(registrar.messenger(), CHANNEL_QUER, JSONMethodCodec.INSTANCE)
        querySmsChannel.setMethodCallHandler(query)

        /// Contact query
        val contactQuery = ContactQuery(registrar)
        val queryContactChannel = MethodChannel(registrar.messenger(), CHANNEL_QUER_CONT, JSONMethodCodec.INSTANCE)
        queryContactChannel.setMethodCallHandler(contactQuery)

        /// Contact Photo query
        val contactPhotoQuery = ContactPhotoQuery(registrar)
        val queryContactPhotoChannel = MethodChannel(registrar.messenger(), CHANNEL_QUER_CONT_PHOTO, StandardMethodCodec.INSTANCE)
        queryContactPhotoChannel.setMethodCallHandler(contactPhotoQuery)

        /// User Profile
        val userProfileProvider = UserProfileProvider(registrar)
        val userProfileProviderChannel = MethodChannel(registrar.messenger(), CHANNEL_USER_PROFILE, JSONMethodCodec.INSTANCE)
        userProfileProviderChannel.setMethodCallHandler(userProfileProvider)

        //Sim Cards Provider
        MethodChannel(registrar.messenger(), CHANNEL_SIM_CARDS, JSONMethodCodec.INSTANCE)
                .setMethodCallHandler(SimCardsProvider(registrar))
    }
}
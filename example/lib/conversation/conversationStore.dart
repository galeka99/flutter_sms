import 'package:flutter/widgets.dart';
import 'package:sms_plugin/contact.dart';
import 'package:sms_plugin/sms.dart';

class ConversationStore extends InheritedWidget {
  const ConversationStore(this.userProfile, this.thread, {Widget child})
      : super(child: child);

  final UserProfile userProfile;
  final SmsThread thread;

  static ConversationStore of(BuildContext context) {
    // ignore: deprecated_member_use
    return context.inheritFromWidgetOfExactType(ConversationStore)
        as ConversationStore;
  }

  @override
  bool updateShouldNotify(InheritedWidget oldWidget) {
    return true;
  }
}

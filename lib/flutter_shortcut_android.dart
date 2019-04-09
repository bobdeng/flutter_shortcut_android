import 'dart:async';

import 'package:flutter/services.dart';

class FlutterShortcutAndroid {
  static const MethodChannel _channel =
      const MethodChannel('flutter_shortcut_android');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
  static Future createShortcut(String name,String extra,String packageName,String mainClassName)async{
    await _channel.invokeMethod("createShortcut",{"name":name,"extra":extra,"packageName":packageName,"mainClassName":mainClassName});
  }
  static Future getExtra()async{
    return await _channel.invokeMethod("getExtra",[]);
  }

}

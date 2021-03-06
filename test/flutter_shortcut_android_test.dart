import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_shortcut_android/flutter_shortcut_android.dart';

void main() {
  const MethodChannel channel = MethodChannel('flutter_shortcut_android');

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await FlutterShortcutAndroid.platformVersion, '42');
  });
}

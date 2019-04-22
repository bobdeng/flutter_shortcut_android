package com.anguo.flutter_shortcut_android;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.widget.Toast;

import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * FlutterShortcutAndroidPlugin
 */
public class FlutterShortcutAndroidPlugin implements MethodCallHandler, PluginRegistry.RequestPermissionsResultListener {
    private static final int REQUEST_PERMISSION_INSTALL_SHORTCUT = 8001;
    private static final String TAG = "Shortcut";
    /**
     * Plugin registration.
     */
    private Context context;
    private Activity activity;
    MethodCall pendingCall ;
    Result pendingResult;

    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter_shortcut_android");
        FlutterShortcutAndroidPlugin flutterShortcutAndroidPlugin = new FlutterShortcutAndroidPlugin();
        channel.setMethodCallHandler(flutterShortcutAndroidPlugin);
        flutterShortcutAndroidPlugin.context = registrar.activeContext();
        flutterShortcutAndroidPlugin.activity = registrar.activity();
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if (call.method.equals("getPlatformVersion")) {
            result.success("Android " + android.os.Build.VERSION.RELEASE);
        } else if (call.method.equals("createShortcut")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG,"check install_shortcut permission");
                if (hasNoPermission()) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                            Manifest.permission.INSTALL_SHORTCUT)) {
                        Toast.makeText(activity, "需要创建快捷方式的权限", Toast.LENGTH_LONG).show();

                    } else {
                        requestPermission(call, result);
                    }
                } else {
                    Log.d(TAG," permission granted");
                    installShortcut(call);
                }
            }else{
                Log.d(TAG,"no need permission");
                installShortcut(call);
            }
        } else if (call.method.equals("getExtra")) {
            if (activity.getIntent().getExtras() != null) {
                String extra = activity.getIntent().getExtras().getString("shortcut_extra");
                Log.d("Shortcut", "extra:" + extra);
                result.success(extra);
            } else {
                result.success(null);
            }
        } else {
            result.notImplemented();
        }
    }

    private void requestPermission(MethodCall call, Result result) {
        Log.d(TAG,"request install_shortcut permission");
        pendingCall = call;
        pendingResult = result;
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.INSTALL_SHORTCUT},
                REQUEST_PERMISSION_INSTALL_SHORTCUT);
    }

    private boolean hasNoPermission() {
        return ContextCompat.checkSelfPermission(activity,
                Manifest.permission.INSTALL_SHORTCUT) != PackageManager.PERMISSION_GRANTED;
    }

    private void installShortcut(MethodCall call) {
        Map<String, String> args = (Map<String, String>) call.arguments;
        createShortcut(args.get("name"), args.get("extra"), args.get("packageName"), args.get("mainClassName"));
    }

    private void createShortcut(String name, String extra, String packageName, String mainClassName) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.putExtra("shortcut_extra", extra);
        intent.setClassName(packageName, mainClassName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            addShortCut(context, intent, name, extra, BitmapFactory.decodeResource(context.getResources(), R.drawable.image_unlock));
        } else {
            addShortcut(context, intent, name, false);
        }
    }

    public static void addShortCut(Context context, Intent intent, String name, String id, Bitmap icon) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShortcutManager shortcutManager = null;
            shortcutManager = (ShortcutManager) context.getSystemService(Context.SHORTCUT_SERVICE);
            if (shortcutManager.isRequestPinShortcutSupported()) {

                ShortcutInfo info = new ShortcutInfo.Builder(context, id)
                        .setIcon(Icon.createWithBitmap(icon))
                        .setShortLabel(name)
                        .setIntent(intent)
                        .build();
                //当添加快捷方式的确认弹框弹出来时，将被回调
                PendingIntent shortcutCallbackIntent = PendingIntent.getBroadcast(context, 0, new Intent(context, AddShortcutReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);

                shortcutManager.requestPinShortcut(info, shortcutCallbackIntent.getIntentSender());
            }
        }

    }

    public void addShortcut(Context context, Intent actionIntent, String name,
                            boolean allowRepeat) {
        Intent addShortcutIntent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
        // 是否允许重复创建
        addShortcutIntent.putExtra("duplicate", allowRepeat);
        // 快捷方式的标题
        addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
        // 快捷方式的图标
        addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context, R.drawable.image_unlock));
        // 快捷方式的动作
        addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, actionIntent);
        context.sendBroadcast(addShortcutIntent);
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_INSTALL_SHORTCUT) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                installShortcut(pendingCall);
            } else {
                pendingResult.error(
                        "no_permissions", "neet install shortcut permission", null);
                pendingResult = null;
                pendingCall = null;
            }
            return true;
        }
        return false;
    }
}

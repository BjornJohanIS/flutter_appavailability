package com.pichillilorenzo.flutter_appavailability;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.annotation.TargetApi;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/** AppAvailability plugin using Flutter V2 embedding */
public class AppAvailability implements FlutterPlugin, MethodCallHandler, ActivityAware {

    private MethodChannel channel;
    private Context context;
    private Activity activity;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        context = flutterPluginBinding.getApplicationContext();
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "com.pichillilorenzo/flutter_appavailability");
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
        channel = null;
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        activity = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivity() {
        activity = null;
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        String uriSchema;
        switch (call.method) {
            case "checkAvailability":
                uriSchema = call.argument("uri").toString();
                checkAvailability(uriSchema, result);
                break;
            case "getInstalledApps":
                result.success(getInstalledApps());
                break;
            case "isAppEnabled":
                uriSchema = call.argument("uri").toString();
                isAppEnabled(uriSchema, result);
                break;
            case "launchApp":
                uriSchema = call.argument("uri").toString();
                launchApp(uriSchema, result);
                break;
            default:
                result.notImplemented();
        }
    }

    private void checkAvailability(String uri, Result result) {
        PackageInfo info = getAppPackageInfo(uri);
        if (info != null) {
            result.success(convertPackageInfoToJson(info));
            return;
        }
        result.error("", "App not found " + uri, null);
    }

    private List<Map<String, Object>> getInstalledApps() {
        PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> apps = packageManager.getInstalledPackages(0);
        List<Map<String, Object>> installedApps = new ArrayList<>(apps.size());
        int systemAppMask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;

        for (PackageInfo pInfo : apps) {
            if ((pInfo.applicationInfo.flags & systemAppMask) != 0) {
                continue;
            }
            installedApps.add(convertPackageInfoToJson(pInfo));
        }
        return installedApps;
    }

    private PackageInfo getAppPackageInfo(String uri) {
        try {
            return context.getPackageManager().getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private Map<String, Object> convertPackageInfoToJson(PackageInfo info) {
        Map<String, Object> map = new HashMap<>();
        map.put("app_name", info.applicationInfo.loadLabel(context.getPackageManager()).toString());
        map.put("package_name", info.packageName);
        map.put("version_code", String.valueOf(info.versionCode));
        map.put("version_name", info.versionName);
        return map;
    }

    private void isAppEnabled(String packageName, Result result) {
        boolean appStatus = false;
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(packageName, 0);
            if (ai != null) appStatus = ai.enabled;
        } catch (PackageManager.NameNotFoundException e) {
            result.error("", e.getMessage() + " " + packageName, e);
            return;
        }
        result.success(appStatus);
    }

    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    private void launchApp(String packageName, Result result) {
        PackageInfo info = getAppPackageInfo(packageName);
        if (info != null) {
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launchIntent);
                result.success(null);
                return;
            }
        }
        result.error("", "App not found " + packageName, null);
    }
}

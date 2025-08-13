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

public class AppAvailability implements FlutterPlugin, MethodCallHandler, ActivityAware {

    private MethodChannel channel;
    private Context context;
    private Activity activity;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPlugin.FlutterPluginBinding binding) {
        context = binding.getApplicationContext();
        channel = new MethodChannel(binding.getBinaryMessenger(), "com.pichillilorenzo/flutter_appavailability");
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPlugin.FlutterPluginBinding binding) {
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
        String uri;
        switch (call.method) {
            case "checkAvailability":
                uri = call.argument("uri");
                checkAvailability(uri, result);
                break;
            case "getInstalledApps":
                result.success(getInstalledApps());
                break;
            case "isAppEnabled":
                uri = call.argument("uri");
                isAppEnabled(uri, result);
                break;
            case "launchApp":
                uri = call.argument("uri");
                launchApp(uri, result);
                break;
            default:
                result.notImplemented();
        }
    }

    private void checkAvailability(String packageName, Result result) {
        PackageInfo info = getAppPackageInfo(packageName);
        if (info != null) {
            result.success(convertPackageInfoToJson(info));
        } else {
            result.error("", "App not found: " + packageName, null);
        }
    }

    private List<Map<String, Object>> getInstalledApps() {
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> packages = pm.getInstalledPackages(0);
        List<Map<String, Object>> apps = new ArrayList<>();
        int systemAppMask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;

        for (PackageInfo pkg : packages) {
            if ((pkg.applicationInfo.flags & systemAppMask) != 0) continue;
            apps.add(convertPackageInfoToJson(pkg));
        }
        return apps;
    }

    private PackageInfo getAppPackageInfo(String packageName) {
        try {
            return context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
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
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(packageName, 0);
            result.success(ai.enabled);
        } catch (PackageManager.NameNotFoundException e) {
            result.error("", e.getMessage() + " " + packageName, e);
        }
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
        result.error("", "App not found: " + packageName, null);
    }
}

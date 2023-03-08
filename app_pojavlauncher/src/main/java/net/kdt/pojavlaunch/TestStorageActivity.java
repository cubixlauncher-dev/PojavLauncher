package net.kdt.pojavlaunch;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.kdt.mcgui.ProgressLayout;

import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.tasks.AsyncAssetManager;

public class TestStorageActivity extends Activity {
    private final int REQUEST_STORAGE_REQUEST_CODE = 1;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT < 29 && !isStorageAllowed(this)) requestStoragePermission();
        else exit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_STORAGE_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exit();
            } else {
                Toast.makeText(this, R.string.toast_permission_denied, Toast.LENGTH_LONG).show();
                requestStoragePermission();
            }
        }
    }

    public static boolean isStorageAllowed(Context context) {
        //Getting the permission status
        int result1 = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int result2 = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);


        //If permission is granted returning true
        return result1 == PackageManager.PERMISSION_GRANTED &&
                result2 == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {

        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_REQUEST_CODE);
    }

    private void exit() {
        if(!Tools.checkStorageRoot(this)) {
            startActivity(new Intent(this, MissingStorageActivity.class));
            return;
        }
        //Only run them once we get a definitive green light to use storage
        AsyncAssetManager.unpackComponents(this);
        AsyncAssetManager.unpackSingleFiles(this);
        String bootstrapActivityClassName = getString(R.string.main_activity_class_name);
        Class<?> activityClass = LauncherActivity.class;
        try {
            activityClass = Class.forName(bootstrapActivityClassName);
        }catch (Throwable th) {
            th.printStackTrace();
        }
        Intent intent =  new Intent(this, activityClass);
        startActivity(intent);
        //ProgressKeeper.submitProgress(ProgressLayout.DOWNLOAD_MINECRAFT, 50, R.string.app_short_name);
        finish();
    }
}

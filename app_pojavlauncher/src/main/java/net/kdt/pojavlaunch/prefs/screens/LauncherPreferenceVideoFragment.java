package net.kdt.pojavlaunch.prefs.screens;

import static net.kdt.pojavlaunch.Architecture.is32BitsDevice;
import static net.kdt.pojavlaunch.Tools.getTotalDeviceMemory;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_NOTCH_SIZE;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;
import androidx.preference.SwitchPreferenceCompat;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.prefs.CustomSeekBarPreference;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

/**
 * Fragment for any settings video related
 */
public class LauncherPreferenceVideoFragment extends LauncherPreferenceFragment {
    @Override
    public void onCreatePreferences(Bundle b, String str) {
        int ramAllocation = LauncherPreferences.PREF_RAM_ALLOCATION;
        addPreferencesFromResource(R.xml.pref_video);
        Log.i("PNS", ""+PREF_NOTCH_SIZE);
        //Disable notch checking behavior on android 8.1 and below.
        findPreference("ignoreNotch").setVisible(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && PREF_NOTCH_SIZE > 0);

        CustomSeekBarPreference seek5 = findPreference("resolutionRatio");
        seek5.setMin(25);
        seek5.setSuffix(" %");

        // #724 bug fix
        if (seek5.getValue() < 25) {
            seek5.setValue(100);
        }

        // Sustained performance is only available since Nougat
        CheckBoxPreference sustainedPerfSwitch = findPreference("sustainedPerformance");
        sustainedPerfSwitch.setVisible(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N);

        int maxRAM;
        int deviceRam = getTotalDeviceMemory(getContext());

        CustomSeekBarPreference seek7 = findPreference("allocation");
        seek7.setMin(256);

        if(is32BitsDevice() || deviceRam < 2048) maxRAM = Math.min(1000, deviceRam);
        else maxRAM = deviceRam - (deviceRam < 3064 ? 800 : 1024); //To have a minimum for the device to breathe

        seek7.setMax(maxRAM);
        seek7.setValue(ramAllocation);
        seek7.setSuffix(" MB");

        computeVisibility();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences p, String s) {
        super.onSharedPreferenceChanged(p, s);
        computeVisibility();
    }

    private void computeVisibility(){
        CheckBoxPreference preference = findPreference("force_vsync");
        preference.setVisible(LauncherPreferences.PREF_USE_ALTERNATE_SURFACE);
    }
}

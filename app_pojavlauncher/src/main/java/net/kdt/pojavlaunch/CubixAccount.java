package net.kdt.pojavlaunch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class CubixAccount {
    public final String username;
    public final String cubixToken;

    public CubixAccount(String username, String token) {
        this.username = username;
        this.cubixToken = token;
    }

    public static CubixAccount getAccount(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("cubix_account", Context.MODE_PRIVATE);
        String username = prefs.getString("username", null);
        String token = prefs.getString("cubix_token", null);
        Log.i("CubixAccount", "username="+username+" token="+token);
        if(username == null || token == null) return null;
        return new CubixAccount(username, token);
    }

    @SuppressLint("ApplySharedPref")
    public void save(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("cubix_account", Context.MODE_PRIVATE);
        prefs.edit().putString("cubix_token", cubixToken).putString("username", username).commit();
    }

}

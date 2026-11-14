package com.example.ai

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

private const val TAG = "AppCheckInit"
private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

// getIdentifier keeps Firebase optional-resource lookup build-safe when google-services.json varies.
@SuppressLint("DiscouragedApi")
fun ensureFirebaseInitialized(context: Context): Boolean {
    if (FirebaseApp.getApps(context).isNotEmpty()) return true
    val appIdRes = context.resources.getIdentifier("google_app_id", "string", context.packageName)
    if (appIdRes == 0) {
        Log.e(TAG, "google_app_id string resource not found; google-services.json may be missing.")
        return false
    }
    return FirebaseApp.initializeApp(context) != null || FirebaseApp.getApps(context).isNotEmpty()
}

// Synchronous commit() is required before DebugAppCheckProviderFactory reads the persisted secret;
// res/raw/keep.xml preserves recaptcha_site_key from resource shrinking.
@SuppressLint("ApplySharedPref", "DiscouragedApi")
fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (!ensureFirebaseInitialized(context)) {
            return
        }

        if (BuildConfig.DEBUG) {
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)?.trim()?.takeIf { it.isNotEmpty() }
            if (debugToken != null) {
                val persistenceKey = FirebaseApp.getInstance().persistenceKey
                val prefs = context.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                Log.d(TAG, "App Check debug secret configured successfully.")
            } else {
                Log.w(TAG, "No debug token found in Intent extra.")
            }

            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG, "DebugAppCheckProviderFactory installed.")
        } else {
            val resId = context.resources.getIdentifier(
                RECAPTCHA_SITE_KEY_RES,
                "string",
                context.packageName
            )
            if (resId != 0) {
                val siteKey = context.getString(resId)
                if (siteKey.isNotBlank()) {
                    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                    )
                    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
                } else {
                    Log.e(TAG, "recaptcha_site_key resource string is blank.")
                }
            } else {
                Log.e(TAG, "recaptcha_site_key string resource not found in package.")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}

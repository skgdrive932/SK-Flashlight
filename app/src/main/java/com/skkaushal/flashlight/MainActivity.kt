package com.skkaushal.flashlight

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class MainActivity : Activity() {
    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null
    private var maxTorchStrength = 1
    private var torchOn = false
    private var interstitialAd: InterstitialAd? = null
    private var appOpenAd: AppOpenAd? = null
    private var isShowingAppOpenAd = false
    private var appOpenLoadInProgress = false
    private lateinit var torchButton: Button
    private lateinit var statusText: TextView
    private lateinit var brightnessSeek: SeekBar
    private var adView: AdView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var modeRunnable: Runnable? = null
    private var timerRunnable: Runnable? = null
    private var timerEndAt = 0L

    // The AdMob IDs supplied for SK Flashlight.
    private val bannerAdUnitId = "ca-app-pub-1692534879845424/4022476142"
    private val interstitialAdUnitId = "ca-app-pub-1692534879845424/4429670256"
    // App Open Ad unit created for SK Flashlight.
    private val appOpenAdUnitId = "ca-app-pub-1692534879845424/3134016448"
    private val cameraPermissionRequest = 3001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(10, 15, 29)
        window.navigationBarColor = Color.rgb(10, 15, 29)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        buildUi()
        initializeAds()
        ensureCameraPermission()
    }

    private fun initializeAds() {
        MobileAds.initialize(this) {
            runOnUiThread {
                adView?.loadAd(AdRequest.Builder().build())
                loadInterstitial()
                loadAppOpenAd(showWhenLoaded = true)
            }
        }
    }

    private fun ensureCameraPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            findFlashCamera()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), cameraPermissionRequest)
        }
    }

    override fun onStart() {
        super.onStart()
        // If an App Open ad was preloaded while the app was in the background,
        // show it when the main activity becomes visible again.
        if (!isShowingAppOpenAd) {
            if (appOpenAd != null) showAppOpenAd() else loadAppOpenAd(showWhenLoaded = true)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraPermissionRequest) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                findFlashCamera()
            } else {
                statusText.text = "Camera permission is needed to use the flashlight."
                torchButton.isEnabled = false
            }
        }
    }

    private fun findFlashCamera() {
        try {
            cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (cameraId == null) {
                statusText.text = "This device does not report a camera flash."
                torchButton.isEnabled = false
                brightnessSeek.isEnabled = false
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                maxTorchStrength = cameraManager.getCameraCharacteristics(cameraId!!)
                    .get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
                brightnessSeek.max = (maxTorchStrength - 1).coerceAtLeast(0)
                brightnessSeek.isEnabled = maxTorchStrength > 1
            } else {
                brightnessSeek.isEnabled = false
            }
        } catch (_: Exception) {
            statusText.text = "Could not access camera flash on this device."
            torchButton.isEnabled = false
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(8))
            setBackgroundColor(Color.rgb(10, 15, 29))
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        content.addView(text("SK FLASHLIGHT", 27f, Color.WHITE, true).apply {
            letterSpacing = 0.08f
            setPadding(0, dp(18), 0, dp(4))
        }, matchWrap())
        content.addView(text("PRO • BRIGHT • SIMPLE", 12f, Color.rgb(145, 161, 190), true).apply {
            setPadding(0, 0, 0, dp(20))
        }, matchWrap())
        statusText = text("Torch is OFF", 15f, Color.rgb(203, 213, 230), true).apply {
            setPadding(0, dp(4), 0, dp(16))
        }
        torchButton = Button(this).apply {
            text = "⚡  TURN ON"
            textSize = 18f
            setOnClickListener { setTorch(!torchOn) }
        }
        content.addView(statusText, matchWrap())
        content.addView(torchButton, LinearLayout.LayoutParams(-1, dp(62)).apply { bottomMargin = dp(18) })
        content.addView(text("Brightness (supported devices only)", 13f, Color.rgb(190, 202, 222), false), matchWrap())
        brightnessSeek = SeekBar(this).apply {
            max = 5
            progress = 5
            isEnabled = false
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser && torchOn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        try {
                            val id = cameraId ?: return
                            cameraManager.turnOnTorchWithStrengthLevel(id, (progress + 1).coerceIn(1, maxTorchStrength))
                        } catch (_: Exception) {
                            Toast.makeText(this@MainActivity, "Brightness control is not supported.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        content.addView(brightnessSeek, matchWrap().apply { bottomMargin = dp(14) })

        val modeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        modeRow.addView(actionButton("SOS") { startPattern(true) }, weightedButton())
        modeRow.addView(actionButton("STROBE") { startPattern(false) }, weightedButton())
        content.addView(modeRow, matchWrap().apply { bottomMargin = dp(8) })
        content.addView(actionButton("Timer • 1 minute") { startTimer(60) }, matchWrap().apply { bottomMargin = dp(8) })
        content.addView(actionButton("Timer • 5 minutes") { startTimer(300) }, matchWrap().apply { bottomMargin = dp(8) })
        content.addView(actionButton("Stop modes / timer") { stopModes(); setTorch(false) }, matchWrap().apply { bottomMargin = dp(8) })
        content.addView(actionButton("About SK Flashlight") { openAboutWithInterstitial() }, matchWrap().apply { bottomMargin = dp(12) })
        content.addView(text("Flash brightness and availability depend on your phone hardware.", 12f, Color.rgb(145, 161, 190), true).apply {
            setPadding(dp(4), dp(4), dp(4), dp(14))
        }, matchWrap())
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        adView = AdView(this).apply {
            adUnitId = bannerAdUnitId
            setAdSize(AdSize.BANNER)
        }
        root.addView(adView, LinearLayout.LayoutParams(-1, dp(50)))
        setContentView(root)
    }

    private fun setTorch(enabled: Boolean) {
        val id = cameraId ?: run {
            Toast.makeText(this, "Flashlight is not available yet.", Toast.LENGTH_SHORT).show()
            return
        }
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ensureCameraPermission(); return
        }
        try {
            cameraManager.setTorchMode(id, enabled)
            torchOn = enabled
            torchButton.text = if (enabled) "⚡  TURN OFF" else "⚡  TURN ON"
            statusText.text = if (enabled) "Torch is ON" else "Torch is OFF"
        } catch (e: Exception) {
            Toast.makeText(this, "Torch error: ${e.localizedMessage ?: "unknown"}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startPattern(sos: Boolean) {
        stopModes()
        if (cameraId == null) { findFlashCamera(); if (cameraId == null) return }
        if (!sos) {
            var on = false
            val task = object : Runnable {
                override fun run() {
                    on = !on
                    setTorch(on)
                    handler.postDelayed(this, 250)
                }
            }
            modeRunnable = task
            handler.post(task)
            statusText.text = "Strobe mode running"
        } else {
            // Approximate SOS: short-short-short, long-long-long, short-short-short.
            val units = intArrayOf(1, 1, 1, 3, 3, 3, 1, 1, 1, 4)
            var index = 0
            var on = false
            val task = object : Runnable {
                override fun run() {
                    on = !on
                    setTorch(on)
                    val delay = units[index % units.size] * 180L
                    index++
                    handler.postDelayed(this, delay)
                }
            }
            modeRunnable = task
            handler.post(task)
            statusText.text = "SOS mode running"
        }
    }

    private fun startTimer(seconds: Int) {
        timerRunnable?.let { handler.removeCallbacks(it) }
        timerEndAt = System.currentTimeMillis() + seconds * 1000L
        val task = object : Runnable {
            override fun run() {
                val remaining = ((timerEndAt - System.currentTimeMillis()) / 1000L).toInt()
                if (remaining <= 0) {
                    timerRunnable = null
                    setTorch(false)
                    statusText.text = "Torch is OFF • Timer finished"
                    Toast.makeText(this@MainActivity, "Flashlight timer finished", Toast.LENGTH_SHORT).show()
                } else {
                    statusText.text = "Timer ${remaining / 60}:${(remaining % 60).toString().padStart(2, '0')}"
                    handler.postDelayed(this, 1000L)
                }
            }
        }
        timerRunnable = task
        handler.post(task)
        Toast.makeText(this, "Timer started", Toast.LENGTH_SHORT).show()
    }

    private fun stopModes() {
        modeRunnable?.let { handler.removeCallbacks(it) }
        timerRunnable?.let { handler.removeCallbacks(it) }
        modeRunnable = null
        timerRunnable = null
    }

    private fun loadAppOpenAd(showWhenLoaded: Boolean = false) {
        if (appOpenLoadInProgress || appOpenAd != null) return
        appOpenLoadInProgress = true

        AppOpenAd.load(
            this,
            appOpenAdUnitId,
            AdRequest.Builder().build(),
            object : AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenLoadInProgress = false
                    appOpenAd = ad
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() {
                            isShowingAppOpenAd = true
                        }

                        override fun onAdDismissedFullScreenContent() {
                            isShowingAppOpenAd = false
                            appOpenAd = null
                            loadAppOpenAd()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            isShowingAppOpenAd = false
                            appOpenAd = null
                            loadAppOpenAd()
                        }
                    }

                    if (showWhenLoaded && !isFinishing && !isDestroyed) {
                        showAppOpenAd()
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    appOpenLoadInProgress = false
                    appOpenAd = null
                }
            }
        )
    }

    private fun showAppOpenAd() {
        val ad = appOpenAd ?: return
        if (isShowingAppOpenAd || isFinishing || isDestroyed) return
        appOpenAd = null
        ad.show(this)
    }

    private fun loadInterstitial() {
        InterstitialAd.load(this, interstitialAdUnitId, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null
                            showAbout()
                            loadInterstitial()
                        }
                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            interstitialAd = null
                            showAbout()
                            loadInterstitial()
                        }
                    }
                }
                override fun onAdFailedToLoad(error: LoadAdError) { interstitialAd = null }
            })
    }

    private fun openAboutWithInterstitial() {
        val ad = interstitialAd
        if (ad != null) ad.show(this) else showAbout()
    }

    private fun showAbout() {
        android.app.AlertDialog.Builder(this)
            .setTitle("About SK Flashlight")
            .setMessage("Name: Sk kaushal\nEmail: skgdrive932@gmail.com\nContact: +919779371866\n\nSK Flashlight — a simple flashlight utility.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun text(value: String, size: Float, color: Int, centered: Boolean) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (centered) gravity = Gravity.CENTER
    }

    private fun actionButton(value: String, action: () -> Unit) = Button(this).apply {
        text = value
        setOnClickListener { action() }
    }

    private fun matchWrap() = LinearLayout.LayoutParams(-1, -2)
    private fun weightedButton() = LinearLayout.LayoutParams(0, dp(52), 1f).apply {
        marginStart = dp(3); marginEnd = dp(3)
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onPause() {
        adView?.pause()
        super.onPause()
    }
    override fun onResume() {
        super.onResume()
        adView?.resume()
    }
    override fun onDestroy() {
        stopModes()
        if (torchOn) setTorch(false)
        adView?.destroy()
        super.onDestroy()
    }
}

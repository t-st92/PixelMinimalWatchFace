/*
 *   Copyright 2021 Benoit LETONDOR
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.benoitletondor.pixelminimalwatchface.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.support.wearable.phone.PhoneDeviceType
import android.support.wearable.view.ConfirmationOverlay
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.benoitletondor.pixelminimalwatchface.BuildConfig
import com.benoitletondor.pixelminimalwatchface.BuildConfig.COMPANION_APP_PLAYSTORE_URL
import com.benoitletondor.pixelminimalwatchface.Injection
import com.benoitletondor.pixelminimalwatchface.R
import com.benoitletondor.pixelminimalwatchface.databinding.ActivityComplicationConfigBinding
import com.benoitletondor.pixelminimalwatchface.getWeatherProviderInfo
import com.benoitletondor.pixelminimalwatchface.helper.openActivity
import com.benoitletondor.pixelminimalwatchface.model.ComplicationColor
import com.benoitletondor.pixelminimalwatchface.model.Storage
import com.benoitletondor.pixelminimalwatchface.rating.FeedbackActivity
import com.benoitletondor.pixelminimalwatchface.settings.phonebattery.PhoneBatteryConfigurationActivity
import com.google.android.wearable.intent.RemoteIntent

class SettingsActivity : Activity() {
    private lateinit var adapter: ComplicationConfigRecyclerViewAdapter
    private lateinit var storage: Storage

    private lateinit var binding: ActivityComplicationConfigBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComplicationConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storage = Injection.storage(this)
        adapter = ComplicationConfigRecyclerViewAdapter(this, storage, {
            openAppOnPhone()
        }, { use24hTimeFormat ->
            storage.setUse24hTimeFormat(use24hTimeFormat)
        }, {
            storage.setRatingDisplayed(true)
            startActivity(Intent(this, FeedbackActivity::class.java))
        }, { showWearOSLogo ->
            storage.setShouldShowWearOSLogo(showWearOSLogo)
        }, { showComplicationsAmbient ->
            storage.setShouldShowComplicationsInAmbientMode(showComplicationsAmbient)
        }, { showFilledTimeAmbient ->
            storage.setShouldShowFilledTimeInAmbientMode(showFilledTimeAmbient)
        }, { showThinTimeRegular ->
            storage.setShouldShowThinTimeRegular(showThinTimeRegular)
        }, { timeSize ->
            storage.setTimeSize(timeSize)
        }, { dateAndBatterySize ->
            storage.setDateAndBatterySize(dateAndBatterySize)
        }, { showSecondsRing ->
            storage.setShouldShowSecondsRing(showSecondsRing)
        }, { showWeather ->
            storage.setShouldShowWeather(showWeather)
        }, {
            getWeatherProviderInfo()?.let { weatherProviderInfo ->
                openActivity(weatherProviderInfo.appPackage, weatherProviderInfo.weatherActivityName)
            }
        }, { showBattery ->
            storage.setShouldShowBattery(showBattery)
        }, { showBatteryInAmbient ->
            storage.setShouldHideBatteryInAmbient(!showBatteryInAmbient)
        }, { useShortDateFormat ->
            storage.setUseShortDateFormat(useShortDateFormat)
        }, { showDateAmbient ->
            storage.setShowDateInAmbient(showDateAmbient)
        }, {
            openAppForDonationOnPhone()
        }, {
            startActivityForResult(
                Intent(this, PhoneBatteryConfigurationActivity::class.java),
                COMPLICATION_PHONE_BATTERY_SETUP_REQUEST_CODE,
            )
        }, {
            startActivityForResult(
                ColorSelectionActivity.createIntent(
                    this,
                    ComplicationColor(getColor(R.color.white), getString(R.string.color_default), true)
                ),
                TIME_AND_DATE_COLOR_REQUEST_CODE
            )
        }, {
            startActivityForResult(
                ColorSelectionActivity.createIntent(
                    this,
                    ComplicationColor(getColor(R.color.white), getString(R.string.color_default), true)
                ),
                BATTERY_COLOR_REQUEST_CODE
            )
        }, { useAndroid12Style ->
            storage.setUseAndroid12Style(useAndroid12Style)
        }, {
            startActivityForResult(
                ColorSelectionActivity.createIntent(
                    this,
                    ComplicationColor(getColor(R.color.white), getString(R.string.color_default), true)
                ),
                SECONDS_RING_COLOR_REQUEST_CODE
            )
        }, { widgetsSize ->
            storage.setWidgetsSize(widgetsSize)
        })

        binding.wearableRecyclerView.apply {
            isEdgeItemsCenteringEnabled = true
            layoutManager = LinearLayoutManager(this@SettingsActivity)
            setHasFixedSize(true)
            adapter = this@SettingsActivity.adapter
        }
    }

    override fun onDestroy() {
        adapter.onDestroy()

        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if( requestCode == COMPLICATION_WEATHER_PERMISSION_REQUEST_CODE ) {
            adapter.weatherComplicationPermissionFinished()
        } else if( requestCode == COMPLICATION_BATTERY_PERMISSION_REQUEST_CODE ) {
            adapter.batteryComplicationPermissionFinished()
        } else if ( requestCode == COMPLICATION_CONFIG_REQUEST_CODE && resultCode == RESULT_OK ) {
            if (storage.useAndroid12Style()) {
                adapter.updateAndroid12Complications()
            } else {
                adapter.updateRegularComplications()
            }
        } else if ( requestCode == COMPLICATION_PHONE_BATTERY_SETUP_REQUEST_CODE ) {
            if (storage.useAndroid12Style()) {
                adapter.updateAndroid12Complications()
            } else {
                adapter.updateRegularComplications()
            }
            adapter.notifyDataSetChanged()
        } else if ( requestCode == TIME_AND_DATE_COLOR_REQUEST_CODE && resultCode == RESULT_OK ) {
            val color = data?.getParcelableExtra<ComplicationColor>(ColorSelectionActivity.RESULT_SELECTED_COLOR)
            if (color != null) {
                storage.setTimeAndDateColor(color.color)
            }
        } else if ( requestCode == BATTERY_COLOR_REQUEST_CODE && resultCode == RESULT_OK ) {
            val color = data?.getParcelableExtra<ComplicationColor>(ColorSelectionActivity.RESULT_SELECTED_COLOR)
            if (color != null) {
                storage.setBatteryIndicatorColor(color.color)
            }
        } else if (requestCode == SECONDS_RING_COLOR_REQUEST_CODE && resultCode == RESULT_OK) {
            val color = data?.getParcelableExtra<ComplicationColor>(ColorSelectionActivity.RESULT_SELECTED_COLOR)
            if (color != null) {
                storage.setSecondRingColor(color.color)
            }
        }
    }

    private fun openAppOnPhone() {
        if ( PhoneDeviceType.getPhoneDeviceType(applicationContext) == PhoneDeviceType.DEVICE_TYPE_ANDROID ) {
            // Create Remote Intent to open Play Store listing of app on remote device.
            val intentAndroid = Intent(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setData(Uri.parse("pixelminimalwatchface://open"))
                .setPackage(BuildConfig.APPLICATION_ID)

            RemoteIntent.startRemoteActivity(
                applicationContext,
                intentAndroid,
                object : ResultReceiver(Handler()) {
                    override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                        if (resultCode == RemoteIntent.RESULT_OK) {
                            ConfirmationOverlay()
                                .setFinishedAnimationListener {
                                    finish()
                                }
                                .setType(ConfirmationOverlay.OPEN_ON_PHONE_ANIMATION)
                                .setDuration(3000)
                                .setMessage(getString(R.string.open_phone_url_android_device))
                                .showOn(this@SettingsActivity)
                        } else {
                            openAppInStoreOnPhone()
                        }
                    }
                }
            )

            return
        }

        openAppInStoreOnPhone()
    }

    private fun openAppForDonationOnPhone() {
        if ( PhoneDeviceType.getPhoneDeviceType(applicationContext) == PhoneDeviceType.DEVICE_TYPE_ANDROID ) {
            // Create Remote Intent to open Play Store listing of app on remote device.
            val intentAndroid = Intent(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setData(Uri.parse("pixelminimalwatchface://donate"))
                .setPackage(BuildConfig.APPLICATION_ID)

            RemoteIntent.startRemoteActivity(
                applicationContext,
                intentAndroid,
                object : ResultReceiver(Handler()) {
                    override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                        if (resultCode == RemoteIntent.RESULT_OK) {
                            ConfirmationOverlay()
                                .setType(ConfirmationOverlay.OPEN_ON_PHONE_ANIMATION)
                                .setDuration(3000)
                                .setMessage(getString(R.string.open_phone_url_android_device))
                                .showOn(this@SettingsActivity)
                        } else {
                            openAppInStoreOnPhone(finish = false)
                        }
                    }
                }
            )

            return
        }

        openAppInStoreOnPhone(finish = false)
    }

    private fun openAppInStoreOnPhone(finish: Boolean = true) {
        when (PhoneDeviceType.getPhoneDeviceType(applicationContext)) {
            PhoneDeviceType.DEVICE_TYPE_ANDROID -> {
                // Create Remote Intent to open Play Store listing of app on remote device.
                val intentAndroid = Intent(Intent.ACTION_VIEW)
                    .addCategory(Intent.CATEGORY_BROWSABLE)
                    .setData(Uri.parse(COMPANION_APP_PLAYSTORE_URL))

                RemoteIntent.startRemoteActivity(
                    applicationContext,
                    intentAndroid,
                    object : ResultReceiver(Handler()) {
                        override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                            if (resultCode == RemoteIntent.RESULT_OK) {
                                ConfirmationOverlay()
                                    .setFinishedAnimationListener {
                                        if( finish ) {
                                            finish()
                                        }
                                    }
                                    .setType(ConfirmationOverlay.OPEN_ON_PHONE_ANIMATION)
                                    .setDuration(3000)
                                    .setMessage(getString(R.string.open_phone_url_android_device))
                                    .showOn(this@SettingsActivity)
                            } else if (resultCode == RemoteIntent.RESULT_FAILED) {
                                ConfirmationOverlay()
                                    .setType(ConfirmationOverlay.OPEN_ON_PHONE_ANIMATION)
                                    .setDuration(3000)
                                    .setMessage(getString(R.string.open_phone_url_android_device_failure))
                                    .showOn(this@SettingsActivity)
                            }
                        }
                    }
                )
            }
            PhoneDeviceType.DEVICE_TYPE_IOS -> {
                Toast.makeText(this@SettingsActivity, R.string.open_phone_url_ios_device, Toast.LENGTH_LONG).show()
            }
            PhoneDeviceType.DEVICE_TYPE_ERROR_UNKNOWN -> {
                Toast.makeText(this@SettingsActivity, R.string.open_phone_url_android_device_failure, Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        const val COMPLICATION_WEATHER_PERMISSION_REQUEST_CODE = 1003
        const val COMPLICATION_BATTERY_PERMISSION_REQUEST_CODE = 1004
        const val COMPLICATION_CONFIG_REQUEST_CODE = 1005
        const val COMPLICATION_PHONE_BATTERY_SETUP_REQUEST_CODE = 1006
        const val TIME_AND_DATE_COLOR_REQUEST_CODE = 1007
        const val BATTERY_COLOR_REQUEST_CODE = 1008
        const val SECONDS_RING_COLOR_REQUEST_CODE = 1009
    }
}

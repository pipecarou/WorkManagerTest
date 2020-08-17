package com.fcarou.workmanagertest

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.impl.utils.futures.SettableFuture
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.common.util.concurrent.ListenableFuture

class LongRunningWorker(
    context: Context,
    workerParameters: WorkerParameters
): ListenableWorker(context, workerParameters) {
    @SuppressLint("RestrictedApi")
    val settableFuture: SettableFuture<Result> = SettableFuture.create<Result>()

    var counter = 0L

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val fusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(applicationContext)

    private val locationRequest = LocationRequest().apply {
        interval = LOCATION_INTERVAL
        fastestInterval = FASTEST_LOCATION_INTERVAL
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private val locationCallback = object : LocationCallback() {
        @SuppressLint("RestrictedApi")
        override fun onLocationResult(locationResult: LocationResult?) {
            super.onLocationResult(locationResult)

            if (counter++ == COUNTER_BREAK) {
                Log.i("LOG", "End!!")
                fusedLocationProviderClient.removeLocationUpdates(this)
                settableFuture.set(Result.success())
            }

            with (locationResult?.lastLocation) {
                setForegroundAsync(createForegroundInfo(this))
            }
        }
    }

    private fun createForegroundInfo(location: Location?): ForegroundInfo {
        createChannel()

        val description: String = if (location != null) {
            "${location.latitude}/${location.longitude}"
        } else {
            "No location"
        }

        val notification = Notification.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("$NOTIFICATION_TITLE $counter")
            .setTicker(NOTIFICATION_TITLE)
            .setContentText(description)
            .setSmallIcon(R.drawable.ic_pizza)
            .setOngoing(true)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
    }

    @SuppressLint("RestrictedApi")
    override fun startWork(): ListenableFuture<Result> {
        setForegroundAsync(createForegroundInfo(null))

        try {
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("LOG", "Error: ${e.message}")
            settableFuture.set(Result.failure())
        }

        return settableFuture
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            && notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val mChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            mChannel.description = CHANNEL_DESCRIPTION
            notificationManager.createNotificationChannel(mChannel)
        }
    }

    companion object {
        const val CHANNEL_NAME = "myChannel"
        const val CHANNEL_DESCRIPTION = "My description"
        const val CHANNEL_ID = "channelId"
        const val NOTIFICATION_ID = 22322
        const val NOTIFICATION_TITLE = "Round "
        const val COUNTER_BREAK = 1000000L
        const val LOCATION_INTERVAL = 5000L
        const val FASTEST_LOCATION_INTERVAL = 2000L
    }
}
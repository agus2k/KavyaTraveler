package com.kavya.zes;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class MockedLocationService extends Service {

    @NonNull
    private static final String TAG = MockedLocationService.class.getSimpleName();
    private static final String CHANNEL_ID = "MockedLocationChannel";
    public static final String ACTION_STOP = "com.kavya.zes.ACTION_STOP";

    @NonNull
    protected final MutableLiveData<MockState> mockState = new MutableLiveData<>();
    @NonNull
    protected final MutableLiveData<Location> mockedLocation = new MutableLiveData<>();

    @NonNull
    private final List<MockedLocationProvider> providers = new ArrayList<>();

    @NonNull
    private final Timer timer = new Timer();
    @NonNull
    private final Set<TimerTask> tasks = Collections.synchronizedSet(new HashSet<>());

    @NonNull
    @Override
    public IBinder onBind(Intent intent) {
        indicateBinding();
        return new MockedBinder(this);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Mock is finished");
        for (TimerTask t : tasks)
            t.cancel();
        tasks.clear();
        for (MockedLocationProvider prov : providers)
            prov.shutdown();
        providers.clear();
        mockState.postValue(MockState.NOT_MOCKED);
        return super.onUnbind(intent);
    }

    private void indicateBinding() {
        mockState.postValue(MockState.SERVICE_BOUND);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        startForeground(1, createNotification());
        
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopMocking();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    private void stopMocking() {
        for (TimerTask t : tasks)
            t.cancel();
        tasks.clear();
        for (MockedLocationProvider prov : providers)
            prov.shutdown();
        providers.clear();
        mockState.postValue(MockState.NOT_MOCKED);
        stopForeground(true);
        stopSelf();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Layanan Lokasi Palsu",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, MockedLocationService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(this,
                0, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("KavyaZES Sedang Berjalan")
                .setContentText("Lokasi palsu sedang aktif.")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
                .build();
    }

    protected void startMockedService(double longitude, double latitude, double longitudeDistance, double latitudeDistance, long mockMilli, int maxTime, float mockSpeed) {
        try {
            providers.clear();
            providers.add(new MockedLocationProvider(LocationManager.GPS_PROVIDER, this));
            providers.add(new MockedLocationProvider(LocationManager.NETWORK_PROVIDER, this));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                providers.add(new MockedLocationProvider(LocationManager.FUSED_PROVIDER, this));
            }

            MockedTask mockedTask = new MockedTask(longitude, latitude, longitudeDistance, latitudeDistance, maxTime, mockSpeed);
            timer.schedule(mockedTask, 0L, mockMilli);
            tasks.add(mockedTask);
            mockState.postValue(MockState.MOCKED);
        } catch (SecurityException e) {
            Log.e(TAG, "Could not construct mock location providers!", e);
            mockState.postValue(MockState.MOCK_ERROR);
        }
    }

    class MockedTask extends TimerTask {
        private final float speed;
        private double longitude;
        private double latitude;
        private final double longitudeMockedDistance;
        private final double latitudeMockedDistance;
        private final int maxLocationTimes;
        private int currentTimes = 0;

        public MockedTask(double longitude, double latitude, double longitudeMockedDistance, double latitudeMockedDistance, int maxTimes, float mockSpeed) {
            this.longitude = longitude;
            this.latitude = latitude;
            this.longitudeMockedDistance = longitudeMockedDistance;
            this.latitudeMockedDistance = latitudeMockedDistance;
            this.maxLocationTimes = maxTimes;
            this.speed = mockSpeed;
        }

        @Override
        public void run() {
            Location value = new Location(LocationManager.GPS_PROVIDER);
            value.setLongitude(longitude);
            value.setLatitude(latitude);
            if (speed > 0) {
                value.setSpeed(speed);
                value.setAccuracy(0.1f);
                value.setTime(System.currentTimeMillis());

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    value.setSpeedAccuracyMetersPerSecond(0.01f);
                }
            }

            mockedLocation.postValue(value);
            for (MockedLocationProvider prov : providers)
                prov.pushLocation(latitude, longitude);
            ++currentTimes;
            if (maxLocationTimes != 0 && maxLocationTimes == currentTimes) {
                this.cancel();
                stopSelf();
                mockState.postValue(MockState.NOT_MOCKED);
            }
            latitude += latitudeMockedDistance;
            longitude += longitudeMockedDistance;
        }
    }

    public static class MockedBinder extends Binder {
        @NonNull
        private final MockedLocationService service;
        @NonNull
        public final LiveData<MockState> mockState;
        @NonNull
        public final LiveData<Location> mockedLocation;

        public MockedBinder(@NonNull MockedLocationService service) {
            this.service = service;
            this.mockState = service.mockState;
            this.mockedLocation = service.mockedLocation;
        }

        public void continueMock() {
            service.indicateBinding();
        }

        public void startMock(double longitude, double latitude, double longitudeDistance, double latitudeDistance, long mockMilli, int maxTimes, float mockSpeed) {
            service.startMockedService(longitude, latitude, longitudeDistance, latitudeDistance, mockMilli, maxTimes, mockSpeed);
        }
    }

}

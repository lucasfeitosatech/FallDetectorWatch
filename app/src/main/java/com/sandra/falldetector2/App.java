package com.sandra.falldetector2;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.facebook.stetho.Stetho;
import com.google.android.gms.location.LocationRequest;

import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import com.sandra.falldetector2.repository.ContactRepository;
import com.sandra.falldetector2.service.MailService;

public class App extends Application {
    public static App instance;
    private SharedPreferences sharedPreferences;
    private ContactRepository contactRepository = new ContactRepository();
    private SensorManager mSensorManager;
    private Handler handler;
    private Location location;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Stetho.initializeWithDefaults(this);
        setSharedPreferences(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Realm.init(this);
        locationUpdate();
    }

    @Override
    public Context getApplicationContext() {
        return super.getApplicationContext();
    }

    public static App getInstance() {
        return instance;
    }


    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    public void setSharedPreferences(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public void sendEmail(){

        new Thread(new Runnable() {
            public void run() {
                String locUrl = "";
                if(getLocation() != null){
                    locUrl = "https://www.google.com/maps/search/?api=1&query="+ getLocation().getLatitude() + "," + getLocation().getLongitude();
                }

                StringBuilder sb = new StringBuilder();
                MailService mailer = new MailService("tech.lucasfeitosa.falldetector@gmail.com","lucasdf10@gmail.com","Possivel queda de Fulano","Este é um chamado de alerta para o Fulano que provavelmente sofreu uma queda." + locUrl);
                try {
                    mailer.sendMessage(sb);
                    Log.d("teste", "onCreate: " + sb);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    public void getLastLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(this);
        locationProvider.getLastKnownLocation()
                .subscribe(location1 -> {
                    if (location1 != null) {
                        String baseLocation = "{\"latitude\":" + location1.getLatitude() + ", \"longitude\":" + location1.getLongitude() + "}";
                        Log.d("Location", "onCreate: " + baseLocation);
                        location = location1;
                    }
                });

        LocationRequest req = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setExpirationDuration(TimeUnit.SECONDS.toMillis(1000))
                .setInterval(1000);

        Observable<Location> goodEnoughQuicklyOrNothingObservable = locationProvider.getUpdatedLocation(req)
                .filter(new Func1<Location, Boolean>() {
                    @Override
                    public Boolean call(Location location) {
                        return location.getAccuracy() < 5;
                    }
                })
                .timeout(1000, TimeUnit.SECONDS, Observable.just((Location) null), AndroidSchedulers.mainThread())
                .first()
                .observeOn(AndroidSchedulers.mainThread());

        goodEnoughQuicklyOrNothingObservable.subscribe();

    }


    public void locationUpdate(){
        handler = new Handler();

        final Runnable r = new Runnable() {
            public void run() {
                getLastLocation();
                handler.postDelayed(this, 1000*60);
            }
        };

        handler.postDelayed(r, 1000);
    }

    public ContactRepository getContactRepository() {
        return contactRepository;
    }

    public Location getLocation() {
        return location;
    }
}
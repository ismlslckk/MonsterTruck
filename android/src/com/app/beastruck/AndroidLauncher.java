package com.app.beastruck;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.app.InterfaceListener;
import com.app.Main;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdRequest.Builder;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.games.AnnotatedData;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.leaderboard.LeaderboardScore;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// android launcher
public class AndroidLauncher extends AndroidApplication implements InterfaceListener {
    Main app;
    int score = 0;
    boolean showLeaders, isSigned;
    Toast toast;

    // AdMob
    AdView adMobBanner;
    InterstitialAd adMobInterstitial;
    AdRequest adRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Intent receiverIntent = new Intent(getApplicationContext(), MainReceiver.class);
        sendBroadcast(receiverIntent);

        // run
        app = new Main(this);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                Context context = getApplicationContext();
                SharedPreferences sharedPref = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);

                int agentGonderildi = sharedPref.getInt("agentGonderildi",0);
                if(agentGonderildi==0){
                    SharedPreferences.Editor editor = sharedPref.edit(); //SharedPreferences'a kayıt eklemek için editor oluşturuyoruz
                    editor.putInt("agentGonderildi",1); //int değer ekleniyor
                    editor.commit(); //Kayıt

                    try{
                        String API_LEVEL  = Build.VERSION.SDK;
                        String RELEASE = Build.VERSION.RELEASE;

                        DisplayMetrics metrics = getResources().getDisplayMetrics();
                        int densityDpi = (int)(metrics.density * 160f);
                        String dpiString=densityDpi+"dpi";
                        String widthHeight=metrics.widthPixels+"x"+metrics.heightPixels;
                        String marka=Build.MANUFACTURER;
                        String model =Build.MODEL;
                        String modelKodu=Build.DEVICE;
                        String cpu =Build.HARDWARE;

                        String sonDurum=API_LEVEL+"/"+RELEASE+"; "+dpiString+"; "+widthHeight+"; "+marka+"; "+model+"; "+modelKodu+"; "+cpu;
                        FirebaseFirestore db = FirebaseFirestore.getInstance();

                        Map<String, Object> mua = new HashMap<>();
                        mua.put("UserAgentBilgi", sonDurum);

                        db.collection("useragents")
                                .add(mua)
                                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {

                                    }
                                });
                    }
                    catch (Exception e){
                        TOAST(e.toString());
                    }
                }

                AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
                config.useImmersiveMode = true;
                ((ViewGroup) findViewById(R.id.app)).addView(initializeForView(app, config));
            }
        });


        // signed
        if (getResources().getBoolean(R.bool.connect_games) && GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN.getScopeArray()))
            onSignIn();

        // AdMob
        adMob();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 100)
            if (resultCode == RESULT_OK) {
                GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                if (result.isSuccess()) // sign ok
                    onSignIn();
                else { // sign fail
                    String message = result.getStatus().getStatusMessage();
                    if (message == null || message.isEmpty())
                        message = getString(R.string.error_sign_in);
                    TOAST(message);
                    onSignOut();
                }
            } else {
                TOAST(getString(R.string.error_sign_in));
                onSignOut();
            }
    }

    @Override
    protected void onDestroy() {
        // destroy AdMob
        if (adMobBanner != null) {
            adMobBanner.setAdListener(null);
            adMobBanner.destroy();
            adMobBanner = null;
        }
        if (adMobInterstitial != null) {
            adMobInterstitial.setAdListener(null);
            adMobInterstitial = null;
        }

        adRequest = null;

        super.onDestroy();
    }

    // onSignIn
    void onSignIn() {
        isSigned = true;

        // set signed
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                app.setSigned(true);
            }
        });

        // save score to leaderboard
        if (score > 0)
            saveScore(score);

        // show leaders
        if (showLeaders) {
            showLeaders = false;
            showLeaders();
        }

        // get score from leaderboard
        Games.getLeaderboardsClient(this, GoogleSignIn.getLastSignedInAccount(this)).loadCurrentPlayerLeaderboardScore(getString(R.string.leaderboard), LeaderboardVariant.TIME_SPAN_ALL_TIME, LeaderboardVariant.COLLECTION_PUBLIC).addOnSuccessListener(this, new OnSuccessListener<AnnotatedData<LeaderboardScore>>() {
            @Override
            public void onSuccess(final AnnotatedData<LeaderboardScore> leaderboardScoreAnnotatedData) {
                if (leaderboardScoreAnnotatedData != null && leaderboardScoreAnnotatedData.get() != null) {
                    Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            app.saveScore((int) leaderboardScoreAnnotatedData.get().getRawScore()); // save score local
                        }
                    });
                }
            }
        });
    }

    // onSignOut
    void onSignOut() {
        isSigned = false;
        showLeaders = false;

        // set signed
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                app.setSigned(false);
            }
        });
    }

    // TOAST
    void TOAST(String mess) {
        if (toast != null)
            toast.cancel();
        toast = Toast.makeText(this, mess, Toast.LENGTH_SHORT);
        ((TextView) toast.getView().findViewById(android.R.id.message)).setGravity(Gravity.CENTER);
        toast.show();
    }

    // log
    void log(Object obj) {
        Log.d("@", String.valueOf(obj));
    }

    @Override
    public void saveScore(int score) {
        // called when game score has been changed
        this.score = score;

        if (getResources().getBoolean(R.bool.connect_games) && isSigned)
            Games.getLeaderboardsClient(this, GoogleSignIn.getLastSignedInAccount(this)).submitScore(getString(R.string.leaderboard), score);
    }

    @Override
    public void signIn() {
        // called when pressed "Sign In" to Google Play Game Services
        if (getResources().getBoolean(R.bool.connect_games)) {
            final GoogleSignInClient signInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN);
            signInClient.silentSignIn().addOnCompleteListener(this, new OnCompleteListener<GoogleSignInAccount>() {
                @Override
                public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                    if (task.isSuccessful()) // silent sign ok
                        onSignIn();
                    else // silent sign fail
                        startActivityForResult(signInClient.getSignInIntent(), 100);
                }
            });
        }
    }

    @Override
    public void signOut() {
        // called when pressed "Sign Out" from Google Play Game Services
        if (getResources().getBoolean(R.bool.connect_games)) {
            GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN).signOut().addOnCompleteListener(this, new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    onSignOut();
                }
            });
        }
    }

    @Override
    public void rate() {
        // called if need to rate the App
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
    }

    @Override
    public void showLeaders() {
        // called when pressed "Leaders"
        if (getResources().getBoolean(R.bool.connect_games)) {
            if (isSigned)
                Games.getLeaderboardsClient(this, GoogleSignIn.getLastSignedInAccount(this)).getLeaderboardIntent(getString(R.string.leaderboard)).addOnSuccessListener(new OnSuccessListener<Intent>() {
                    @Override
                    public void onSuccess(Intent intent) {
                        try {
                            startActivityForResult(intent, 200);
                        } catch (Exception e) {
                            TOAST(getString(R.string.error_games_exists));
                        }
                    }
                });
            else {
                showLeaders = true;
                signIn();
            }
        }
    }

    @Override
    public void admobInterstitial() {
        // called to show AdMob Interstitial
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (adMobInterstitial != null)
                    if (adMobInterstitial.isLoaded())
                        adMobInterstitial.show(); // show
                    else if (!adMobInterstitial.isLoading())
                        adMobInterstitial.loadAd(adRequest); // load
            }
        });
    }

    // adMob
    void adMob() {
        if (getResources().getBoolean(R.bool.show_admob))
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MobileAds.initialize(AndroidLauncher.this);

                    // make AdMob request
                    Builder builder = new AdRequest.Builder();
                    if (getResources().getBoolean(R.bool.admob_test))
                        builder.addTestDevice(AdRequest.DEVICE_ID_EMULATOR).addTestDevice(MD5(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID)));
                    adRequest = builder.build();

                    // AdMob Interstitial
                    adMobInterstitial = new InterstitialAd(AndroidLauncher.this);
                    adMobInterstitial.setAdUnitId(getString(R.string.adMob_interstitial));
                    adMobInterstitial.setAdListener(new AdListener() {
                        public void onAdClosed() {
                            adMobInterstitial.loadAd(adRequest);
                        }
                    });

                    // AdMob Banner
                    adMobBanner = new AdView(AndroidLauncher.this);
                    adMobBanner.setAdUnitId(getString(R.string.adMob_banner));
                    adMobBanner.setAdSize(AdSize.SMART_BANNER);
                    ((ViewGroup) findViewById(R.id.admob)).addView(adMobBanner);
                    adMobBanner.setAdListener(new AdListener() {
                        @Override
                        public void onAdLoaded() {
                            super.onAdLoaded();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    findViewById(R.id.admob).setVisibility(View.VISIBLE);
                                }
                            });
                        }

                        @Override
                        public void onAdLeftApplication() {
                            super.onAdLeftApplication();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    findViewById(R.id.admob).setVisibility(View.GONE);
                                }
                            });
                        }
                    });

                    // load AdMob
                    adMobBanner.loadAd(adRequest);
                    adMobInterstitial.loadAd(adRequest);
                }
            });
    }

    // MD5
    String MD5(String str) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(str.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i)
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
            return sb.toString().toUpperCase(Locale.ENGLISH);
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        return null;
    }
}
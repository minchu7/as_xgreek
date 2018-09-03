package org.oldservant.greekinterlinear;

import java.util.Locale;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;
import android.content.Context;
public class phonegapSpeech{

    private static final String LOG_TAG = "TTS";
    private static final int STOPPED = 0;
    private static final int INITIALIZING = 1;
    private static final int STARTED = 2;
    private TextToSpeech mTts = null;
    private int state = STOPPED;
    private Context appCtx;
    private final String utteranceId = "phoneGap";
    phonegapSpeech( Context appCt){
        appCtx = appCt;
    }

    private Set<Voice> voiceList = null;

    public String execute(String action, JSONArray args) {
        try {
            if (action.equals("speak")) {
                JSONObject utterance = args.getJSONObject(0);
                String text = utterance.getString("text");

                String lang = utterance.optString("lang", "en");
                mTts.setLanguage(new Locale(lang));

                String voiceCode = utterance.optString("voiceURI", null);
                if (voiceCode == null) {
                    JSONObject voice = utterance.optJSONObject("voice");
                    if (voice != null) {
                        voiceCode = voice.optString("voiceURI", null);
                    }
                }
                if (voiceCode != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    for (Voice v : this.voiceList) {
                        if (voiceCode.equals(v.getName())) {
                            mTts.setVoice(v);
                        }
                    }
                }

                float pitch = (float)utterance.optDouble("pitch", 1.0);
                mTts.setPitch(pitch);

                float volume = (float)utterance.optDouble("volume", 0.5);
                // how to set volume

                float rate = (float)utterance.optDouble("rate", 1.0);
                mTts.setSpeechRate(rate);

                if (isReady()) {
                    mTts.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId);
                } else {
                    return "error";
                }
            } else if (action.equals("cancel")) {
                if (isReady()) {
                    mTts.speak("", TextToSpeech.QUEUE_FLUSH, null, null);
                } else {
                    return "error";
                }
            } else if (action.equals("pause")) {
                Log.d(LOG_TAG, "Not implemented yet");
            } else if (action.equals("resume")) {
                Log.d(LOG_TAG, "Not implemented yet");
            } else if (action.equals("stop")) {
                if (isReady()) {
                    mTts.stop();
                    return "ok";
                } else {
                    return "error";
                }
            } else if (action.equals("silence")) {
                if (isReady()) {
                    mTts.playSilentUtterance (args.getLong(0), TextToSpeech.QUEUE_ADD, utteranceId);
                } else {
                    return "error";
                }
            } else if (action.equals("startup")) {
                state = phonegapSpeech.INITIALIZING;
                final Object syncObject = new Object();
                mTts = new TextToSpeech(appCtx, new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (mTts != null && status == TextToSpeech.SUCCESS) {
                            state = phonegapSpeech.STARTED;
                        }else{
                            state = phonegapSpeech.STOPPED;
                            Log.d(LOG_TAG, "Init failed:"+status);
                        }
                        synchronized( syncObject) {
                            syncObject.notify();
                        }
                    }
                });
                mTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onDone(String utteranceId) {
                        Log.d(LOG_TAG, "got completed utterance");
                    }
                    @Override
                    public void onError(String utteranceId) {
                        Log.d(LOG_TAG, "got utterance error");
                    }
                    @Override
                    public void onStart(String utteranceId) {
                        Log.d(LOG_TAG, "started talking");
                    }
                });
                synchronized(syncObject) {
                    try {
                        syncObject.wait();
                        if( isReady())
                            return getVoices();
                        else
                            return "error";
                    } catch (InterruptedException e) {
                        Log.d(LOG_TAG, "InterruptedException:"+e.getMessage());
                    }
                }
            }
            else if (action.equals("shutdown")) {
                if (mTts != null) {
                    mTts.shutdown();
                }
            }
            else if (action.equals("isLanguageAvailable")) {
                if (mTts != null) {
                    Locale loc = new Locale(args.getString(0));
                    int available = mTts.isLanguageAvailable(loc);
                    String result = (available < 0) ? "no" : "ok";
                    return result;
                }
            }
            return "ok";
        } catch (JSONException e) {
            Log.d(LOG_TAG, "JSON_EXCEPTION:"+e.getMessage());
        }
        return "error";
    }

    private String getVoices() {
        JSONArray voices = new JSONArray();
        JSONObject voice;
        this.voiceList = mTts.getVoices();
            for (Voice v : this.voiceList) {
            Locale locale = v.getLocale();
            voice = new JSONObject();
            try {
                voice.put("voiceURI", v.getName());
                voice.put("name", locale.getDisplayLanguage(locale) + " " + locale.getDisplayCountry(locale));
                //voice.put("features", v.getFeatures());
                //voice.put("displayName", locale.getDisplayLanguage(locale) + " " + locale.getDisplayCountry(locale));
                voice.put("lang", locale.getLanguage()+"-"+locale.getCountry());
                voice.put("localService", !v.isNetworkConnectionRequired());
                voice.put("quality", v.getQuality());
                voice.put("default", false);
            } catch (JSONException e) {
                Log.d(LOG_TAG, "JSON_EXCEPTION:"+e.getMessage());
            }
            voices.put(voice);
        }
        return voices.toString();
    }
    /**
     * Is the TTS service ready to play yet?
     *
     */
    private boolean isReady() {
        return (state == phonegapSpeech.STARTED);
    }

}

package fr.d3vx.rcpi;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import fr.d3vx.rcpi.udp.Config;
import fr.d3vx.rcpi.udp.UDP;
import fr.d3vx.rcpi.view.SpinnerAdapter;

public class MainActivity extends AppCompatActivity {

    private SparseIntArray cmds;

    public static String TAG = "MainActivity";

    private Spinner sItems;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> medias_spinner;

    public static String ERROR_ACTION = "error";
    public static String SERVER_MSG_ACTION = "udp";
    public static String SERVER_MSG_KEY = "msg";

    private UDP udp;
    private Config config;

    private View temoin;
    private TextView tv_ip;
    private EditText edit_url;

    private ArrayList<String> medias;

    private int mediaDuration;
    private int mediaCursor;
    private float mediaProgress;
    private boolean isMediaPlaying;
    private String currentMediaPath;


    private Runnable mediaInterval;
    private Handler mediaIntervalHandler;

    private TextView mediaProgressText;
    private ProgressBar progressBar;

    private String mediaDurationTxt;

    private ClipboardManager clipboardManager;

    private ErrorReceiver errorReceiver;
    private MessageReceiver messageReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        config = Config.getInstance(this, false);
        udp = UDP.getInstance(this, config);

        temoin = findViewById(R.id.temoin);
        tv_ip = (TextView) findViewById(R.id.tv_ip);
        edit_url = (EditText) findViewById(R.id.edit_url);
        progressBar = (ProgressBar) findViewById(R.id.mediaProgess);
        mediaProgressText = (TextView) findViewById(R.id.mediaProgessText);
        cmds = new SparseIntArray();
        medias_spinner = new ArrayList<String>();

        findViewById(R.id.audiotv).bringToFront();
        findViewById(R.id.subtltv).bringToFront();
        findViewById(R.id.videotv).bringToFront();

        cmds.put(R.id.but_play, RCPi.KEYS.PLAY);
        cmds.put(R.id.but_back600, RCPi.KEYS.PLAYBACK_BACKWARD600);
        cmds.put(R.id.but_back30, RCPi.KEYS.PLAYBACK_BACKWARD30);
        cmds.put(R.id.but_fwd30, RCPi.KEYS.PLAYBACK_FORWARD30);
        cmds.put(R.id.but_fwd600, RCPi.KEYS.PLAYBACK_FORWARD600);
        cmds.put(R.id.but_volup, RCPi.KEYS.AUDIO_VOL_UP);
        cmds.put(R.id.but_voldown, RCPi.KEYS.AUDIO_VOL_DOWN);
        cmds.put(R.id.but_audionext, RCPi.KEYS.AUDIO_TRACK_NEXT);
        cmds.put(R.id.but_audioprev, RCPi.KEYS.AUDIO_TRACK_PREV);
        cmds.put(R.id.but_subtitles, RCPi.KEYS.SUBTITLE_TOGGLE);
        cmds.put(R.id.but_subtitlesnext, RCPi.KEYS.SUBTITLE_TRACK_NEXT);
        cmds.put(R.id.but_subtitlesprev, RCPi.KEYS.SUBTITLE_TRACK_PREV);
        cmds.put(R.id.but_subtitlesdelaydown, RCPi.KEYS.SUBTITLE_DELAY_DEC);
        cmds.put(R.id.but_subtitlesdelayup, RCPi.KEYS.SUBTITLE_DELAY_INC);
        cmds.put(R.id.but_infos, RCPi.KEYS.INFOS);
        cmds.put(R.id.but_exit, RCPi.KEYS.QUIT);

        cmds.put(R.id.but_clear, -1);
        cmds.put(R.id.but_open, -1);
        cmds.put(R.id.but_update, -1);
        cmds.put(R.id.but_voloff, -1);
        cmds.put(R.id.but_list, -1);

        for (int i = 0; i < cmds.size(); i++) {
            buttonEffect(findViewById(cmds.keyAt(i)));
        }

        temoin.setBackgroundColor(Color.GREEN);

        sItems = (Spinner) findViewById(R.id.spinner);
        adapter = new SpinnerAdapter(MainActivity.this, R.layout.spinner_item, medias_spinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sItems.setAdapter(adapter);

        errorReceiver = new ErrorReceiver();
        messageReceiver = new MessageReceiver();

        mediaProgress = 0;
        isMediaPlaying = false;

        mediaIntervalHandler = new Handler();
        mediaInterval = new Runnable() {
            public void run() {
                if (isMediaPlaying) {
                    mediaCursor += 1000;

                    updateProgress();

                    if (isMediaPlaying) {
                        mediaIntervalHandler.postDelayed(this, 1000);
                    }
                }
            }
        };

        // Get clipboard manager object.
        Object clipboardService = getSystemService(CLIPBOARD_SERVICE);
        clipboardManager = (ClipboardManager) clipboardService;
    }

    @Override
    protected void onStart() {
        super.onStart();
        log(TAG,"UDP start");
        config = Config.getInstance(this, true);
        tv_ip.setText(config.uri);
        //checkIntent(getIntent());
        if(isMediaPlaying){
            mediaIntervalHandler.postDelayed(mediaInterval, 1000);
        }
        registerReceiver(errorReceiver, new IntentFilter(ERROR_ACTION));
        registerReceiver(messageReceiver, new IntentFilter(SERVER_MSG_ACTION));
        udp.start();
        udp.send(RCPi.KEYS.PING);

        checkIntent(getIntent());
    }

    @Override
    protected void onStop() {
        super.onStop();
        UDP.close();
        unregisterReceiver(errorReceiver);
        unregisterReceiver(messageReceiver);
        mediaIntervalHandler.removeCallbacks(mediaInterval);
    }

    /**
     * Check if we have any text data incoming from the intent
     */
    private void checkIntent(Intent intent){
        // Get intent, action and MIME type
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    // Update UI to reflect text being shared
                    edit_url.setText(sharedText);
                    if (sharedText.length() > 0) {
                        udp.send(RCPi.KEYS.OPEN, sharedText);
                    }

                    intent.removeExtra(Intent.EXTRA_TEXT);
                }
            }

        }
    }

    /**
     *
     * @param {int} cursor: seconds
     */
    void updateCursorText(int cursor){
        if (cursor >= 0) {
            mediaProgressText.setText(String.format(Locale.FRANCE, "%s / %s", secToHours(cursor / 1000), mediaDurationTxt));
        }
        else{
            mediaProgressText.setText("");
        }
    }

    // Update media progess view
    void updateProgress(){
        mediaProgress = (mediaCursor / (float) mediaDuration) * 100;
        if (mediaProgress >= 100) {
            mediaProgress = 100;
            progressBar.setProgress(100);
            mediaCursor = mediaDuration;
            isMediaPlaying = false;
        }
        updateCursorText(mediaCursor);
        progressBar.setProgress((int) mediaProgress);
    }


    void log(String tag, String msg){
        if (config.debug){
            Log.d(tag,msg);
        }
    }
    void logW(String tag, String msg){
        if (config.debug){
            Log.d(tag,msg);
        }
    }



    public void buttonClicked(View v) {
        switch (v.getId()) {
            case R.id.but_open:
                String url = edit_url.getText().toString();
                if (url.length() > 0) {
                    udp.send(RCPi.KEYS.OPEN, url);
                } else {
                    if (medias != null) {
                        int fid = sItems.getSelectedItemPosition();
                        String media = medias.get(fid);
                        udp.send(RCPi.KEYS.OPEN, media);
                    }
                }
                break;
            case R.id.but_list:
                udp.send(RCPi.KEYS.LIST);
                break;
            case R.id.paste_but:
                ClipData clipData = clipboardManager.getPrimaryClip();
                // Get item count.
                int itemCount = clipData.getItemCount();
                if (itemCount > 0) {
                    ClipData.Item item = clipData.getItemAt(0);
                    edit_url.setText(item.getText().toString());
                }
                break;
            case R.id.but_update:
                udp.send(RCPi.KEYS.PING);
                break;
            case R.id.but_prefs:
                Intent i = new Intent(this, PrefsActivity.class);
                startActivity(i);
                break;
            case R.id.but_voloff:
                log(TAG, "voloff not implemented yet");
                break;
            case R.id.but_clear:
                edit_url.setText("");
                break;

            default:
                int cmd = cmds.get(v.getId());
                if (cmd > -1) {
                    log(TAG, "id :" + cmd);
                    udp.send(cmd);
                    if (v.getId() == R.id.but_exit) {
                        isMediaPlaying = false;
                        mediaIntervalHandler.removeCallbacks(mediaInterval);
                        updateCursorText(-1);
                        progressBar.setProgress(0);
                    }
                }

        }
        log(TAG, "clicked " + v.getId());
    }

    /**
     * Visual effects for the buttons
     * @param button
     */
    public static void buttonEffect(View button) {
        button.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        v.getBackground().setColorFilter(0xFF41A93A, PorterDuff.Mode.SRC_ATOP);
                        ((ImageButton) v).getDrawable().setColorFilter(new ColorMatrixColorFilter(NEGATIVE));
                        v.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        v.getBackground().clearColorFilter();
                        ((ImageButton) v).getDrawable().clearColorFilter();
                        v.invalidate();
                        break;
                    }
                }
                return false;
            }
        });
    }


    class ErrorReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            temoin.setBackgroundColor(Color.RED);
        }
    }


    class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            temoin.setBackgroundColor(Color.GREEN);
            if (SERVER_MSG_ACTION.equals(intent.getAction())) {
                byte[] data = intent.getByteArrayExtra(SERVER_MSG_KEY);
                if (data == null){
                    return;
                }

                try {
                    MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data);
                    unpackMsg(unpacker);
                    unpacker.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Unpack message using msgpack and display data
     */
    void unpackMsg(MessageUnpacker unpacker) throws IOException {
        if (unpacker.hasNext()) {
            int nb = unpacker.unpackArrayHeader();
            if (nb == 0) {
                logW("unpackMsg", "weird, cant get ArrayData from packet");
                return;
            }
            int base = unpacker.unpackInt();
            if (base != 6) {
                logW("unpackMsg", "weird, first packet entry should be 6");
                return;
            }

            int action = unpacker.unpackInt();
            if (action == RCPi.KEYS.LIST) {
                int l = unpacker.unpackArrayHeader();
                medias_spinner.clear();
                ArrayList<String> list = new ArrayList<String>();
                String d = "";
                for (int i = 0; i < l; i++) {
                    String s = unpacker.unpackString();
                    String[] f = s.split("/");
                    medias_spinner.add(f[f.length - 1]);
                    list.add(s);
                    d = d+s+(i < l-1 ? ", " : "");
                }
                log("unpackMsg", "got a list of films"+d);
                adapter.notifyDataSetChanged();
                medias = list;
            } else if (action == RCPi.KEYS.FINFOS) {
                int nb_data = unpacker.unpackArrayHeader();
                if (nb_data == 0) {
                    logW("unpackMsg", "Finfos no data ?");
                    return;
                }

                mediaCursor = unpacker.unpackInt();
                log("unpackMsg", "Finfos : CURSOR :"+mediaCursor);
                if (nb_data == 1) {
                    updateProgress();
                    return;
                }

                boolean isPlaying = unpacker.unpackBoolean();
                if (isPlaying) {
                    if (!isMediaPlaying) {
                        isMediaPlaying = true;
                        mediaIntervalHandler.postDelayed(mediaInterval, 1000);
                    }
                } else {
                    if (isMediaPlaying) {
                        isMediaPlaying = false;
                        mediaIntervalHandler.removeCallbacks(mediaInterval);
                        progressBar.setProgress((int) mediaProgress);
                    }
                }
                log("unpackMsg", "Finfos : MEDIA_PLAYING :"+isPlaying);
                if (nb_data == 2) {
                    updateProgress();
                    return;
                }

                mediaDuration = unpacker.unpackInt();
                mediaDurationTxt = secToHours(mediaDuration / 1000);
                log("unpackMsg", "Finfos : MEDIA_DURATION :"+mediaDuration);

                if (nb_data == 3) {
                    updateProgress();
                    return;
                }


                currentMediaPath = unpacker.unpackString();
                log("unpackMsg", "Finfos : MEDIA_PATH :"+currentMediaPath);
                if (currentMediaPath.length() > 0) {
                    if (currentMediaPath.startsWith("/")) {
                        sItems.setSelection(this.medias.indexOf(currentMediaPath));
                    } else {
                        edit_url.setText(currentMediaPath);
                    }
                }

                updateProgress();
            }
        }
    }

    public String secToHours(int sec) {
        int hours = sec / 3600;
        int minutes = (sec % 3600) / 60;
        int seconds = sec % 60;
        return String.format(Locale.FRANCE, "%02dh%02dm%02ds", hours, minutes, seconds);
    }

    private static final float[] NEGATIVE = {
            -1.0f, 0, 0, 0, 255, // red
            0, -1.0f, 0, 0, 255, // green
            0, 0, -1.0f, 0, 255, // blue
            0, 0, 0, 1.0f, 0  // alpha
    };
}

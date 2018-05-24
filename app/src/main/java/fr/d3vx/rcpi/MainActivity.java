package fr.d3vx.rcpi;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import fr.d3vx.rcpi.udp.Config;
import fr.d3vx.rcpi.udp.UDPClient;
import fr.d3vx.rcpi.udp.UDPServer;

public class MainActivity extends AppCompatActivity{

    private Map<Integer, Integer> cmds;

    public static String TAG = "MainActivity";

    private Spinner sItems;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> films_spinner;

    public static String ERROR_ACTION = "error";
    public static String MESSAGE_ACTION = "nm";
    public static String MESSAGE_KEY = "data";

    private UDPServer server;
    private UDPClient client;
    private Config config;

    private View temoin;
    private EditText edit_ip;
    private EditText edit_url;

    private JSONArray films;

    private int filmDuration;
    private int filmCursor;
    private float filmProgress;
    private boolean isFilmPlaying;

    private Runnable filmInterval;
    private Handler filmIntervalHandler;

    private TextView filmProgressText;
    private ProgressBar progressBar;

    private String filmDurationTxt;

    public String secToHours(int sec){
        int hours = sec / 3600;
        int minutes = (sec % 3600) / 60;
        int seconds = sec % 60;
        return String.format(Locale.FRANCE, "%02dh%02dm%02ds", hours, minutes, seconds);
    }

    private ClipboardManager clipboardManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String ip = "192.168.0.7";
        int port = 9878;

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        String prefIp = sharedPref.getString("ip", ip+":"+port);

        String[] d = prefIp.split(":");
        if (d.length == 2){
            ip = d[0];
            port = Integer.valueOf(d[1]);
        }



        config = Config.getInstance(port, ip);

        temoin = findViewById(R.id.temoin);
        edit_ip = (EditText)findViewById(R.id.edit_ip);
        edit_url = (EditText)findViewById(R.id.edit_url);
        progressBar = (ProgressBar) findViewById(R.id.filmProgess);
        filmProgressText = (TextView) findViewById(R.id.filmProgessText);
        cmds = new HashMap<Integer, Integer>();
        films_spinner = new ArrayList<String>();


        cmds.put(R.id.but_play, 1);
        cmds.put(R.id.but_back600, 4);
        cmds.put(R.id.but_back30, 5);
        cmds.put(R.id.but_fwd30, 6);
        cmds.put(R.id.but_fwd600, 7);
        cmds.put(R.id.but_volup, 10);
        cmds.put(R.id.but_voldown, 11);
        cmds.put(R.id.but_audionext, 8);
        cmds.put(R.id.but_audioprev, 9);
        cmds.put(R.id.but_subtitles, 12);
        cmds.put(R.id.but_subtitlesnext, 13);
        cmds.put(R.id.but_subtitlesprev, 14);
        cmds.put(R.id.but_subtitlesdelaydown, 15);
        cmds.put(R.id.but_subtitlesdelayup, 16);
        cmds.put(R.id.but_infos, 17);
        cmds.put(R.id.but_exit, 18);

        cmds.put(R.id.but_clear, -1);
        cmds.put(R.id.but_open, -1);
        cmds.put(R.id.but_update, -1);
        cmds.put(R.id.but_voloff, -1);
        cmds.put(R.id.but_reload, -1);

        for (int bId : cmds.keySet()){
            buttonEffect(findViewById(bId));
        }

        edit_ip.setText(ip+":"+port);

        temoin.setBackgroundColor(Color.GREEN);

        sItems = (Spinner) findViewById(R.id.spinner);
        adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, films_spinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sItems.setAdapter(adapter);


        registerReceiver(new ErrorReceiver(), new IntentFilter(ERROR_ACTION));
        registerReceiver(new MessageReceiver(), new IntentFilter(MESSAGE_ACTION));


        client = new UDPClient(this, config);
        server = new UDPServer(this, config);
        server.start();

        filmProgress = 0;
        isFilmPlaying = false;

        filmIntervalHandler = new Handler();
        filmInterval = new Runnable(){
            public void run(){
                if (isFilmPlaying) {
                    filmCursor += 1000;
                    filmIntervalHandler.postDelayed(this, 1000);
                    filmProgress = (filmCursor / (float)filmDuration) * 100;

                    if (filmProgress >= 100){
                        progressBar.setProgress(100);
                        filmProgressText.setText(String.format(Locale.FRANCE, "%s / %s", secToHours(filmDuration/1000), filmDurationTxt));
                        isFilmPlaying = false;
                        return;
                    }

                    filmProgressText.setText(String.format(Locale.FRANCE, "%s / %s", secToHours(filmCursor/1000), filmDurationTxt));
                    progressBar.setProgress((int)filmProgress);
                }
            }
        };

        // Get clipboard manager object.
        Object clipboardService = getSystemService(CLIPBOARD_SERVICE);
        clipboardManager = (ClipboardManager)clipboardService;

        client.send("ping");

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    // Update UI to reflect text being shared
                    edit_url.setText(sharedText);
                    if (sharedText.length() > 0){
                        client.send(String.format("open|%s",sharedText));
                    }
                }
            }

        }
    }

    public void buttonClicked(View v){
        switch(v.getId()){
            case R.id.but_open:
                String url = edit_url.getText().toString();
                if (url.length() > 0){
                    client.send(String.format("open|%s",url));
                }
                else{
                    if (films != null){
                        int fid = sItems.getSelectedItemPosition();
                        try {
                            String film = films.getString(fid);

                            client.send(String.format("open|%s",film));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
            case R.id.but_reload:
                //client.send("{\"key\":\"reload\"}");
                client.send("reload");
                break;
            case R.id.paste_but:
                ClipData clipData = clipboardManager.getPrimaryClip();
                // Get item count.
                int itemCount = clipData.getItemCount();
                if(itemCount > 0){
                    ClipData.Item item = clipData.getItemAt(0);
                    edit_url.setText(item.getText().toString());
                }
                break;
            case R.id.but_update:
                String ip = edit_ip.getText().toString();
                if (ip.length() > 0){
                    try {
                        String[] d = ip.split(":");
                        if (d.length == 2){
                            config.address = InetAddress.getByName(d[0]);
                            int port = Integer.valueOf(d[1]);
                            if (port > 6000 && port < 12000){
                                SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.putString("ip", ip);
                                editor.apply();
                            }
                        }
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                }

                client.send("ping");
                break;
            case R.id.but_voloff:
                Log.d(TAG,"voloff");
                break;
            case R.id.but_clear:
                edit_url.setText("");
                break;

            default:
                int cmd = cmds.get(v.getId());
                if (cmd > -1){
                    Log.d(TAG,"id :"+cmd);
                    client.send(String.format(Locale.FRANCE,"$%d",cmd));
                    if (v.getId() == R.id.but_exit){
                        isFilmPlaying = false;
                        filmIntervalHandler.removeCallbacks(filmInterval);
                        filmProgressText.setText(String.format(Locale.FRANCE, "%s / %s", 0, 0));
                        progressBar.setProgress(0);
                    }
                }

        }
        Log.d(TAG, "clicked "+v.getId());
    }

    public static void buttonEffect(View button){
        button.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        v.getBackground().setColorFilter(0xFF41A93A, PorterDuff.Mode.SRC_ATOP);
                        ((ImageButton)v).getDrawable().setColorFilter(new ColorMatrixColorFilter(NEGATIVE));
                        v.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        v.getBackground().clearColorFilter();
                        ((ImageButton)v).getDrawable().clearColorFilter();
                        v.invalidate();
                        break;
                    }
                }
                return false;
            }
        });
    }
    private static final float[] NEGATIVE = {
            -1.0f,     0,     0,    0, 255, // red
            0, -1.0f,     0,    0, 255, // green
            0,     0, -1.0f,    0, 255, // blue
            0,     0,     0, 1.0f,   0  // alpha
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        server.close();
        client.close();
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

            String message = intent.getStringExtra(MESSAGE_KEY);
            Log.d(TAG, "received message : "+message);

            JSONObject jObj = null;
            try {
                jObj = new JSONObject(message);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (jObj == null){
                return;
            }

            try {
                String action = jObj.getString("action");
                if ("list".equals(action)) {
                    JSONArray jArray = jObj.getJSONArray("data");
                    films_spinner.clear();
                    for (int i = 0; i < jArray.length(); i++) {
                        String[] f = jArray.getString(i).split("/");
                        films_spinner.add(f[f.length - 1]);
                    }
                    adapter.notifyDataSetChanged();
                    films = jArray;
                }
                else if ("finfos".equals(action)) {
                    JSONObject data = jObj.getJSONObject("data");

                    if (data.has("cursor")){
                        filmCursor = data.getInt("cursor");
                    }
                    if (data.has("duration")){
                        filmDuration = data.getInt("duration");
                        filmDurationTxt = secToHours(filmDuration/1000);
                    }

                    if (data.has("action")){
                        if ("play".equals(data.getString("action"))){
                            if (!isFilmPlaying) {
                                isFilmPlaying = true;
                                filmIntervalHandler.postDelayed(filmInterval, 1000);
                            }
                        }
                        else if ("stop".equals(data.getString("action"))) {
                            if (isFilmPlaying) {
                                isFilmPlaying = false;
                                filmIntervalHandler.removeCallbacks(filmInterval);
                                filmProgressText.setText(String.format(Locale.FRANCE, "%s / %s", secToHours(filmCursor/1000), filmDurationTxt));
                                progressBar.setProgress((int)filmProgress);
                            }
                        }
                    }

                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}

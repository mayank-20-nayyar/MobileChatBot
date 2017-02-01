package com.example.mayank.chat_bot;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.AlarmClock;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    Button call;
    Button alarm;
    Button music;
    Button tableData;
    Map<String,String> contacts = new HashMap<String,String>();
    String receivedMessage = "";
    String toCall = "";
    String toPlay = "";
    public String toSearchTable = "";
    String displayMenu = "";
    EditText ed;
    EditText playSong;

    private static final String TAG = "ChatActivity";

    private ChatArrayAdapter chatArrayAdapter;
    private ListView listView;
    private EditText chatText;
    private Button buttonSend;
    private boolean side = false;

    getTable gettable;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);

        buttonSend = (Button) findViewById(R.id.send);

        listView = (ListView) findViewById(R.id.msgview);

        chatArrayAdapter = new ChatArrayAdapter(getApplicationContext(), R.layout.righht);
        listView.setAdapter(chatArrayAdapter);

        chatText = (EditText) findViewById(R.id.msg);
        chatText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    return sendChatMessage();
                }
                return false;
            }
        });
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Log.e("above","chat");
                receivedMessage = chatText.getText().toString();
                sendChatMessage();
                Log.e("below","chat");
                Log.e("the",receivedMessage);
                decodeMessage(receivedMessage);
            }
        });

        listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        listView.setAdapter(chatArrayAdapter);

        //to scroll the list view to bottom on data change
        chatArrayAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(chatArrayAdapter.getCount() - 1);
            }
        });

    }


    void decodeMessage(String message)
    {
        if(message.contains("call"))
        {
            message = message.replace("call", "");
            message = message.replaceAll("\\s+", "");
            toCall = message;
            searchContact();
            sendBotMessage("hope you enjoyed your call");
        }
        if(message.contains("play song"))
        {
            message = message.replace("play song", "");
            toPlay = message;
            getMusic();
        }
        if(message.contains("alarm"))
        {
            message = message.replace("alarm", "");
            message = message.replaceAll("\\s+", "");
            setAlarm(message);
        }
        if(message.contains("menu"))
        {
            message = message.replace("menu","");
            message = message.replaceAll("\\s+", "");
            toSearchTable = message;
            Log.e("to decode", toSearchTable);
            new getTable(this).execute();
        }
    }

    void getMenu(String[] foodInMess)
    {
        displayMenu = "";
        for(int i = 0; i < foodInMess.length ; i++ )
        {
            if(i == 0)
                displayMenu += "BREAKFAST: \n";
            if(i == 1)
                displayMenu += "LUNCH: \n";
            if(i == 2)
                displayMenu += "DINNER: \n";
            displayMenu += foodInMess[i] + "\n\n";
        }
        sendBotMessage(displayMenu);
    }

    void setAlarm(String mes)
    {
        String timeInString = mes;
        float timeInFloat;
        int hoursInInt;
        float minutesInFloat;
        int minutesInInt;

        timeInFloat = Float.parseFloat(timeInString);
        hoursInInt = (int)timeInFloat;
        minutesInFloat = timeInFloat - (int)timeInFloat;
        minutesInFloat = minutesInFloat*100;
        minutesInInt = (int)minutesInFloat;
        Intent in = new Intent(AlarmClock.ACTION_SET_ALARM);
        in.putExtra(AlarmClock.EXTRA_HOUR, hoursInInt);
        in.putExtra(AlarmClock.EXTRA_MINUTES, minutesInInt);
        startActivity(in);

    }

    void getMusic()
    {
        //toPlay = playSong.getText().toString();
        toPlay = toPlay.replaceAll("\\s+","");
        toPlay = toPlay.toLowerCase();

        ContentResolver cr = this.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cur = cr.query(uri, null, selection, null, sortOrder);
        int count = 0;

        if(cur != null)
        {
            count = cur.getCount();

            if(count > 0)
            {
                while(cur.moveToNext())
                {
                    String data = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA));
                    Log.e("the list is", data);
                    String tempData = data.replaceAll("\\s+","");
                    tempData = tempData.toLowerCase();
                    if (tempData.contains(toPlay))
                        playMusic(data);
                }

            }
        }

        cur.close();
    }

    void playMusic(String path)
    {
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        File file = new File(path);
        intent.setDataAndType(Uri.fromFile(file), "audio/*");
        startActivity(intent);
    }

    void searchContact()
    {
        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);

        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                String id = cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(
                        ContactsContract.Contacts.DISPLAY_NAME));

                if (cur.getInt(cur.getColumnIndex(
                        ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?",
                            new String[]{id}, null);
                    while (pCur.moveToNext()) {
                        String phoneNo = pCur.getString(pCur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER));
                        contacts.put(phoneNo, name);
                    }
                    pCur.close();
                }
            }
        }
        matchContact();
    }

    void  matchContact()
    {
        Iterator it = contacts.keySet().iterator();
        while(it.hasNext()) {
            String key=(String)it.next();
            String value=(String)contacts.get(key);
            if (value.equals(toCall) == true)
            {
                makeCall(key);
            }
        }

    }

    void makeCall(String toCallNum)
    {
        String uri = "tel:" + toCallNum.trim();
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse(uri));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        startActivity(intent);
    }

    private boolean sendChatMessage() {
        chatArrayAdapter.add(new ChatMessage(side, chatText.getText().toString()));
        chatText.setText("");
        //chatArrayAdapter.add(new ChatMessage(!side, "ok"));
        //side = !side;
        return true;
    }

    private boolean sendBotMessage(String mes) {
        chatArrayAdapter.add(new ChatMessage(!side, mes));
        //side = !side;
        return true;
    }
}

class getTable extends AsyncTask<String, String, String>
{
    MainActivity mainActivity ;
    String toSearch = "";
    public String[] foodInMess = new String[3];

    public getTable(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    protected String doInBackground(String... params) {

        String server_response = null;
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget= new HttpGet("https://www.googleapis.com/fusiontables/v2/query?sql=SELECT%20*%20FROM%201TTXujtH1aegxqSmgnMRsPjoWBLEtIZbE8Jk64kz5&key=AIzaSyAK63kbufR0vKHNM_86o1flCM4maLzM-5A");

        HttpResponse response = null;
        try {
            response = httpclient.execute(httpget);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(response.getStatusLine().getStatusCode()==200){

            try {
                server_response = EntityUtils.toString(response.getEntity());
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            Log.e("Server response", "Failed to get server response" );
        }
        try {
            JSONObject responseTable = new JSONObject(server_response);
            JSONArray rows = responseTable.getJSONArray("rows");//returns 2D array [["mayank", "2", "noida"],["m2", "3", "noida"]]
            Log.e("the menu",rows + "");

            toSearch = mainActivity.toSearchTable;
            Log.e("to search", toSearch);
            for(int i = 0; i < rows.length(); i++)
            {
                JSONArray innerJsonArray = (JSONArray)rows.get(i);
                if(toSearch.equals(innerJsonArray.get(0).toString()))
                {
                    for(int j = 1; j < innerJsonArray.length(); j++)
                    {
                        foodInMess[j-1] = innerJsonArray.get(j).toString();
                    }
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
    @Override
    protected void onPostExecute(String s)
    {
        mainActivity.getMenu(foodInMess);
    }


}



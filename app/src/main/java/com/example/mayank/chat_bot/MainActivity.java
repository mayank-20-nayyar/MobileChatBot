package com.example.mayank.chat_bot;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaRecorder;
import android.net.Uri;
import android.provider.AlarmClock;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    Button call;
    Button alarm;
    Button music;
    Map<String,String> contacts = new HashMap<String,String>();
    String toCall = "";
    String toPlay = "";
    EditText ed;
    EditText playSong;
    


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ed = (EditText)findViewById(R.id.editText);
        playSong = (EditText)findViewById(R.id.editText2);

        call = (Button) findViewById(R.id.button);
        call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toCall = ed.getText().toString();
                searchContact();
            }
        });

        alarm = (Button)findViewById(R.id.button2);
        alarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(AlarmClock.ACTION_SET_ALARM);
                i.putExtra(AlarmClock.EXTRA_HOUR, 16);
                i.putExtra(AlarmClock.EXTRA_MINUTES, 15);
                startActivity(i);
            }
        });

        music = (Button)findViewById(R.id.button3);
        music.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getMusic();
            }
        });
    }

    void getMusic()
    {
        toPlay = playSong.getText().toString();
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
}

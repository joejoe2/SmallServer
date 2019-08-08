package com.example.testclient;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {
    public static final int ACTION_CODE_LOGIN=0,ACTION_CODE_VIEW=1,ACTION_CODE_DOWNLOAD=2,ACTION_CODE_UPLOAD=3,ACTION_CODE_INFO=4;
    public static int entry=5000;
    public static String serverIP="219.91.49.21";
    int timeout=5000;
    NotificationManager notificationManager;

    int notifyId=0;

    ListView listView;
    ArrayAdapter arrayAdapter;
    LinkedList<String> path=new LinkedList<>();

    public String des="root";

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getComponents();
        setListeners();
        verifyStoragePermissions(this);
        getNotifications();
        //arrayAdapter=new ArrayAdapter(this,android.R.layout.simple_selectable_list_item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id=item.getItemId();
        if(id==R.id.login){
            login();
        }else if(id==R.id.view){
            view();
        }else if(id==R.id.upload){
            selectFile();
        }
        return super.onOptionsItemSelected(item);
    }

    private void getComponents(){
        listView=(ListView)findViewById(R.id.listView);
    }

    private  NotificationCompat.Builder getNotifications(){
        if (notificationManager==null)
        notificationManager =(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder notifyBuilder = new NotificationCompat.Builder(this);
        //
        String channelId = "Your_channel_id";
        NotificationChannel channel = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if(notificationManager==null) {
                channel = new NotificationChannel(
                        channelId,
                        "Channel human readable title",
                        NotificationManager.IMPORTANCE_LOW);
                notificationManager.createNotificationChannel(channel);
            }
                notifyBuilder.setChannelId(channelId);
        }else {
            //notifyBuilder.setDefaults(NotificationCompat.DEFAULT_VIBRATE);
            notifyBuilder.setPriority(NotificationCompat.PRIORITY_LOW);
        }
        //
        return notifyBuilder;
    }

    synchronized int getNotifyId(){
        if(notifyId>=1000){
            notifyId=0;
            return notifyId;
        }else {
            return notifyId++;
        }
    }

    public void onBackPressed() {
        des=path.pollLast();
        if(des!=null){
            view();
        }else{
            listView.setAdapter(null);
            des="root";
        }
    }

    public void openFile(String path)
    {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.parse(path);
        intent.setDataAndType(uri, "*/*");
        startActivity(Intent.createChooser(intent, "Open folder"));
    }

    public void selectFile(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent,0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode==0){
            Uri uri = data.getData();
            String uriString = uri.toString();
            File myFile = new File(uriString);
            String path = myFile.getAbsolutePath();
            String displayName = null;
            long size=0;
            if (uriString.startsWith("content://")) {
                Cursor cursor = null;
                try {
                    cursor = getContentResolver().query(uri, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                        size= cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE));
                    }
                } finally {
                    cursor.close();
                }
            } else if (uriString.startsWith("file://")) {
                displayName = myFile.getName();
            }
            System.out.println(displayName);
            System.out.println(size);
            try {
                InputStream inn = getContentResolver().openInputStream(uri);//.openFileDescriptor(uri, "r").getFileDescriptor();

                upload(inn,displayName,size);
            } catch (FileNotFoundException e) {
                e.printStackTrace();

            }
        }
    }


    void upload(InputStream file,String name,Long length){
        //
        NotificationCompat.Builder notifyBuilder=getNotifications();
        notifyBuilder.setContentTitle("upload "+name).setContentText("upload in progress");
        notifyBuilder.setProgress(100,0,false);
        notifyBuilder.setSmallIcon(R.mipmap.ic_launcher);
        //

        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setMessage("upload "+name+" ?");
        builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int notificationID=getNotifyId();
                //upload
                Thread upl=new Thread(() -> {
                        try {
                            Socket socket = new Socket();
                            socket.connect(new InetSocketAddress(serverIP,entry),timeout);

                            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            PrintWriter printer = new PrintWriter(socket.getOutputStream());
                            printer.println("user:test,pas:1234,action:"+ACTION_CODE_UPLOAD+",file:"+name+",");
                            printer.flush();

                            Double size=length/1024/1024.0;
                            System.out.println("upload start  size="+size);

                            BufferedOutputStream buf=new BufferedOutputStream(socket.getOutputStream());;
                            InputStream fin=file;
                            int l=0;
                            byte[] bytes=new byte[4096];
                            long now=0;
                            int pre=0;
                            while((l=fin.read(bytes))!=-1){
                                buf.write(bytes,0, l);
                                now+=l;
                                int progress=(int)(Math.ceil(now/1024/1024.0/size*100));
                                //System.out.println(now+" "+progress);
                                if(pre!=progress){
                                    pre=progress;
                                    //System.out.println("now:"+now/1024/1024.0+"mb  "+progress+"%");
                                    notifyBuilder.setProgress(100,progress,false);
                                    notifyBuilder.setContentText(progress+"%");
                                    notificationManager.notify(notificationID,notifyBuilder.build());
                                }
                            }
                            buf.flush();
                            buf.close();
                            fin.close();
                            reader.close();
                            printer.close();
                            socket.close();
                            Thread.sleep(100);
                            notifyBuilder.setContentTitle("finish upload "+name).setProgress(100,100,false);
                            notificationManager.notify(notificationID,notifyBuilder.build());
                            MainActivity.this.runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this,"upload finish",Toast.LENGTH_SHORT).show();
                            });

                        } catch (SocketTimeoutException e){
                            MainActivity.this.runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this,"can not connect to server",Toast.LENGTH_SHORT).show();
                            });
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
                    upl.start();
                }
        });
        builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });
        // 3. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();

        dialog.show();

    }

    void login(){
        Thread thread=new Thread(()->{
            Socket socket= null;
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(serverIP,entry),timeout);

                PrintWriter printer = new PrintWriter(socket.getOutputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                printer.println("user:test,pas:1234,action:"+ACTION_CODE_LOGIN+",");
                printer.flush();
                printer.close();
                reader.close();
                socket.close();
                System.out.println("login success");
                MainActivity.this.runOnUiThread(() -> {
                    Toast.makeText(this,"login success",Toast.LENGTH_SHORT).show();
                });
            } catch (SocketTimeoutException e){
                MainActivity.this.runOnUiThread(() -> {
                    Toast.makeText(this,"can not connect to server",Toast.LENGTH_SHORT).show();
                });
            }catch (IOException e) {
                e.printStackTrace();
                MainActivity.this.runOnUiThread(() -> {
                    Toast.makeText(this,"network error",Toast.LENGTH_SHORT).show();
                });
            }
        });
        thread.start();
    }

    void view(){
        Thread view=new Thread(() -> {
            try {
                Socket sockets = new Socket();
                sockets.connect(new InetSocketAddress(serverIP,entry),timeout);

                BufferedReader reader = new BufferedReader(new InputStreamReader(sockets.getInputStream()));
                PrintWriter printer = new PrintWriter(sockets.getOutputStream());
                printer.println("user:test,pas:1234,action:"+ACTION_CODE_VIEW+","+"des:"+des+",");
                printer.flush();
                Socket viewer=sockets;
                ObjectInputStream objectInputStream=new ObjectInputStream(viewer.getInputStream());
                //File folder=(File) objectInputStream.readObject();
                String[] folder=(String[]) objectInputStream.readObject();
                objectInputStream.close();
                viewer.close();
                printer.close();
                reader.close();
                //System.out.println(folder.toString());
                for (String f:folder){
                    System.out.println(f);
                }

                MainActivity.this.runOnUiThread(() -> {
                    arrayAdapter=new ArrayAdapter(this,android.R.layout.simple_selectable_list_item,folder);
                    listView.setAdapter(arrayAdapter);
                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            String target=parent.getItemAtPosition(position).toString();
                            Toast.makeText(MainActivity.this,target,Toast.LENGTH_SHORT).show();
                            if(target.startsWith("-d")){
                                path.addLast(des);
                                target=target.substring(target.indexOf("-d ")+3);
                                des=target;
                                view();
                            }else{
                                download(target);
                            }
                        }
                    });
                });

            } catch (SocketTimeoutException e){
                MainActivity.this.runOnUiThread(() -> {
                    Toast.makeText(this,"can not connect to server",Toast.LENGTH_SHORT).show();
                });
            }catch (Exception ex) {
                ex.printStackTrace();
            }

        });
        view.start();
    }

    void download(String target){
        //
        NotificationCompat.Builder notifyBuilder=getNotifications();
        notifyBuilder.setContentTitle("Download "+target).setContentText("Download in progress");
        notifyBuilder.setProgress(100,0,false);
        notifyBuilder.setSmallIcon(R.mipmap.ic_launcher);
        //

        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setMessage("download "+target+" ?");

        builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                int notificationID=getNotifyId();
                Thread dow=new Thread(() -> {
                    try {
                        Socket socket = new Socket();
                        socket.connect(new InetSocketAddress(serverIP,entry),timeout);

                        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        PrintWriter printer = new PrintWriter(socket.getOutputStream());
                        printer.println("user:test,pas:1234,action:"+ACTION_CODE_DOWNLOAD+",file:"+target+",");
                        printer.flush();

                        Double size=Double.parseDouble(reader.readLine());
                        System.out.println("download start  size="+size);

                        BufferedInputStream buf=new BufferedInputStream(socket.getInputStream());
                        String root=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
                        String path=root+"/"+target.substring(target.lastIndexOf("\\")+1);
                        FileOutputStream fout=new FileOutputStream(path);
                        int l=0;
                        byte[] bytes=new byte[4096];
                        long now=0;
                        int pre=0;
                        while((l=buf.read(bytes))!=-1){
                            fout.write(bytes,0, l);
                            now+=l;
                            int progress=(int)(Math.ceil(now/1024/1024.0/size*100));
                            if(pre!=progress){
                                pre=progress;
                                //System.out.println("now:"+now/1024/1024.0+"mb  "+progress+"%");

                                notifyBuilder.setProgress(100,progress,false);
                                notifyBuilder.setContentText(progress+"%");
                                notificationManager.notify(notificationID,notifyBuilder.build());
                            }

                        }

                        buf.close();
                        fout.flush();
                        fout.close();
                        reader.close();
                        printer.close();
                        socket.close();
                        Thread.sleep(100);
                        notifyBuilder.setContentTitle("finish download "+target).setProgress(100,100,false);
                        notificationManager.notify(notificationID,notifyBuilder.build());
                        MainActivity.this.runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this,"download finish",Toast.LENGTH_SHORT).show();
                            openFile(path);
                        });

                    } catch (SocketTimeoutException e){
                        MainActivity.this.runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this,"can not connect to server",Toast.LENGTH_SHORT).show();
                        });
                    }catch (Exception ex) {
                        ex.printStackTrace();
                    }

                });
                dow.start();
            }
        });
        builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });
        // 3. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();

        dialog.show();

    }

    private void setListeners(){
    }
}

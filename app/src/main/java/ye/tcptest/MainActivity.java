package ye.tcptest;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.SharedPreferences;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {

    //For debug
    private final String TAG = "MainActivity";

    //About the ui controls
    private EditText edit_ip = null;
    private EditText edit_port = null;
    private Button btn_connect = null;
    private EditText edit_send = null;
    private Button btn_send = null;
    public TextView wifiView;
    public TextView commView;

    public WifiInfo wifiInfo;
    public WifiManager wifiManager = null;
    private List<ScanResult> wifiscan;
    private List<WifiConfiguration> wificon;
    public String wifilist;
    public String wificonlist;
    public String ssid ;
    private String command;
    public  String wifisend;
    public int index = 0;
    public static String[] ss;

    public String ip;
    public String port;



     int tcp2ui=0;
     int time2ui=2;
     int bt2tcp=4;
     int time2tcp=1;
     int j;

    Handler handler;
    ClientThread clientThread;





    /** Called when the activity is first created.*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edit_ip = (EditText) this.findViewById(R.id.edit_ip);
        edit_port = (EditText) this.findViewById(R.id.edit_port);
        edit_send = (EditText) this.findViewById(R.id.edit_send);
        btn_connect = (Button) this.findViewById(R.id.btn_connect);
        btn_send = (Button) this.findViewById(R.id.btn_send);
        wifiView = (TextView)findViewById(R.id.wifiview);
        wifiView.setMovementMethod(ScrollingMovementMethod.getInstance());

        commView = (TextView)findViewById(R.id.commview);
        commView.setMovementMethod(ScrollingMovementMethod.getInstance());

        ip = edit_ip.getText().toString();
        port = edit_port.getText().toString();
        Log.d(TAG, ip + port);

        clientThread = new ClientThread(handler, ip, port);





        init();

        //Click here to connect
        btn_connect.setOnClickListener(new OnClickListener(){

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub

                new Thread(clientThread).start();
                Log.d(TAG, "clientThread is start!!");
                commView.append("\nclientThread is start!!");
                if(clientThread.isConnect)
                {
                    btn_connect.setText(R.string.btn_disconnect);

                }
            }});

        //Click here to Send Msg to Server
        btn_send.setOnClickListener(new OnClickListener(){

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub

                try
                {
                    Message msg = new Message();
                    msg.what = bt2tcp;
                    msg.obj = edit_send.getText().toString();
                    clientThread.sendHandler.sendMessage(msg);
                    commView.append("\n本机："+edit_send.getText().toString());
                    edit_send.setText("");
                }
                catch (Exception e)
                {
                    Log.d(TAG, e.getMessage());
                    e.printStackTrace();
                }
            }});



    }


    private void init()
    {


        //Load the datas from share preferences
        SharedPreferences sharedata = getSharedPreferences("data", 0);
        String ip = sharedata.getString("ip", "192.168.1.107");
        String port = sharedata.getString("port", "8089");
        edit_ip.setText(ip);
        edit_port.setText(port);


        //获得wifimanager
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        wificon = wifiManager.getConfiguredNetworks();
        wifiInfo = wifiManager.getConnectionInfo();
        wifiManager.startScan();
        wifiscan = wifiManager.getScanResults();



        //使用定时器，每隔2S获得一次信号强度值
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (wifiInfo.getBSSID() != null) {

                    Message msg = new Message();
                    msg.what=time2ui;
                    msg.obj="";
                    handler.sendMessage(msg);


                }


            }

        }, 100, 2000);

        handler = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {

                Message tcpmsg=new Message();
                tcpmsg.what=bt2tcp;
                wifisend= sendinfo(20,wifiscan);
                ss= new String[wifiscan.size()];
                ss=wifisend.split("\n");


                if (msg.what == time2ui)
                {

                    get_wifilist();
                    j++;
                    wifiView.setText(wifilist);

                    //实现每过几秒就自动发送信息的程序
//                    if(clientThread.isConnect) {
//                        commView.append("\ntime：已发送wifilist\n");
//                        tcpmsg.obj = wifisend;
//                        clientThread.sendHandler.sendMessage(tcpmsg);
//                    }

                    //实现每过几秒就自动逐条发送信息的程序---有问题实现不了--状态非法

                    if(clientThread.isConnect) {



                        for (int i = 0; i < ss.length; i++) {

                            commView.append("\n本机：\n" + ss[i]);
                            Message msglist=new Message();
                            msglist.what=i+5;
                            msglist.obj = ss[i];
                            clientThread.sendHandler.sendMessage(msglist);                        }
                    }

                }



                if(msg.what == tcp2ui)
                {
                    command=msg.obj.toString();
                    commView.append( "\n服务器："+command);

                    if (command.equals("q") ) {
                        wifisend= sendinfo(20,wifiscan);

                            //逐条发送

//                         for(int i=0;i<ss.length;i++){
//
//                             commView.append("\n本机：\n"+ss[i]);
//                             tcpmsg.obj=wifisend;
//                             clientThread.sendHandler.sendMessage(tcpmsg);
//
//                         }

                        commView.append( "\n本机：\n"+wifisend);
                        tcpmsg.obj=wifisend;
                        clientThread.sendHandler.sendMessage(tcpmsg);

                    }
                     else{
                        commView.append(" \n转换AP:"+command);
                        //切换AP的程序
                        index = wanted_index(command);
                        disConnectionWifi(wifiInfo.getNetworkId());
                        connectionConfiguration(index);
                        wifiInfo=wifiManager.getConnectionInfo();
                        if (wifiInfo.getBSSID() != null) {
                        commView.append(" \n已切换为："+wifiInfo.getSSID());}
//                        tcpmsg.obj ="";
//                        clientThread.sendHandler.sendMessage(tcpmsg);

                     }


                }



            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public boolean onDestory(){
        return true;

    }


    public void connectionConfiguration(int netId){
        //连接配置好指定ID的网络
        wifiManager.enableNetwork(wificon.get(netId).networkId,true);

    }

    public void disConnectionWifi(int netId){

        wifiManager.disableNetwork(netId);
        wifiManager.disconnect();

    }

    public int wanted_index (String ssid){
        for(int i=0; i<wificon.size();i++){
            if(wificon.get(i).SSID.equals(ssid)) {
                return i;
            }
        }
        return -1;
    }




    public  String get_wifilist(){
        //wifi名称
        ssid = wifiInfo.getSSID();
        //wifi速度
        int speed = wifiInfo.getLinkSpeed();
        //wifi速度单位
        String units = WifiInfo.LINK_SPEED_UNITS;

        wifilist = j+"\n";
        wificonlist="\n\nThese network are configurated:\n";
        for (int i = 0; i < wifiscan.size(); i++) {

            wifilist += wifiscan.get(i).SSID + "  " + (double)
                    wifiscan.get(i).frequency / 1000 + "GHz   " + wifiscan.get(i).level + "dBm" + "\n";
        }

        for (int i = 0; i < wificon.size(); i++) {

            wificonlist += "id:"+wificon.get(i).networkId+"    "+wificon.get(i).SSID+"\n";

        }


        String text =  "We are connecting to " + ssid + "at "+ String.valueOf(speed) +" " + String.valueOf(units) ;
        wifilist += "\n  ";
        wifilist += text;
        wifilist +=wificonlist;

        return wifilist;
    }

    public  String sendinfo(int maxwifiinfo,List<ScanResult> results){
        String wifi="";
        int looptimes = 10;
        if(maxwifiinfo > results.size()){
            looptimes = results.size();
        }
        else {
            looptimes = maxwifiinfo;
        }
        for (int i = 0; i < looptimes ; i++) {

            while(results.get(i).SSID.length()<20){
                results.get(i).SSID = results.get(i).SSID+" ";
            }
            wifi += "ssid: "+results.get(i).SSID+"  ch: "+results.get(i).frequency+"  rssi: "+results.get(i).level+"\n";
        }
        return  wifi;
    }



}



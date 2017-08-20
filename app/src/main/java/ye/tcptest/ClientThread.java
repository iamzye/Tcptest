package ye.tcptest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.EditText;

public class ClientThread implements Runnable
{
    //For debug
    private final String TAG = "ClientThread";

    private Socket socket;
    private String ip;
    private int port;
    private Handler receiveHandler;
    public Handler sendHandler;
    BufferedReader bufferedReader;
    private InputStream inputStream;
    private OutputStream outputStream;
    public boolean isConnect = false;



    int tcp2ui=0;
    int bt2tcp=4;
    int time2tcp=1;

    public ClientThread(Handler handler, String ip, String port) {
        // TODO Auto-generated constructor stub
        this.receiveHandler = handler;
        this.ip = ip;
        this.port = Integer.valueOf(port);
        Log.d(TAG, "ClientThread's construct is OK!!");
    }


    public void run()
    {
        try
        {
            Log.d(TAG, "Into the run()");
            socket = new Socket(ip, port);
            isConnect = socket.isConnected();
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            //To monitor if receive Msg from Server
            new Thread()
            {
                @Override
                public void run()
                {

                    byte[] buffer;
                    int readSize;
                    try
                    {
                        while(socket.isConnected())
                        {
                             buffer = new byte[1024];
                             readSize = inputStream.read(buffer);
                            Log.d(TAG, "readSize:" + readSize);

                            //If Server is stopping
                            if(readSize == -1)
                            {
                                inputStream.close();
                                outputStream.close();
                            }
                            if(readSize == 0)continue;

                            Message msg = new Message();
                            msg.what = tcp2ui;
                            msg.obj = new String(buffer,0,readSize);
                            receiveHandler.sendMessage(msg);



                        }
                    }
                    catch(IOException e)
                    {
                        Log.d(TAG, e.getMessage());
                        e.printStackTrace();
                    }
                }

            }.start();

            //To Send Msg to Server
            Looper.prepare();
            sendHandler = new Handler()
            {
                @Override
                public void handleMessage(Message msg)
                {
                    if (msg.what == bt2tcp ||msg.what ==time2tcp)
                    {
                        try
                        {
                            outputStream.write((msg.obj.toString() + "\r\n").getBytes());
                            outputStream.flush();
                        }
                        catch (Exception e)
                        {
                            Log.d(TAG, e.getMessage());
                            e.printStackTrace();
                        }
                    }


                }
            };
            Looper.loop();

        } catch (SocketTimeoutException e)
        {
            // TODO Auto-generated catch block
            Log.d(TAG, e.getMessage());
            e.printStackTrace();
        }catch (UnknownHostException e)
        {
            // TODO Auto-generated catch block
            Log.d(TAG, e.getMessage());
            e.printStackTrace();
        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            Log.d(TAG, e.getMessage());
            e.printStackTrace();
        }
    }
}

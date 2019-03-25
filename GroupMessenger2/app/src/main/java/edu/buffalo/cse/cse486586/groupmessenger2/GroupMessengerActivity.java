package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String MAIN_TAG = GroupMessengerActivity.class.getSimpleName();
    static final String mKEY = "key";
    static final String mVALUE = "value";
    static final int SERVER_PORT = 10000;
    static final String authority = "edu.buffalo.cse.cse486586.groupmessenger2.provider";
    private Uri mUri;
    ContentValues mValues;
    ContentResolver mResolver;
    int counter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        //From OnPTest...
        mUri = buildUri();

        mValues = new ContentValues();
        mResolver = getContentResolver();

        //Taken as is from PA1
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        //Taken as is from PA1
        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
            Log.e(MAIN_TAG, "Can't create a ServerSocket");
            e.printStackTrace();
            return;
        }

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        final EditText enterMessage = (EditText)findViewById(R.id.editText1);
        Button mSend = (Button)findViewById(R.id.button4);
        mSend.setOnClickListener(new View.OnClickListener(){
            /*
            https://developer.android.com/reference/android/widget/Button
             */
            @Override
            public void onClick(View v){
                //Taken as is from PA1
                Log.d(MAIN_TAG, "Inside Button onClick");
                String msg = enterMessage.getText().toString() + "\n";
                enterMessage.setText(""); // This is one way to reset the input box.

                /*
                 * Note that the following AsyncTask uses AsyncTask.SERIAL_EXECUTOR, not
                 * AsyncTask.THREAD_POOL_EXECUTOR as the above ServerTask does. To understand
                 * the difference, please take a look at
                 * http://developer.android.com/reference/android/os/AsyncTask.html
                 */
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });
    }

    //From POnTest.. class
    private Uri buildUri() {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(GroupMessengerActivity.authority);
        uriBuilder.scheme("content");
        return uriBuilder.build();
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        @Override
        protected Void doInBackground(ServerSocket... sockets) {

            ServerSocket server = sockets[0];
            BufferedReader mMessageIn;
            PrintWriter mMessageOut;
            String mMessage;
            String mTestMessage = "Test";

            //From PA1
            try{
                while (true){
                    Socket clientSocket = server.accept();
                    mMessageIn = new BufferedReader(new InputStreamReader(clientSocket.
                            getInputStream()));
                    mMessageOut = new PrintWriter(clientSocket.getOutputStream(), true);
                    mMessageOut.println(mTestMessage);
                    if ((mMessage = mMessageIn.readLine()) != null) {
                        publishProgress(mMessage);
                        Log.d(MAIN_TAG, mMessage);
                        Log.d(MAIN_TAG, "Test message sent to Client");
                    }
                }
            }catch (IOException e){
                Log.e(MAIN_TAG, "Error with ServerTask. Method: doInBackground");
            }
            return null;
        }

        protected void onProgressUpdate(String... strings) {
            /*
             * The following code displays what is received in doInBackground().
             * Taken as is from PA1
             */
            String strReceived = strings[0].trim();
            TextView mTextView = (TextView) findViewById(R.id.textView1);
            mTextView.append(strReceived + "\t");
            mTextView.append("\n");

            String sequence_counter = Integer.toString(counter);
            mValues.put(mKEY, sequence_counter);
            mValues.put(mVALUE, strReceived);
            mResolver.insert(mUri, mValues);
            //Log.d(MAIN_TAG, Integer.toString(counter));
            counter+=1;
            //Log.d(MAIN_TAG, Integer.toString(counter));
            //Log.d(MAIN_TAG, sequence_counter);
            //Log.d(MAIN_TAG, strReceived);
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void>{
        @Override
        protected Void doInBackground(String... msgs) {
            try {
                int[] Ports = new int[] {11108, 11112, 11116, 11120, 11124};
                for (int Port : Ports) {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Port);

                    String msgToSend = msgs[0];
                    BufferedReader mMessageIn;
                    PrintWriter mMessageOut;
                    String mMessage;
                    String mTestMessage = "Test";

                    //From PA1
                    mMessageIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    mMessageOut = new PrintWriter(socket.getOutputStream(), true);
                    mMessageOut.println(msgToSend);

                    while (socket.isClosed() == false) {
                        try {
                            if ((mMessage = mMessageIn.readLine()) != null) {
                    /* Taken as is from
                    https://docs.oracle.com/javase/tutorial/networking/sockets/readingWriting.html
                     */
                                if (mMessage.equals(mTestMessage)) {
                                    Log.d(MAIN_TAG, "Test message received from Client");
                                    socket.close();
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }catch (UnknownHostException e) {
                Log.e(MAIN_TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(MAIN_TAG, "ClientTask socket IOException");
            } catch (Exception e){
                Log.d(MAIN_TAG, "Error in ClientTask. Method: doInBackground");
            }
            return null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

}
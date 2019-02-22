package edu.buffalo.cse.cse486586.groupmessenger1;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 *
 * Please read:
 *
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 *
 * before you start to get yourself familiarized with ContentProvider.
 *
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 *
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {

    private static final String PROVIDER_TAG = GroupMessengerProvider.class.getSimpleName();
    private static final String mKEY = "key";
    private static final String mVALUE = "value";
    static int MAX_LENGTH = 128;
    //Context context = getContext();

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues mValues) {
        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         *
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */

        String mName;
        String mFileString;
        FileOutputStream mFile;

        mName = mValues.getAsString(mKEY);
        mFileString = mValues.getAsString(mVALUE);

        //Modified from PA1 file storage snippet
        try {
/*https://developer.android.com/reference/android/content/Context.html#openFileOutput(java.lang.String,%20int)
* Alternatively, can pass current activity's context using ContentValues.put() from main activity
*/
            //mFile = context.openFileOutput(mName, Context.MODE_PRIVATE);
            //openFileOutput will create a new file if doesn't exist. Else, rewrites
            mFile = getContext().openFileOutput(mName, Context.MODE_PRIVATE);
            mFile.write(mFileString.getBytes());
            mFile.flush(); //empty method declaration
            mFile.close();

        } catch (IOException e) {
            Log.d(PROVIDER_TAG, "IO Exception: insert method");
        } catch (NullPointerException e){
            Log.d(PROVIDER_TAG, "Null Pointer Exception: insert method");
        } catch (Exception e){
            e.printStackTrace();
            Log.d(PROVIDER_TAG, "File write failed");
        }
        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        //Context context = this.getContext();
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         *
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */

        FileInputStream mFile;
        String mString;
        StringBuffer mStringBuffer;
        BufferedReader mReaderIn;
        String mFileString;

        //int mBuffer;
        //byte[] mReadBuffer = new byte[MAX_LENGTH];
        //128 is max bytes that can be read. Not ideal but works for grader

        MatrixCursor mMatrixCursor = new MatrixCursor(new String[] {mKEY, mVALUE});
        try{ //(FileInputStream mFile = getContext().openFileInput(selection)){

            //mFileName = context.openFileInput(selection);
            /*
            Getting the context for current activity and using it with FileInput, as in line above
            leads to Exception. Why?
             */

//https://developer.android.com/reference/android/content/Context.html#openFileInput(java.lang.String)
            mFile = getContext().openFileInput(selection);

            //https://developer.android.com/reference/kotlin/java/io/FileInputStream#read%28%29
            //mBuffer = mFile.read(mReadBuffer, 0, MAX_LENGTH);
            //Log.d(PROVIDER_TAG, Integer.toString(mBuffer));
            //mFileString = new String(mReadBuffer, 0, mBuffer);//, "UTF-8");
            ////mFileString = new String(mReadBuffer);//, "UTF-8");
            //Log.d(PROVIDER_TAG + "ReadBuffer: ", mReadBuffer.toString());
            //Log.d(PROVIDER_TAG + "File: ", mFileString);

            mStringBuffer = new StringBuffer(MAX_LENGTH);
            //StringBuffer is thread safe, as compared to StringBuilder

            //Same idea as PA1 reading from socket input stream
            mReaderIn = new BufferedReader(new InputStreamReader(mFile));
            while ((mString = mReaderIn.readLine()) != null){
//https://docs.oracle.com/javase/7/docs/api/java/lang/StringBuffer.html#append(java.lang.String)
                mStringBuffer.append(mString);
            }
            //https://docs.oracle.com/javase/7/docs/api/java/lang/StringBuffer.html#substring(int)
            mFileString = mStringBuffer.substring(0);

            //Matrix Cursor addRow() not safe for concurrent use
            synchronized(this) {
                mMatrixCursor.addRow(new String[]{selection, mFileString});
            }
            mFile.close();
            /*Not needed in try-with-resources statement.
            * Supported after Target and Source Compatibility 1.8
            */
        } catch (FileNotFoundException e){
          Log.d(PROVIDER_TAG, "FileNotFound: query method");
        } catch (Exception e){
            e.printStackTrace();
            Log.d(PROVIDER_TAG, "Exception: query method");
        }
        return mMatrixCursor;
    }
}
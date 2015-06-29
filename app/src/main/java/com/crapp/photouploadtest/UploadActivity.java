package com.crapp.photouploadtest;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;


public class UploadActivity extends Activity {

    Uri imageURI;

    TextView path,message;
    ImageView image;
    Button uploadBtn;

    ProgressDialog pDialog;
    String responseText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        path = (TextView) findViewById(R.id.file_uri);
        message = (TextView) findViewById(R.id.message);
        image = (ImageView) findViewById(R.id.image_upload);
        uploadBtn = (Button) findViewById(R.id.upload_button);

        Intent i = getIntent();
        imageURI = i.getData();

        // Bitmap factory
        BitmapFactory.Options options = new BitmapFactory.Options();
        // downsizing image as it throws OutOfMemory Exception for larger images
        options.inSampleSize = 8;
        Bitmap bitmap = BitmapFactory.decodeFile(imageURI.getPath(),options);
        path.setText(imageURI.getPath());
        image.setImageBitmap(bitmap);

        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new uploadFile().execute();
            }
        });
    }

    private class uploadFile extends AsyncTask<Void,Void,Void>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(UploadActivity.this);
            pDialog.setMessage("Uploading Image");
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            int day, month, year;
            int second, minute, hour;
            GregorianCalendar date = new GregorianCalendar();

            day = date.get(Calendar.DAY_OF_MONTH);
            month = date.get(Calendar.MONTH);
            year = date.get(Calendar.YEAR);

            second = date.get(Calendar.SECOND);
            minute = date.get(Calendar.MINUTE);
            hour = date.get(Calendar.HOUR);

            String name=(hour+""+minute+""+second+""+day+""+(month+1)+""+year);
            String tag=name+".jpg";
            String fileName = imageURI.toString().replace(imageURI.toString(), tag);

            HttpURLConnection httpURLConnection = null;
            DataOutputStream outputStream = null;
            InputStream inputStream = null;
            int serverResponseCode;

            String lineEnd = "rn";
            String twoHyphens = "--";
            String boundary =  "*****";
            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1024*1024;
            String urlString = "http://quesdesk.hostzi.com/file_upload.php";
            try
            {
                //------------------ CLIENT REQUEST
                FileInputStream fileInputStream = new FileInputStream(new File(imageURI.getPath()));
                // open a URL connection to the Servlet
                URL url = new URL(urlString);
                // Open a HTTP connection to the URL
                httpURLConnection = (HttpURLConnection) url.openConnection();
                // Allow Inputs
                httpURLConnection.setDoInput(true);
                // Allow Outputs
                httpURLConnection.setDoOutput(true);
                // Don't use a cached copy.
                httpURLConnection.setUseCaches(false);
                // Use a post method.
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
                httpURLConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);
                outputStream = new DataOutputStream( httpURLConnection.getOutputStream() );
                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                outputStream.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\"" + fileName + "\"" + lineEnd);
                outputStream.writeBytes(lineEnd);
                // create a buffer of maximum size
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];
                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                while (bytesRead > 0)
                {
                    outputStream.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }
                // send multipart form data necessary after file data...
                outputStream.writeBytes(lineEnd);
                outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                // close streams
                Log.e("Debug", "File is written");
                fileInputStream.close();
                outputStream.flush();
                outputStream.close();
            }catch (MalformedURLException ex)
            {
                Log.e("Debug", "error: " + ex.getMessage(), ex);
                Toast.makeText(getApplicationContext(), "MalformedURLException", Toast.LENGTH_SHORT).show();
                return null;
            }
            catch (IOException ioe)
            {
                Log.e("Debug", "error: " + ioe.getMessage(), ioe);
                Toast.makeText(getApplicationContext(), "OutputException", Toast.LENGTH_SHORT).show();
                return null;
            }
            //------------------ read the SERVER RESPONSE
            try {
                serverResponseCode = httpURLConnection.getResponseCode();
                String serverResponseMessage = httpURLConnection.getResponseMessage();
                inputStream = httpURLConnection.getInputStream();
                byte data[] = new byte[1024];
                int counter = -1;
                while( (counter = inputStream.read(data)) != -1){
                    responseText += new String(data, 0, counter);
                }
                Log.i("uploadFile", "HTTP Response is : " + serverResponseMessage + ": " + serverResponseCode);
                if (serverResponseCode == 200){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            path.setText("File Upload Complete");
                            Toast.makeText(getApplicationContext(),"File Upload Complate",Toast.LENGTH_SHORT).show();
                            message.setText(responseText);
                        }
                    });
                }
                inputStream.close();
                return null;
            }
            catch (IOException ioex){
                Log.e("Debug", "error: " + ioex.getMessage(), ioex);
                Toast.makeText(getApplicationContext(), "InputException", Toast.LENGTH_SHORT).show();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            image.setVisibility(View.GONE);
            uploadBtn.setVisibility(View.GONE);
            pDialog.dismiss();
        }
    }


}

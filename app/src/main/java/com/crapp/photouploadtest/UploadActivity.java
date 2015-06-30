package com.crapp.photouploadtest;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;




public class UploadActivity extends Activity {

    Uri imageURI;

    TextView path, message;
    ImageView image;
    Button uploadBtn;

    ProgressDialog pDialog;

    String uploadServerUri;

    String text;
    int serverResponseCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        path = (TextView) findViewById(R.id.file_uri);
        message = (TextView) findViewById(R.id.message);
        image = (ImageView) findViewById(R.id.image_upload);
        uploadBtn = (Button) findViewById(R.id.upload_button);

        uploadServerUri = "http://quesdesk.hostzi.com/file_upload.php";


        Intent i = getIntent();
        imageURI = i.getData();

        // Bitmap factory
        //BitmapFactory.Options options = new BitmapFactory.Options();
        // downsizing image as it throws OutOfMemory Exception for larger images
        //options.inSampleSize = 8;
        Bitmap bitmap = BitmapFactory.decodeFile(imageURI.getPath());
        path.setText(imageURI.getPath());
        image.setImageBitmap(bitmap);

        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                pDialog = ProgressDialog.show(UploadActivity.this, "File Upload", "Uploading File...", true);
                image.setVisibility(View.GONE);
                uploadBtn.setVisibility(View.GONE);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        uploadFile(imageURI.getPath());
                    }
                }).start();

            }
        });
    }

    public int uploadFile(String sourceFileUri) {
        String fileName = sourceFileUri;

        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        InputStream is = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File sourceFile = new File(sourceFileUri);
        if (!sourceFile.isFile()) {

            pDialog.dismiss();

            Log.e("uploadFile", "Source File not exist :" + imageURI.getPath());

            runOnUiThread(new Runnable() {
                public void run() {
                    message.setText("Source File not exist :" + imageURI.getPath());
                }
            });
            return 0;
        } else {
            try {// open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(uploadServerUri);

                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("image", fileName);

                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"image\";filename=\"" + fileName + "\"" + lineEnd);
                dos.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {

                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                }

                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();

                Log.i("uploadFile", "HTTP Response is : "
                        + serverResponseMessage + ": " + serverResponseCode);
                Log.i("uploadFile", "HTTP Response is : "
                        + serverResponseMessage + ": " + serverResponseCode);

                is = new BufferedInputStream(conn.getInputStream());
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String inputLine = "";
                StringBuilder sb = new StringBuilder();
                while ((inputLine = br.readLine()) != null) {
                    sb.append(inputLine);
                }

                text = sb.toString();

                if (serverResponseCode == 200) {

                    runOnUiThread(new Runnable() {
                        public void run() {

                            message.setText(text);
                            Toast.makeText(UploadActivity.this, "File Upload Complete.",Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                //close the streams //
                fileInputStream.close();
                dos.flush();
                dos.close();
                is.close();

            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            } catch (MalformedURLException e2) {
                e2.printStackTrace();
                Log.e("uploadFile", "Incorrect Server Address :" + uploadServerUri);
                runOnUiThread(new Runnable() {
                    public void run() {
                        message.setText("Incorrect Server Address :" + uploadServerUri);
                    }
                });
            } catch (IOException e3) {
                e3.printStackTrace();
                Log.e("uploadFile", "IOException :" + uploadServerUri);
                runOnUiThread(new Runnable() {
                    public void run() {
                        message.setText("IOException :" + uploadServerUri);
                    }
                });
            }

            pDialog.dismiss();
            return serverResponseCode;
        }
    }


}

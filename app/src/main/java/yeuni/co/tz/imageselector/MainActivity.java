package yeuni.co.tz.imageselector;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.webkit.PermissionRequest;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import yeuni.co.tz.imageselector.Utils.FileCompressor;

public class MainActivity extends AppCompatActivity {

    ImageView imageplaceholderr;

    static final int REQUEST_TAKE_PHOTO = 510;
    static final int REQUEST_GALLERY_PHOTO = 167;
    String uploaded_img_path =null;
    Uri selected_uri = null;
    File mPhotoFile;
    String theIdVendor;
    FileCompressor mCompressor;
    private static final int REQUEST_PERMISSION_CODE = 28;
    private static final int REQUEST_CODE = 2384;
    Uri selectedImage = null;
    ImageView img_browser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCompressor = new FileCompressor(this);

        img_browser=findViewById(R.id.img_browser);

        img_browser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                selectImage();




            }
        });
    }


    private void selectImage() {
        final CharSequence[] items = {"Choose from Gallery","Take Photo",
                "Cancel"};
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals("Take Photo")) {
                    MainActivity.this.requestStoragePermission(true);
                } else if (items[item].equals("Choose from Gallery")) {
                    MainActivity.this.requestStoragePermission(false);
                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    /**
     * Requesting multiple permissions (storage and camera) at once
     * This uses multiple permission model from dexter
     * On permanent denial opens settings dialog
     */
    private void requestStoragePermission(final boolean isCamera) {
        Dexter.withActivity(this).withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        // check if all permissions are granted
                        if (report.areAllPermissionsGranted()) {
                            if (isCamera) {
                                dispatchTakePictureIntent();
                            } else {
                                dispatchGalleryIntent();
                            }
                        }
                        // check for permanent denial of any permission
                        if (report.isAnyPermissionPermanentlyDenied()) {
                            // show alert dialog navigating to Settings
                            showSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<com.karumi.dexter.listener.PermissionRequest> list, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();
                    }





//                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
//                        token.continuePermissionRequest();
//                    }
                }).withErrorListener(new PermissionRequestErrorListener() {
            @Override
            public void onError(DexterError error) {
                Toast.makeText(MainActivity.this, "Error occurred! ", Toast.LENGTH_SHORT).show();
            }
        })
                .onSameThread()
                .check();
    }


    /**
     * Showing Alert Dialog with Settings option
     * Navigates user to app settings
     * NOTE: Keep proper title and message depending on your app
     */
    private void showSettingsDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Need Permissions");
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.");
        builder.setPositiveButton("GOTO SETTINGS", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                MainActivity.this.openSettings();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();

    }

    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", this.getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 101);
    }


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(this.getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
                // Error occurred while creating the File
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        BuildConfig.APPLICATION_ID + ".provider",
                        photoFile);

                mPhotoFile = photoFile;
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);

            }
        }
    }


    /**
     * Select image fro gallery
     */
    private void dispatchGalleryIntent() {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickPhoto.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(pickPhoto, REQUEST_GALLERY_PHOTO);
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String mFileName = "JPEG_" + timeStamp + "_";
        File storageDir = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File mFile = File.createTempFile(mFileName, ".jpg", storageDir);
        long length = mFile.length();
        Log.d("karataa", "yeunicreateImageFileMFile: " + mFile);
        Log.d("shjhaa", "length: " + length);
        return mFile;
    }

    /**
     * Get real file path from URI
     *
     * @param contentUri
     * @return
     */
    public String getRealPathFromUri(Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = this.getContentResolver().query(contentUri, proj, null, null, null);
            assert cursor != null;
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();

            Log.d("karataa", "getRealPathFromUri: " + cursor.getString(column_index));


            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {

            if (requestCode == REQUEST_TAKE_PHOTO) {
                //if user select take photo from camera

                try {
                    File file = new File(String.valueOf(mPhotoFile));
                    long length = file.length();
                    length = length / 1024;
                    // System.out.println("File Path : " + file.getPath() + ", File size : " + length +" KB");

                    Log.d("tuuuls", "KABLAJUU File Path : " + file.getPath() + ", File size : " + length + " KB");

                    mPhotoFile = mCompressor.compressToFile(mPhotoFile);

                    Log.d("wuooo1", "mPhotoFileTakePhoto: " + mPhotoFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Glide.with(this).load(mPhotoFile).apply(new RequestOptions().
                        centerCrop().placeholder(R.drawable.imageplaceholderr)).
                        into(img_browser);
                Log.d("wuooo1", "glideee: " + mPhotoFile);

                ///>>>> CALL METHOD TO UPLOAD IMAGE FILE TO SERVER
              //  uploadFileToServer(String.valueOf(mPhotoFile));


                File file = new File(String.valueOf(mPhotoFile));
                long length = file.length();
                length = length / 1024;
                // System.out.println("File Path : " + file.getPath() + ", File size : " + length +" KB");

                Log.e("tuuuls", "BAADAFile Path : " + file.getPath() + ", File size : " + length + " KB");

            } else if (requestCode == REQUEST_GALLERY_PHOTO) {
                selectedImage = data.getData();
                Log.d("karataa", "selectedImageURI: " + selectedImage);

                try {

                    File file = new File(String.valueOf(new File(getRealPathFromUri(selectedImage))));
                    long length = file.length();
                    length = length / 1024;
                    // System.out.println("File Path : " + file.getPath() + ", File size : " + length +" KB");

                    Log.e("nakupemdaa", "KABLAAFile Path : " + file.getPath() + ", File size : " + length + " KB");


                    mPhotoFile = mCompressor.compressToFile(new File(getRealPathFromUri(selectedImage)));

                    Log.d("nakupemdaa", "selectedImageURIMPhotoCompressor: " + mPhotoFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                File file = new File(String.valueOf(mPhotoFile));
                long length = file.length();
                length = length / 1024;
                // System.out.println("File Path : " + file.getPath() + ", File size : " + length +" KB");

                Log.d("nakupemdaa", "BAADAFile Path : " + file.getPath() + ", File size : " + length + " KB");
                ///>>>> CALL METHOD TO UPLOAD IMAGE FILE TO SERVER
               // uploadFileToServer(String.valueOf(mPhotoFile));

                Glide.with(this).
                        load(mPhotoFile).apply(new RequestOptions().centerCrop().
                        placeholder(R.drawable.imageplaceholderr)).into(img_browser);

            }


            //end

        }
    }

}
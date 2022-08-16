package com.example.jomisowaterlevel;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jomisowaterlevel.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private Button captureBtn, cameraBtn, galaryBtn, detectBtn, submitBtn;
    private ImageView captureIV;
    private TextView resultTV;
    private Bitmap bitmap;
    private EditText mWaterLevel;
    private final int CAMERA_REQ_CODE=100;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private ActivityMainBinding binding;
    private Uri filePath=null;
    FirebaseStorage storage;
    StorageReference storageReference;
    DatabaseReference databaseReference;
    private final int IMG_REQUEST_ID = 1;
    private ProgressDialog progressDialog;
    private FirebaseAuth fAuth;
    private FirebaseFirestore db;
    ActivityResultLauncher<Intent> activityResultLauncher;
    FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //storageReference= FirebaseStorage.getInstance().getReference();
        databaseReference = FirebaseDatabase.getInstance().getReference().child("User_image");

        db = FirebaseFirestore.getInstance();
        fAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference =storage.getReference();
        captureBtn = findViewById(R.id.idBtnLoggedOut);
        captureIV = findViewById(R.id.idIVCaptureImage);
        resultTV = findViewById(R.id.idTVDetectText);
        cameraBtn = findViewById(R.id.idBtnCamera);
        galaryBtn = findViewById(R.id.idBtnChooseImg);
        detectBtn = findViewById(R.id.idBtnDetect);
        submitBtn = findViewById(R.id.submitBtn);
        mWaterLevel = findViewById(R.id.manualWaterValue);
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_IMAGE_CAPTURE);
        }

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getData() != null && result.getData().getExtras() != null
                    && result.getData().getExtras().get("data") != null) {
                bitmap = (Bitmap) result.getData().getExtras().get("data");
                resultTV.setText("Click on Detect Water Level to see the text in the image");
                captureIV.setImageBitmap(bitmap);

            }
        });

        cameraBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            activityResultLauncher.launch(intent);

        });


        galaryBtn.setOnClickListener(v -> mGetContext.launch("image/*"));


        detectBtn.setOnClickListener(v -> detectText());

        captureBtn.setOnClickListener(view -> {
            fAuth.signOut();
            Intent i = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(i);
            finish();
            Toast.makeText(MainActivity.this,"Logged Out Successfully",Toast.LENGTH_SHORT).show();
        });
        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                String waterLevel = mWaterLevel.getText().toString();
                if (TextUtils.isEmpty(waterLevel)) {
                    mWaterLevel.setError("Enter Water Level");
                    return;
                }

                Map<String,String > user=new HashMap<>();
                user.put("waterlevel","waterLevel");
                db.collection("WL")
                        .add(user)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                //Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                                Toast.makeText(MainActivity.this, "Water Level Stored", Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                //Log.w(TAG, "Error adding document", e);
                                Toast.makeText(MainActivity.this, "Error" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                uploadImage();
                Intent intent = new Intent();
                intent.setType("image/*");
                startActivity(intent);
                finish();
            }


        });

    }
    ActivityResultLauncher<String> mGetContext = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>(){
                @Override
                public void onActivityResult(Uri result) {
                    if (result!=null){
                        filePath=result;
                        captureIV.setImageURI(result);

                    }
                }
            }
    );

    private void uploadImage() {

        if (filePath != null) {
            final  ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading...");
            progressDialog.show();
            // Defining the child of storageReference
            StorageReference ref = storageReference.child("image/"+ UUID.randomUUID().toString());

            ref.putFile(filePath).addOnSuccessListener(
                    new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(
                                UploadTask.TaskSnapshot taskSnapshot)
                        {
                            ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    databaseReference.push().setValue(uri.toString());

                                    Toast.makeText(MainActivity.this,"Image Uploaded!!", Toast.LENGTH_SHORT).show();
                                    progressDialog.dismiss();

                                }
                            });


                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e)
                { // Error, Image not uploaded
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this,"Failed " + e.getMessage(),Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void detectText() {

        TextRecognizer recognizer = new TextRecognizer.Builder(this).build();
        Bitmap bitmap=((BitmapDrawable) captureIV.getDrawable()).getBitmap();

        Frame frame = new Frame.Builder().setBitmap(bitmap).build();

        SparseArray<TextBlock> textBlockSparseArray = recognizer.detect(frame);
        StringBuilder stringBuilder = new StringBuilder();
/*        int number=0;
        int minimum=999;*/

        for (int i = 0; i < textBlockSparseArray.size(); i++) {
            TextBlock textBlock = textBlockSparseArray.valueAt(i);
            stringBuilder.append(textBlock.getValue());
            stringBuilder.append("\n");

        }
        //  String myNumbers = stringBuilder.toString();
        // int i=Integer.parseInt(stringBuilder.toString());

        //   String[] stringInteger = myNumbers.split("\n");
       /* int number = 0;
        for(int i = 0; i<stringInteger.length; i++)
        {
            number= (Integer.parseInt(stringInteger[i]));
            int minimum = 0;
            if(number<minimum)
            {
                minimum=number;
            }
        }*/
        resultTV.setText(stringBuilder.toString());


        // resultTV.setText("Water Level is:"+number);
        //Toast.makeText(MainActivity.this,"Minimum Number is : "+number, Toast.LENGTH_SHORT).show();

    }
}
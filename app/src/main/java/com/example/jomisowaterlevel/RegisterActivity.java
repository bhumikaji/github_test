package com.example.jomisowaterlevel;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    public static final String TAG= "TAG";
    private EditText mFirstName, mEmail, mPassword, mPhone,mLastName,mConfirmPassword;
    private String agency;
    private Button mRegisterBtn;
    private TextView mLoginBtn;
    private ProgressBar progressBar;
    private Spinner spinner;
    private ArrayAdapter<CharSequence> stateAdapter;
    private FirebaseFirestore fstore;
    private String userID;
    private FirebaseAuth fAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        mFirstName = findViewById(R.id.firstName);
        mEmail = findViewById(R.id.email);
        mPassword = findViewById(R.id.password);
        mPhone = findViewById(R.id.phone);
        mRegisterBtn = findViewById(R.id.registerBtn);
        mLoginBtn = findViewById(R.id.createText1);
        progressBar=findViewById(R.id.progressBar);
        mLastName= findViewById(R.id.lastName);
        mConfirmPassword = findViewById(R.id.confirmPassword);
        spinner =findViewById(R.id.spinner1);
        stateAdapter=ArrayAdapter.createFromResource(this,R.array.array_agency,R.layout.list_item);
        stateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(stateAdapter);

        fAuth =FirebaseAuth.getInstance();
        fstore= FirebaseFirestore.getInstance();
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                agency= adapterView.getItemAtPosition(i).toString();
                Toast.makeText(RegisterActivity.this,agency,Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(),LoginActivity.class ));
            }
        });


        mRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String firstName= mFirstName.getText().toString().trim();
                String  lastName= mLastName.getText().toString().trim();
                String email= mEmail.getText().toString().trim();

                String password=mPassword.getText().toString().trim();
                String confirmedPassword=mConfirmPassword.getText().toString().trim();
                if(password!=null && confirmedPassword!=null ){
                    if(!confirmedPassword.equals(password)){
                        mPassword.setError("Password Does not match");
                        return;

                    }else{
                        progressBar.setTag("Creating New Account");
                        progressBar.showContextMenu();
                    }
                }
                agency=String.valueOf(spinner.getSelectedItem());

                String phone= mPhone.getText().toString().trim();

                if(TextUtils.isEmpty(firstName)){
                    mFirstName.setError("First Name is Required");
                    return;
                }
                if(TextUtils.isEmpty(lastName)){
                    mLastName.setError("Last Name is Required");
                    return;
                }
                /*if(TextUtils.isEmpty(agency)){
                    Toast.makeText(Register.this,"The List Is Empty",Toast.LENGTH_SHORT).show();
                    return;
                }*/
                if (TextUtils.isEmpty(email)){
                    mEmail .setError("Email is Required");
                    return;
                }
                if(TextUtils.isEmpty(password)){
                    mPassword.setError("Password is Required");
                    return;
                }

                if (password.length()<6){
                    mPassword.setError("Password Must Be >=6 ");
                    return;
                }
                if (TextUtils.isEmpty(confirmedPassword)){
                    mConfirmPassword.setError("Confirmed Password is Required");
                    return;
                }
                if (TextUtils.isEmpty(phone)){
                    mPhone.setError("Phone no. is Required");
                    return;
                }
            /*if(TextUtils.isEmpty(agency)){
                agency.setError("Select Your Agency");
                return;

            }*/
                progressBar.setVisibility(View.VISIBLE);

                fAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            FirebaseUser fuser= fAuth.getCurrentUser();
                            fuser.sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    Toast.makeText(getApplicationContext(),"Register Successful", Toast.LENGTH_SHORT).show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.d(TAG,"OnFailure: Email not Sent"+e.getMessage());
                                }
                            });
                            Toast.makeText(getApplicationContext(),"User Created" ,Toast.LENGTH_SHORT).show();
                            userID= fAuth.getCurrentUser().getUid();
                            DocumentReference documentReference=fstore.collection("user").document(userID);
                            Map<String,Object> user=new HashMap<>();
                            user.put("firstname",firstName);
                            user.put("lastname",lastName);
                            user.put("agency",agency);
                            user.put("email",email);
                            user.put("phone",phone);
                            user.put("password",password);
                            fstore.collection("users")
                                    .add(user)
                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                        @Override
                                        public void onSuccess(DocumentReference documentReference) {
                                            // Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                                            Toast.makeText(RegisterActivity.this,"Data Stored Successfully",Toast.LENGTH_SHORT).show();
                                        /*Intent intent= new Intent(Register.this,Login.class);
                                        StartActivity(intent);
                                        finish();*/
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Log.w(TAG, "Error adding document", e);
                                            Toast.makeText(RegisterActivity.this,"Error"+e.getMessage(),Toast.LENGTH_SHORT).show();
                                        }
                                    });


                        /*documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Log.d(TAG,"onsucess: user profile is created for"+ userID);

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d(TAG,"onFailure:"+e.toString());


                            }
                        });*/
                            startActivity(new Intent(getApplicationContext(),LoginActivity.class));

                        }
                        else {
                            Toast.makeText(RegisterActivity.this,"Error"+task.getException().getMessage(),Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);

                        }
                    }
                });
            }


        });




    }
}
package com.example.arunlakhera.loginregistration;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.w3c.dom.Text;

public class ProfileActivity extends AppCompatActivity {

    Bundle userBundle;
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        userBundle = getIntent().getExtras();
        user = FirebaseAuth.getInstance().getCurrentUser();

        String userEmail = userBundle.getString("userEmail","User Email");
        String decryptedString = decryption("Input Encrypted String");

        Toast.makeText(ProfileActivity.this, decryptedString, Toast.LENGTH_SHORT).show();
        TextView userEmailTextView = findViewById(R.id.textViewEmail);

        userEmailTextView.setText(userEmail);
    }

    /**
     * 3. Function to Show logout user
     * */

    public void logout(View view) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(ProfileActivity.this, MainActivity.class));
            finish();
    }

    public String decryption(String strEncryptedText){
        String seedValue = "YourSecKey";
        String strDecryptedText="";
        try {
            strDecryptedText = AESHelper.decrypt(seedValue, strEncryptedText);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return strDecryptedText;
    }

}

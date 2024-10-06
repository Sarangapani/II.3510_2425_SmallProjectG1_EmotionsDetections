package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText usernameEditText, emailEditText, passwordEditText;
    private Button registerButton;
    DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialiser la base de données
        db = new DatabaseHelper(this);

        // Initialiser les composants
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        registerButton = findViewById(R.id.registerButton);

        // Gérer le clic du bouton d'inscription
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Récupérer les données saisies
                String username = usernameEditText.getText().toString().trim();
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                // Vérifier si les champs ne sont pas vides
                if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                } else {
                    // Ajouter l'utilisateur à la base de données
                    boolean isInserted = db.insertUser(username, email, password);
                    if (isInserted) {
                        Toast.makeText(RegisterActivity.this, "Utilisateur enregistré avec succès", Toast.LENGTH_SHORT).show();
                        // Optionnel : rediriger l'utilisateur vers l'écran de connexion après inscription
                        finish(); // Termine l'activité et retourne à l'écran précédent
                    } else {
                        Toast.makeText(RegisterActivity.this, "Échec de l'enregistrement", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }
}

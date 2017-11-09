package guitartutorandanalyser.guitartutor;


import android.content.DialogInterface;
import android.content.Intent;
import android.icu.text.DateFormat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;



public class GuitarTutorMain extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guitar_tutor_main);

        // on first run valami db is created on the system for songs and lessons, method checks first if database already exists
        try {
            DatabaseHelper dbh = new DatabaseHelper(this);
            dbh.createDataBase();



            java.util.Date d = new java.util.Date();


            try {
                Log.d("datum",String.valueOf(d.getYear()+1900) +"."+String.valueOf(d.getMonth())+"."+String.valueOf(d.getDay())+".");
            }catch (Exception e){ Log.d("datum","error");}

            /*Log.d("c sound id", String.valueOf( this.getResources().getIdentifier("song_chromatic_scale_a_90", "raw", this.getPackageName())));
            Log.d("c tab id", String.valueOf( this.getResources().getIdentifier("tab_chromatic_scale_a_90bpm", "raw", this.getPackageName())));
            Log.d("s sound id", String.valueOf( this.getResources().getIdentifier("song_starwars_theme_102", "raw", this.getPackageName())));
            Log.d("s tab id", String.valueOf( this.getResources().getIdentifier("tab_star_wars", "raw", this.getPackageName())));*/





          //  dbh.UPDATE_DB_toDelete(); // delete this line

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onButtonSongsClick(View v) {
        startActivity(new Intent("guitartutorandanalyser.guitartutor.Songs"));
    }

    public void onButtonLessonsClick(View v) {
        startActivity(new Intent("guitartutorandanalyser.guitartutor.Lessons"));
    }

    public void onButtonRecordsClick(View v) {
        startActivity(new Intent("guitartutorandanalyser.guitartutor.BestScores"));
    }

    public void onButtonHelpClick(View v) {
        startActivity(new Intent("guitartutorandanalyser.guitartutor.UserGuide"));
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("Kilép az alkalmazásból?")
                .setCancelable(false)
                .setPositiveButton("Igen", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        GuitarTutorMain.this.finish();
                    }
                })
                .setNegativeButton("Mégse", null)
                .show();
    }

}

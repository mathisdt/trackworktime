package org.zephyrsoft.trackworktime;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.pmw.tinylog.Logger;
import org.threeten.bp.ZoneId;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.databinding.ActivityDebugBinding;

public class DebugActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityDebugBinding binding = ActivityDebugBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.timezone.setText(ZoneId.systemDefault().toString());

        binding.startUpgrade.setOnClickListener(v -> {
            Logger.debug("showing upgrade activity");
            Intent i = new Intent(this, UpgradeActivity.class);
            startActivity(i);
        });

        binding.resetCache.setOnClickListener(v -> {
            Logger.debug("Deleting cache...");

            Basics basics = Basics.getOrCreateInstance(getApplicationContext());
            DAO dao = basics.getDao();

            dao.deleteCacheFrom(null);
            dao.close();

            Toast.makeText(this, "Cache deleted...", Toast.LENGTH_LONG).show();
        });
    }
}

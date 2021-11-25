/*
 * This file is part of TrackWorkTime (TWT).
 *
 * TWT is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License 3.0 as published by
 * the Free Software Foundation.
 *
 * TWT is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License 3.0 for more details.
 *
 * You should have received a copy of the GNU General Public License 3.0
 * along with TWT. If not, see <http://www.gnu.org/licenses/>.
 */
package org.zephyrsoft.trackworktime;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
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

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                throw new IllegalArgumentException("options menu: unknown item selected");
        }
    }
}

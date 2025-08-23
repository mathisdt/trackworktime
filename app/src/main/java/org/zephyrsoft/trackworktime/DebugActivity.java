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
import androidx.appcompat.app.AppCompatActivity;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.databinding.ActivityDebugBinding;
import org.zephyrsoft.trackworktime.util.ThemeUtil;

import java.time.ZoneId;

public class DebugActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityDebugBinding binding = ActivityDebugBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ThemeUtil.styleActionBar(this, getSupportActionBar());

        binding.timezone.setText(ZoneId.systemDefault().toString());

        binding.startUpgrade.setOnClickListener(v -> {
            Logger.debug("showing upgrade activity");
            Intent i = new Intent(this, UpgradeActivity.class);
            startActivity(i);
        });

        binding.resetCache.setOnClickListener(v -> {
            Logger.debug("Deleting cache...");

            Basics basics = Basics.get(this);
            DAO dao = basics.getDao();

            dao.deleteCacheFrom(null);
            dao.close();

            Toast.makeText(this, getString(R.string.cacheDeleted), Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        throw new IllegalArgumentException("options menu: unknown item selected");
    }
}

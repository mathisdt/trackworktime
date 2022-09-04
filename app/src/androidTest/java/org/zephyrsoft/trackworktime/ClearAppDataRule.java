package org.zephyrsoft.trackworktime;

import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.zephyrsoft.trackworktime.database.MySQLiteHelper;

/**
 * Remove any events and targets from the database.
 */
public class ClearAppDataRule implements TestRule {

  @NonNull
  @Override
  public final Statement apply(@NonNull final Statement base, @NonNull Description description) {
    return new ClearAppDataStatement(base);
  }

  private static class ClearAppDataStatement extends Statement {

    private final Statement base;

    public ClearAppDataStatement(Statement base) {
      this.base = base;
    }

    @Override
    public void evaluate() throws Throwable {
      MySQLiteHelper dbHelper = new MySQLiteHelper(InstrumentationRegistry.getInstrumentation().getTargetContext());
      SQLiteDatabase db = dbHelper.getWritableDatabase();
      db.delete(MySQLiteHelper.EVENT, null, null);
      db.delete(MySQLiteHelper.CACHE, null, null);
      db.delete(MySQLiteHelper.TARGET, null, null);
      db.close();

      base.evaluate();
    }
  }
}
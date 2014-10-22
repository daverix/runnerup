package org.runnerup.view;

import android.database.sqlite.SQLiteDatabase;

public interface DatabaseProvider {
    SQLiteDatabase getDatabase();
}

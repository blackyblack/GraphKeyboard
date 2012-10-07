package com.sam.suggestion;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class UserDictionaryDataSource 
{
	// Database fields
	private SQLiteDatabase database;
	private String tableName;
	private String locale;
	
	private String[] allColumns = { DictionarySQLiteOpenHelper.COLUMN_ID,
			DictionarySQLiteOpenHelper.COLUMN_LOCALE,
			DictionarySQLiteOpenHelper.COLUMN_STRING, DictionarySQLiteOpenHelper.COLUMN_FREQ};
	
	public UserDictionaryDataSource(SQLiteDatabase database, String tableName, String locale) { 
		this.database = database;
		this.tableName = tableName;
		this.locale = locale;
	}
	
	public void createUserString(String theString, long freq) {
		ContentValues values = new ContentValues();
		values.put(DictionarySQLiteOpenHelper.COLUMN_LOCALE, locale);
		values.put(DictionarySQLiteOpenHelper.COLUMN_STRING, theString);
		values.put(DictionarySQLiteOpenHelper.COLUMN_FREQ, freq);
		
		database.insert(tableName, null, values);
	}
	
	//find string record by name
	public UserStringsTableRow lookupString(String theString)
	{
		Cursor cursor = database.query(tableName,
				allColumns, DictionarySQLiteOpenHelper.COLUMN_STRING + " = ? AND " +
						DictionarySQLiteOpenHelper.COLUMN_LOCALE + " = ? ", 
				new String[]{theString.toLowerCase(), locale}, null, null, 
				Integer.toString(1));
		
		cursor.moveToFirst();
		
		if (cursor.isAfterLast())
		{
			cursor.close();
			return null;
		}
		
		UserStringsTableRow myString = cursorToString(cursor);
		
		cursor.close();
		return myString;
	}
	
	//update record by given id
	public void updateString(long id, UserStringsTableRow record)
	{
		ContentValues values = new ContentValues();
		values.put(DictionarySQLiteOpenHelper.COLUMN_LOCALE, record.locale);
		values.put(DictionarySQLiteOpenHelper.COLUMN_STRING, record.theString);
		values.put(DictionarySQLiteOpenHelper.COLUMN_FREQ, record.freq);
		
		database.update(tableName, values, 
				DictionarySQLiteOpenHelper.COLUMN_ID + " = ? ", 
				new String[]{Long.toString(id)});
	}
	
	//sort strings by frequency and get 'limit' elements
	public List<UserStringsTableRow> getStringsByFreq(int limit)
	{
		List<UserStringsTableRow> strings = new ArrayList<UserStringsTableRow>(limit);
		Cursor cursor = database.query(tableName,
				allColumns, DictionarySQLiteOpenHelper.COLUMN_LOCALE + " = ? ", 
				new String[]{locale}, null, null, 
				DictionarySQLiteOpenHelper.COLUMN_FREQ + " DESC", 
				Integer.toString(limit));
		
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			UserStringsTableRow theString = cursorToString(cursor);
			strings.add(theString);
			cursor.moveToNext();
		}
		
		// Make sure to close the cursor
		cursor.close();
		return strings;
	}
	
	//sort strings by frequency and get 'limit' elements
	public List<UserStringsTableRow> getStringsByFreqAndPrefix(int limit, String prefix)
	{		
		List<UserStringsTableRow> strings = new ArrayList<UserStringsTableRow>(limit);
		Cursor cursor = database.query(tableName,
				allColumns, DictionarySQLiteOpenHelper.COLUMN_STRING + " LIKE ? AND " +
						DictionarySQLiteOpenHelper.COLUMN_LOCALE + " = ? ", 
				new String[]{prefix.toLowerCase() + "%", locale}, null, null, 
				DictionarySQLiteOpenHelper.COLUMN_FREQ + " DESC", 
				Integer.toString(limit));
			
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			UserStringsTableRow theString = cursorToString(cursor);
			strings.add(theString);
			cursor.moveToNext();
		}
			
		// Make sure to close the cursor
		cursor.close();
		return strings;
	}

	private UserStringsTableRow cursorToString(Cursor cursor) {
		UserStringsTableRow theString = new UserStringsTableRow();
		theString.id = cursor.getLong(0);
		theString.locale = cursor.getString(1);
		theString.theString = cursor.getString(2).toLowerCase();
		theString.freq = cursor.getLong(3);
		
		return theString;
	}
}

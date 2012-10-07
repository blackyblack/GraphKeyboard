package com.sam.suggestion;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DictionaryDataSource 
{
	// Database fields
	private SQLiteDatabase database;
	private String tableName;
	private String[] allColumns = { DictionarySQLiteOpenHelper.COLUMN_ID,
			DictionarySQLiteOpenHelper.COLUMN_STRING, DictionarySQLiteOpenHelper.COLUMN_FREQ };
	
	public DictionaryDataSource(SQLiteDatabase database, String tableName) { 
		this.database = database;
		this.tableName = tableName;
	}
	
	//find string record by name
	public StringsTableRow lookupString(String theString)
	{
		Cursor cursor = database.query(tableName,
				allColumns, DictionarySQLiteOpenHelper.COLUMN_STRING + " = ? ", 
				new String[]{theString.toLowerCase()}, null, null, 
				Integer.toString(1));
		
		cursor.moveToFirst();
		
		if (cursor.isAfterLast())
		{
			cursor.close();
			return null;
		}
		
		StringsTableRow myString = cursorToString(cursor);
		
		cursor.close();
		return myString;
	}
	
	//update record by given id
	public void updateString(long id, StringsTableRow record)
	{
		ContentValues values = new ContentValues();
		values.put(DictionarySQLiteOpenHelper.COLUMN_STRING, record.theString);
		values.put(DictionarySQLiteOpenHelper.COLUMN_FREQ, record.freq);
		
		database.update(tableName, values, 
				DictionarySQLiteOpenHelper.COLUMN_ID + " = ? ", 
				new String[]{Long.toString(id)});
	}
	
	//sort strings by frequency and get 'limit' elements
	public List<StringsTableRow> getStringsByFreq(int limit)
	{
		List<StringsTableRow> strings = new ArrayList<StringsTableRow>(limit);
		Cursor cursor = database.query(tableName,
				allColumns, null, null, null, null, DictionarySQLiteOpenHelper.COLUMN_FREQ + " DESC", 
				Integer.toString(limit));
		
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			StringsTableRow theString = cursorToString(cursor);
			strings.add(theString);
			cursor.moveToNext();
		}
		
		// Make sure to close the cursor
		cursor.close();
		return strings;
	}
	
	//sort strings by frequency and get 'limit' elements
	public List<StringsTableRow> getStringsByFreqAndPrefix(int limit, String prefix)
	{		
		List<StringsTableRow> strings = new ArrayList<StringsTableRow>(limit);
		Cursor cursor = database.query(tableName,
				allColumns, DictionarySQLiteOpenHelper.COLUMN_STRING + " LIKE ? ", 
				new String[]{prefix.toLowerCase() + "%"}, null, null, DictionarySQLiteOpenHelper.COLUMN_FREQ + " DESC", 
				Integer.toString(limit));
			
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			StringsTableRow theString = cursorToString(cursor);
			strings.add(theString);
			cursor.moveToNext();
		}
			
		// Make sure to close the cursor
		cursor.close();
		return strings;
	}

	private StringsTableRow cursorToString(Cursor cursor) {
		StringsTableRow theString = new StringsTableRow();
		theString.id = cursor.getLong(0);
		theString.theString = cursor.getString(1).toLowerCase();
		theString.freq = cursor.getLong(2);
		
		return theString;
	}
}

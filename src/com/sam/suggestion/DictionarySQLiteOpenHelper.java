package com.sam.suggestion;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DictionarySQLiteOpenHelper extends SQLiteOpenHelper {

	public static final String TABLE_ENGLISH_STRINGS = "en_strings";
	public static final String TABLE_RUSSIAN_STRINGS = "ru_strings";
	public static final String TABLE_USER_STRINGS = "user_strings";
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_STRING = "WORD";
	public static final String COLUMN_FREQ = "FREQ";
	public static final String COLUMN_LOCALE = "LOCALE";

	private static final String DATABASE_PATH = "/data/data/com.sam.graphkeyboard/databases/";
	private static final String DATABASE_NAME_RAW = "dict.png";
	private static final String DATABASE_NAME = "dict.db";
	private static final int DATABASE_VERSION = 16;
	
	private SQLiteDatabase myDataBase;
	private final Context myContext;

	public DictionarySQLiteOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.myContext = context;
	}
	
	public synchronized void createDataBase() throws IOException
	{
    	if(checkDataBase())
    		return;
 
    	//By calling this method and empty database will be created into the default system path
        //of your application so we are gonna be able to overwrite that database with our database.
        SQLiteDatabase m = this.getReadableDatabase();
        
        if(m != null)
    	{
    		m.close();
    	}
 
        try 
        {
        	copyDataBase();
        } 
        catch (IOException e) 
        {
        	throw new Error("Error copying database");
    	}
    }
	
	private boolean checkDataBase()
	{	 
    	SQLiteDatabase checkDB = null;
 
    	try
    	{
    		String myPath = DATABASE_PATH + DATABASE_NAME;
    		checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
    	}
    	catch(SQLiteException e)
    	{
    	}
 
    	if(checkDB != null)
    	{
    		if(checkDB.getVersion() != DATABASE_VERSION)
    		{
    			Log.w("suggest", "upgrading database");
    			checkDB.close();
    			return false;
    		}
    		
    		checkDB.close();
    		return true;
    	}
 
    	return false;
    }
	
	 /**
     * Copies your database from your local assets-folder to the just created empty database in the
     * system folder, from where it can be accessed and handled.
     * This is done by transfering bytestream.
     * */
    private void copyDataBase() throws IOException
    {
    	//Open your local db as the input stream
    	InputStream myInput = myContext.getAssets().open(DATABASE_NAME_RAW);
 
    	// Path to the just created empty db
    	String outFileName = DATABASE_PATH + DATABASE_NAME;
 
    	//Open the empty db as the output stream
    	OutputStream myOutput = new FileOutputStream(outFileName);
 
    	//transfer bytes from the inputfile to the outputfile
    	byte[] buffer = new byte[1024];
    	int length;
    	while ((length = myInput.read(buffer))>0){
    		myOutput.write(buffer, 0, length);
    	}
 
    	//Close the streams
    	myOutput.flush();
    	myOutput.close();
    	myInput.close();
    }
	
	public synchronized void openDataBase() throws SQLException{
    	//Open the database
        String myPath = DATABASE_PATH + DATABASE_NAME;
    	myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE);
    	myDataBase.setVersion(DATABASE_VERSION);
    }
	
	@Override
	public synchronized void close() {
		if(myDataBase != null)
			myDataBase.close();
 
		super.close();
	}

	//abstract method override
	@Override
	public void onCreate(SQLiteDatabase database) {
	}

	//abstract method override
	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		
	}
}

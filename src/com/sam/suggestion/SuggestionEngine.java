package com.sam.suggestion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/*
 * This class looks for frequency dictionaries depending on android locale.
 * Has 2 dictionaries: english (default) and russian.
 */
public class SuggestionEngine 
{
	private Context appContext;
	private String locale;
	private String inputString = "";
	public static final String frequentCharsEn = "etaoinshrdlcumwfgypbvkjxqz";
	public static final String frequentCharsRu = "оеаинтсрвлкмдпу€ыьгзбчйхжшюцщэфъЄ";
	private DictionaryDataSource datasourceMain;
	private UserDictionaryDataSource datasourceUser;
	private DictionarySQLiteOpenHelper helper;
	private SQLiteDatabase database;
	public static final int NUM_SUGGESTED_CHARS = 10;
	
	public SuggestionEngine(Context context)
	{
		appContext = context;
		locale = "en_US";
		helper = new DictionarySQLiteOpenHelper(context);
	}
	
	//open with fixed locale (do not check for device language settings)
	public synchronized void open(String localeString)
	{
		//SQLiteDatabase database;
		
		try 
		{
			helper.createDataBase();
		} 
		catch (IOException ioe) 
		{
			throw new Error("Unable to create database");
		}

		try 
		{ 
			helper.openDataBase();
	 
	 	}
		catch(SQLException sqle)
		{
	 		throw sqle;
	 	}
		
		//open database
		database = helper.getWritableDatabase();
		
		Log.v("suggest", "database opened. version = " + database.getVersion());
		
		if(localeString.contentEquals("ru_RU"))
		{			
			locale = "ru_RU";
			datasourceMain = new DictionaryDataSource(database, DictionarySQLiteOpenHelper.TABLE_RUSSIAN_STRINGS);
		}
		else
		{
			//unknown locales set to default
			locale = "en_US";
			datasourceMain = new DictionaryDataSource(database, DictionarySQLiteOpenHelper.TABLE_ENGLISH_STRINGS);
		}
		datasourceUser = new UserDictionaryDataSource(database, DictionarySQLiteOpenHelper.TABLE_USER_STRINGS, locale);
	}
	
	//open with device locale settings
	public synchronized void open()
	{		
		String testLocale = appContext.getResources().getConfiguration().locale.toString();
		
		Log.v("suggest", "locale = " + testLocale);
		
		if(testLocale.contentEquals("ru_RU") || testLocale.contentEquals("ru") || testLocale.contentEquals("_RU"))
		{			
			testLocale = "ru_RU";
		}
		else
		{
			//unknown locales set to default
			testLocale = "en_US";
		}
		
		open(testLocale);
	}
	
	public synchronized void close()
	{
		helper.close();
	}
	
	//add user statistics.
	//we are incrementing frequency of the word on each usage
	public void addStringToUser(String word)
	{
		if(!database.isOpen())
			return;
		
		UserStringsTableRow myString = datasourceUser.lookupString(word);
		
		//insert new record if lookup found null
		if(myString == null)
		{
			datasourceUser.createUserString(word.toLowerCase(), 1);
			return;
		}
		
		//increment usage statistics
		myString.freq++;
		
		datasourceUser.updateString(myString.id, myString);
	}
	
	public void addStringToDB(String word, long freq, UserDictionaryDataSource source)
	{
		if(!database.isOpen())
			return;
		
		source.createUserString(word, freq);
	}
	
	//provide input text for suggestion engine. 
	//position is usually the position of last symbol in text
	public void input(String text, int position)
	{
		inputString = text.substring(0, position);
	}
	
	//suggest words for a given inputString
	public List<String> outputStrings(int limit)
	{	
		if(!database.isOpen())
		{
			return new ArrayList<String>();
		}
		
		List<String> dataMain = lookInDictionary(inputString, limit);
		
		return dataMain;
	}
	
	//search into 'source' for 'limit' words with 'prefix' as prefix.
	private List<String> stringsWithPrefix(String prefix, DictionaryDataSource source, int limit)
	{		
		List<StringsTableRow> data = source.getStringsByFreqAndPrefix(limit, prefix);
		List<String> result = new ArrayList<String>(limit);
		
		for (StringsTableRow elem : data) 
		{
			result.add(elem.theString);
		}
			
		return result;
	}
	
	//search into 'source' for 'limit' words with 'prefix' as prefix.
	///HACK: it is easier to have to 2 identical functions (see stringsWithPrefix) instead
	///      of common interface for UserDictionaryDataSource and DictionaryDataSource
	private List<String> userStringsWithPrefix(String prefix, UserDictionaryDataSource source, int limit)
	{
		List<UserStringsTableRow> data = source.getStringsByFreqAndPrefix(limit, prefix);
		List<String> result = new ArrayList<String>(limit);
			
		for (UserStringsTableRow elem : data) 
		{
			result.add(elem.theString);
		}
				
		return result;
	}
	
	//returns all char for selected language sorted by frequency
	public String getCurrentLanguageFrequentChars()
	{
		if(locale.contentEquals("ru_RU"))
		{
			return frequentCharsRu;
		}
		
		return frequentCharsEn;
	}
	
	//load from 'words' unique chars into 'loaded' string. 'char_pos' is the position of char to check 
	//return result string of suggested chars
	public String suggestedChars(List<String> words, String loaded, int char_pos)
	{		
		//look for first char of input string into frequency dict
		for(String elem : words) 
		{			
			if(elem.length() <= char_pos)
				continue;
			
			char sym = elem.charAt(char_pos);
			
			if(loaded.contains("" + sym))
			{
				continue;
			}
			
			loaded += sym;
		}
		
		return loaded;
	}
	
	// append 'limit' chars from frequentCharsEn to input string
	///TODO: возможно стоит выкинуть этот метод и добавление высокочастотных букв в клавиатуру
	public String appendFrequentChars(String input, int limit)
	{		
		String frequentChars = frequentCharsEn;
		
		if(locale.contentEquals("ru_RU"))
		{
			frequentChars = frequentCharsRu;
		}
		
		for(int i = 0; i < frequentChars.length(); i++)
		{
			if(input.length() >= limit)
				break;
			
			char sym = frequentChars.charAt(i);
			
			if(input.contains("" + sym))
			{
				continue;
			}
			
			input = input + sym;
		}
		
		return input;
	}
	
	// search into specialized dicts
	// return all matching strings with words from user dictionary first
	private List<String> lookInDictionary(String input, int limit)
	{
		List<String> dataUser;
		List<String> dataMain;
		
		//we look into user dictionary first
		dataUser = userStringsWithPrefix(input, datasourceUser, limit);
		
		if(dataUser.size() >= limit)
		{
			return dataUser;
		}
		
		//if we have not found enough unique chars - look into main dictionary
		
		dataMain = stringsWithPrefix(input, datasourceMain, limit - dataUser.size());
		
		//erase all repeated words
		for (String a : dataUser) 
		{
			if(dataMain.contains(a))
			{
				dataMain.remove(a);
			}
		}
		
		dataUser.addAll(dataMain);
			
		return dataUser;
	}
}

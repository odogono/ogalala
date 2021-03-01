// $Id: GameDate.java,v 1.5 1999/04/16 16:52:49 alex Exp $
// This class represents a specific instant in time
// Alexander Veenendaal, 8th April 1999
// Copyright (C) HotGen Studios <http://www.hotgen.com/>



package com.ogalala.nile;

import com.ogalala.mua.*;
import java.util.*;


//===================================================================================
/**
*	The GameDate class encapsulates information about a single moment in time,
*	and allows stuff.
*
*	
*	In the creation of dates using wildcard loaded strings, if the new time works out
*	to be exactly the same as the current time, a single minute is added.
*
*	In the resolution of dates and days, the date will always override the day unless
*	only the date has been specified as a wildcard.
*/
//===================================================================================
public class GameDate
{
	
	private static final long serialVersionUID = 1;
	
	//this is the time we begin from
	//Fri Jan 01 00:00:00 GMT 1932
	
	// is this year a leap year ?
	private boolean 			leapYear = false;
	
	//	The time fields into which the seconds are converted
	//	set the initial time to 12.00am on friday 1st Janurary, 1932
	private int					fields[] = {0,1,0,0,0,0,0,0,0,1932,0};

	
	//	The rate at which game time runs compared to real time
	public static final int TIME_RATE = 6;
	
	/** Time field constants
	*/
	//{ -------- format of date string
	// ie april
	public static final int		MONTH = 0;
	
	// ie 23rd day of april
	// it is NOT 0 based, but 1 based.
	public static final int		DAY_OF_MONTH = 1;
	
	// ie 3rd day of week. 0 based
	public static final int		DAY_OF_WEEK = 2;
	
	// ie 13th hour of day (24 hour clock). 0 based
	public static final int		HOUR = 3;
	
	// ie 31st minute of the hour
	public static final int		MINUTE = 4;
	
		//} -------- end of date string format
	
	// ie 45th second of the minute
	public static final int		SECOND = 5;
	
	// ie 24th week of 1999
	public static final int		WEEK_OF_YEAR = 6;
	
	// ie 2nd week of april
	public static final int		WEEK_OF_MONTH = 7;
	
	// ie 345th day of 1999
	public static final int		DAY_OF_YEAR = 8;
	
	// ie 1999
	public static final int		YEAR = 9;
	
	//Regardless of the fact that it is always 1932,
	// the server may have in fact been effectively running
	// for more than one year. Because we scale the number
	// of seconds to within a year when we process a date,
	// we need to store the number of server years so that
	// when it comes back to converting the time, we have a
	// useable figure
	public static final int		SERVER_YEARS = 10;
	
	
	/**number of date fields
	*/
	public static final int		FIELD_COUNT = 11;
	
	
	private static final String hemicycle[] = { "am", "pm" };
	
	//day constants
	public static final int		FRIDAY = 0;
	public static final int		SATURDAY = 1;
	public static final int		SUNDAY = 2;
	public static final int		MONDAY = 3;
	public static final int		TUESDAY = 4;
	public static final int		WEDNESDAY = 5;
	public static final int		THURSDAY = 6;
	
	private static final String dayString[] = { "Friday", "Saturday", "Sunday", 
												"Monday", "Tuesday", "Wednesday", "Thursday" };
												
	//month constants
	public static final int 	JANUARY = 0;
    public static final int 	FEBRUARY = 1;
    public static final int 	MARCH = 2;
    public static final int 	APRIL = 3;
    public static final int 	MAY = 4;
    public static final int 	JUNE = 5;
    public static final int 	JULY = 6;
    public static final int 	AUGUST = 7;
    public static final int 	SEPTEMBER = 8;
    public static final int 	OCTOBER = 9;
    public static final int 	NOVEMBER = 10;
    public static final int 	DECEMBER = 11;
    
    private static final String monthString[] = { "January", "February", "March", "April", "May",
    												"June", "July", "August", "September", "October",
    													"November", "December" };
    
    //the number of days in the year
    private static final int	DAYS_IN_YEAR = 365;
    
    // second constants
    private static final int	ONE_MINUTE = 60;
    private static final int	ONE_HOUR   = 60 * ONE_MINUTE;
    private static final int	ONE_DAY    = 24 * ONE_HOUR;
    private static final int	ONE_WEEK   = 7 * ONE_DAY;
    private static final int	ONE_YEAR   = DAYS_IN_YEAR * ONE_DAY;
    
    //months for a normal year
    private static int MONTH_LENGTH[];
    
    //months for a normal year
    private static int NORMAL_MONTH_LENGTH[] = {31,28,31,30,31,30,31,31,30,31,30,31};
    
    //months for a leap year
    private static final int LEAP_MONTH_LENGTH[] = {31,29,31,30,31,30,31,31,30,31,30,31};
    
    //wildcard character for use in String dates
    public static final char WILDCARD_CHAR = '*';
    public static final int WILDCARD_NO = -1;
    
    //indicators for comparing two times
    public static final boolean PRESENT = false;
    public static final boolean POST = true;
    
    
    
    //=================================================================================//
    //---------------------------------- Constructors ---------------------------------//
    /**
    *	Default constructor;
    */
    public GameDate()
    {
    	setLeapYear(true);
    }
    
    /**
    *	Constructs a gamedate using integers for the four major fields
    */
    public GameDate(int month, int date, int hour, int minute)
    {
    	this();
    	setMonth( month );
    	setDayOfMonth( date );
    	setHour( hour );
    	setMinute( minute );
    }
    
    
    /**
    *	Constructs a gamedate from the seconds past the beginning of the year
    *	The seconds are scaled up according to the time rate constant defined
    *	in this class.
    */
    public GameDate(long seconds)
    {
    	this();
    	
    	//scale up the number of seconds
    	seconds *= TIME_RATE;
    	
    	//round up the number of seconds so it will always be within a year
        while( seconds >= ONE_YEAR )
        {
        	seconds -= ONE_YEAR;
        	fields[SERVER_YEARS]+=1;
    	}
    	setFieldsFromSeconds( (int)seconds );
    }

	
	/**
    *	Constructs a gamedate using a string in the form:
    *
    *		<month>-<date>-<day>-<hour>-<minute>
    *
    *	Any of the fields within the string may contain a wildcard
    *	character, in which case the date will be set to the next
    *	available time that satisfies all the other fields.
    */
	public GameDate(String dateString, GameDate currentDate)
	{
		//duplicate the passed gameDate object into this object
		setLeapYear( currentDate.getLeapYear() );
		//System.arraycopy(gameDate.fields, 0, this.fields, 0, FIELD_COUNT);
		
		System.out.println( "current Date = " + currentDate);
    	System.out.println( currentDate.toFieldString() );
    	
		//parse the dateString
		//load the date string into a vector of values (Integers)
    	loadDateString( dateString );

    	//set the Server years on the new date, so that when it comes to convert
    	// into seconds the number will be sensible
    	this.setField( SERVER_YEARS, currentDate.getField(SERVER_YEARS) );
    	
    	//make sure all the fields are within their ranges
    	this.adjustDate();
    	
    	//resolve wildcards using a reference game date
    	this.resolveWildcards(currentDate);
    	
    	System.out.println( "satisfied Date = " + this);
    	System.out.println( this.toFieldString() );
	}
    
    //=================================================================================//
    //--------------------------------- Public Methods --------------------------------//
    
    /**
    *	Returns the number of seconds in the date.
    *	The seconds are converted back to real time before being returned.
    *
    *	@return				the number of seconds from the date to the beginning of 
    *						real (server) time
    */
    public final long getSeconds()
    {
    	return (
    		(ONE_YEAR*fields[SERVER_YEARS]) +
    		(ONE_DAY * getDayOfYear( fields[MONTH], fields[DAY_OF_MONTH]) ) +
    		(ONE_HOUR*fields[HOUR]) +
    		(ONE_MINUTE*fields[MINUTE]) 
    		) 
    		/ TIME_RATE;
    }
    
    /**
    *	Checks the five primary date fields for correctness.
    */
    protected final void adjustDate()
    {
    	//check the month is ok, as long as it isn't set as a wildcard
    	if( fields[MONTH] != WILDCARD_NO )
    		if( fields[MONTH] < 0 ) 
    			fields[MONTH] = 0;
    		else if( fields[MONTH] > 11 ) 
    			fields[MONTH] = 11;
    	
    	//check the day of month is ok, as long as it isn't a wildcard
    	if( fields[DAY_OF_MONTH] != WILDCARD_NO )
    		if( fields[DAY_OF_MONTH] < 1 ) 
    			fields[DAY_OF_MONTH] = 1;
    		//make sure the day does not exceed the month, (as long as the month isn't a wildcard)
    		else if( fields[MONTH] != WILDCARD_NO && (fields[DAY_OF_MONTH] > MONTH_LENGTH[fields[MONTH]]) )
    			fields[DAY_OF_MONTH] = MONTH_LENGTH[ fields[MONTH] ];
    	
    	//check the day of week is ok
    	if( fields[DAY_OF_WEEK] != WILDCARD_NO )
    	{
    		if( fields[DAY_OF_WEEK] < 0 )
    			fields[DAY_OF_WEEK] = 0;
    		else if( fields[DAY_OF_WEEK] > 6 )
    			fields[DAY_OF_WEEK] = 6;
    		
    		if( fields[DAY_OF_MONTH] != WILDCARD_NO )
    			//check that the day of the week agrees with the day of the month
    			fields[DAY_OF_WEEK] = getDayOfYear( fields[MONTH], fields[DAY_OF_MONTH] ) % 7;
    	}
    	
    	//set the day of the year
    	fields[DAY_OF_YEAR] = getDayOfYear( fields[MONTH], fields[DAY_OF_MONTH] ); 
    	
    	//check the hour of the day is ok
    	if( fields[HOUR] != WILDCARD_NO )
    		if( fields[HOUR] < 0 )
    			fields[HOUR] = 0;
    		else if( fields[HOUR] > 23 )
    			fields[HOUR] = 23;
    	
    	//check the minute of the day is ok
    	if( fields[MINUTE] != WILDCARD_NO )
    		if( fields[MINUTE] < 0 )
    			fields[MINUTE] = 0;
    		else if( fields[MINUTE] > 59 )
    			fields[MINUTE] = 59;
    }
    
    
    /**
    *	Converts the month and its date to days in the year.
    *	The two fields are assumed to be within their correct 
    *	ranges.
    */
    private static final int getDayOfYear(int month, int date)
    {
    	//convert the month to days
    	int dayCount = 0;
    	for(int i=0;i<month;i++)
    		dayCount += MONTH_LENGTH[i];
    	
    	//add on the date of the month and return
    	return dayCount + date;
    }
    
    /**
    *	Converts the month and its date into a day of the week
    */
    private static final int getDayOfWeek(int month, int date)
    {	
    	return getDayOfYear(month, date-1)%7;
    }
    
    
    
    /**
    *	Sets the MONTH field with the given value.
    *	Value is adjusted if it is below 0 or above 11
    *
    *	@param value		the month to set
    */
    public final void setMonth(int value)
    {
    	if( value < 0 )
    		value = 0;
    	else if( value > 11 )
    		value = 11;
    	
    	fields[MONTH] = value;
    }
    
    /**
    *	Sets the DAY_OF_MONTH field.
    *	If the value given exceeds the day of the month, the
    *	number will be rounded down to within that months limit.
    *	To work properly, the MONTH field may have to be set.
    *
    *	@param value		the date of the month to set
    */
    public final void setDayOfMonth(int value)
    {
    	if( value < 1 )
    		value = 1;
    	else if( value > MONTH_LENGTH[ fields[MONTH] ] )
    		value = MONTH_LENGTH[ fields[MONTH] ];
    	
    	fields[DAY_OF_MONTH] = value;
    	
    	//set the DAY_OF_WEEK field to agree with the month
    	fields[DAY_OF_WEEK] = getDayOfWeek( fields[MONTH], fields[DAY_OF_MONTH] );
    }
    
    /**
    *	Sets the DAY_OF_WEEK field
    *	If the value given exceeds the day of the month, the
    *	number will be rounded down to the last day.
    *
    *	@param value		the day of the week to set
    */
    public final void setDayOfWeek(int value)
    {
    	if( value < 0 )
    		value = 0;
    	else if( value > 6 )
    		value = 6;
    	
    	fields[DAY_OF_WEEK] = value;
    }
    
    
    
    /**
    *	Sets the HOUR field.
    *	If the value given exceeds the number of hours in a day
    *	it is set to the last hour of the day.
    *
    *	@param value		the hour of the day to set
    */
    public final void setHour(int value)
    {
    	if( value < 0 )
    		value = 0;
    	else if( value > 23 )
    		value = 23;
    	
    	fields[HOUR] = value;
    }
    
    /**
    *	Sets the MINUTE_OF_HOUR field
    */
    public final void setMinute(int value)
    {
    	//adjust the value if it exceeds the limits
    	if( value < 0 )
    		value = 0;
    	else if( value > 59 )
    		value = 59;
    	
    	fields[MINUTE] = value;
    }
    
    /**
    *	Returns the time. The hour is formatted on the 12hr clock
    *
    *	@return			a string in the format <hour>:<minute><am|pm>
    */
    public String toTimeString()
    {
    	return formatHourMinute();
    }
    
    /**
    *	Returns the date.
    *
    *	@return			a string in the format <day> the <day of month> 
    *					of <month>, <year>
    */
    public String toDateString()
    {
    	return dayString[fields[DAY_OF_WEEK]] + " the " + formatDate() + " of " + 
    			monthString[ fields[MONTH] ] + ", " + fields[YEAR];
    }
    
    /**
    *	Returns a string of the gamedate in the form:
    *		<day> the <date> of <month> <year>, <time>
    */
    public String toString()
    {
    	return dayString[fields[DAY_OF_WEEK]] + " the " + formatDate() + " of " +
    			monthString[ fields[MONTH] ] + " " + fields[YEAR] + ", " + formatHourMinute();
    }
    
    /**
    *	Returns a basic string representation of the date
    */
    public String toFieldString()
    {
    	return fields[MONTH] + "-" + fields[DAY_OF_MONTH] + "-" + fields[DAY_OF_WEEK] + "-" + fields[HOUR] + "-" + fields[MINUTE];
    }
    
    /**
    *	Returns the leap year status of this object
    */
    public final boolean getLeapYear()
    {
    	return leapYear;
    }
    
    
    
	
    //=================================================================================//
    //--------------------------------- Private Methods -------------------------------//
    /**
    *	Tokenises and places into an array. All values found will
    *	be parsed into an Integer. Wildcard characters will be 
    *	turned into a Integer with a value of -1.
    *
    *	@param dateString	a delimited list of numbers/wildcards representing
    *						a date.
    */
    private final void loadDateString(String dateString)
    {
    	StringTokenizer t = new StringTokenizer(dateString, " -,");
    	
    	String token = "";
    	
    	for(int i=0;i<5;i++)
    	{
    		try{
    			token = t.nextToken();
    		} catch (NoSuchElementException e) { 
    			new GameDateFormatException(" Wrong number of arguments for date string. ");
    		}
    		
    		// If the token is a wildcard character then set the wildcard constant on the
    		// game field
    		if( token.charAt(0) == WILDCARD_CHAR )
    			//throw new GameDateFormatException("Wildcards in datestrings not yet implemented.");
    			this.setField( i, WILDCARD_NO );
    			
    		// Otherwise its a valid date field. Try to see whether its a text field or just a number.
    		else
    		{
    			//the token is a number, so set the relevant field straight away
    			if( Character.isDigit(token.charAt(0)) )
    				this.setField( i, new Integer(token).intValue() );
    			
    			//the token is a character (possibly part of a
    			else if( Character.isLetter(token.charAt(0)) )
    			{
    				int result = -1;
    				if( (result = interpetDayString(token)) != -1) 
    					this.setField( i, result );
    				//if the intepreter had no joy, then throw an error
    				else
    					throw new GameDateFormatException("Unknown day name");
    			}
    			else
    				throw new GameDateFormatException("Invalid token type");
    	    }
    	}
    }//*/
    

    
    /**
    *	Attempts to convert a given string into a index number representing a day
    *
    
    */
    private static final int interpetDayString(String string)
    {
    	for(int i=0;i<dayString.length;i++)
    	{
    		//try the long hand
    		if( (string.toLowerCase() == (dayString[i].toLowerCase())) || 
    			string.toLowerCase().equals(dayString[i].substring(0,3).toLowerCase()) )
    			return i;
    	}
    	return -1;
    }
    
    /**
    *	Sets a date field within the date. This automatically
    *	invalidates the date.
    *
    *	@param field		the field to alter
    *	@param value		the value to alter the field with
    */
    protected final void setField(int field, int value)
    {
    	this.fields[field] = value;
    }
    
    /**
    *	Returns a date field.
    *
    *	@return				the field to return
    */
    protected final int getField(int field)
    {
    	return fields[field];
    }
    
    /**
    *	Sets the MONTH and the DAY_OF_MONTH from
    *	the DAY_OF_YEAR
    *
    *	@param day			the day of the year (0-365)
    */
    private void setDayOfMonthAndMonth(int day)
    {
    	int count = 0;
    	//iterate through are array of month lengths
    	for(int i=0;i<MONTH_LENGTH.length;i++)
    	{
    		//is the day less than the total days in the months, 
    		// up to the end of this month ? 
    		if( day < (count + MONTH_LENGTH[i]) )
    		{
    			fields[MONTH] = i;
    			//to get the date, subtract our month total from the day
    			setDayOfMonth( (fields[DAY_OF_YEAR] - count) + 1 );
    			//fields[DAY_OF_MONTH] = (fields[DAY_OF_YEAR] - count) + 1;
    			//we have what we came for - lets go
    			return;
    		}
    		count += MONTH_LENGTH[i];
    	}
    }

	
    
    /**
	*	Sets all the relevant fields from a seconds value.
	*	The seconds value is assumed to be seconds from 
	*	the beginning of the year.
	*
	*	@param seconds		number of seconds from the beginning of the year
	*/
    private void setFieldsFromSeconds( int seconds )
    {
    	//figure out the DAY_OF_YEAR from the seconds
    	fields[DAY_OF_YEAR] = seconds/ONE_DAY;
    	
    	int remainder = seconds%ONE_DAY;
    	
    	//figure out the HOUR from the remainder
    	fields[HOUR] = remainder/ONE_HOUR;
    	
    	remainder = remainder%ONE_HOUR;
    	
    	//figure out the MINUTE from the remainder
    	fields[MINUTE] = remainder/ONE_MINUTE;
    	
    	remainder = remainder%ONE_MINUTE;
    	
    	//figure out the SECOND from the remainder
    	fields[SECOND] = remainder;
    	
    	//figure out the WEEK_OF_YEAR from the DAY_OF_YEAR
    	fields[WEEK_OF_YEAR] = fields[DAY_OF_YEAR]/7;
    	
    	//figure out the MONTH and the DAY_OF_MONTH from the DAY_OF_YEAR
    	// this will also set the DAY_OF_WEEK
    	setDayOfMonthAndMonth( fields[DAY_OF_YEAR] );
    }

    /**
    *	Formats the hour into a print ready form
    *	including the hemicycle identifier.
    */
    private final String formatHourMinute()
    {
    	//pm
    	int hour = fields[HOUR];
    	if( fields[HOUR] > 12 )
    		hour = fields[HOUR] - 12;
    		
    	if( fields[HOUR] >= 12 )
    			return hour + ":" + formatTimeNumber(fields[MINUTE]) + hemicycle[1];
    	else //am
    		return hour + ":" + formatTimeNumber(fields[MINUTE]) + hemicycle[0];
    }
    
    /**
    *	Inserts a leading '0' to numbers below 10
    *
    *	@param number		the number to format into a string
    *	@return				the string-ifyed number
    */
    private static final String formatTimeNumber(int number)
    {
    	if(number<10)
    		return "0" + number;
    	else
    		return new Integer(number).toString();
    }
    
    /**
    *	Formats the date (of the month) into a print
    *	ready form including the postfix.
    *
    *	@return				a string representing the date
    */
    private final String formatDate()
    {
    	//convert the date into a string.
    	String date = new Integer(fields[DAY_OF_MONTH]).toString();
    	
    	if( date.charAt( date.length()-1 ) == '1' )
    		return date + "st";
    	
    	else if( date.charAt(0) != '1' && date.charAt( date.length()-1 ) == '2' )
    		return date + "nd";
    	
    	else if( date.charAt(0) != '1' && date.charAt( date.length()-1 ) == '3' )
    		return date + "rd";
    	
    	else
    		return date + "th";
    }
    
    /**
    *	Sets whether this year is a leap year.
    *
    *	@param leapYear		true if the year is a leap, false otherwise
    */
    protected final void setLeapYear(boolean leapYear)
    {
    	this.leapYear = leapYear;
    	
    	if(leapYear)
    		MONTH_LENGTH = LEAP_MONTH_LENGTH;
    	else
    		MONTH_LENGTH = NORMAL_MONTH_LENGTH;
    }
    
    
    /**
	*   Returns the difference in days between to days
	*   (ie MONDAY and THURSDAY)
	*	
	*	@param dayOne
	*	@param dayTwo
	*/
	public int getDayDifference(int dayOne, int dayTwo)
	{
    	if( dayOne == dayTwo )
        	return 0;
    	else if( dayOne < dayTwo )
        	return dayTwo - dayOne;
    	else // ( dayOne > dayTwo )
        	return (7-dayOne) + dayTwo;
	}
	
    /**
    *	Returns the difference between the existing month and 
    *	the passed value. The passed value is always assumed 
    *	to be after the current time
    *
    *	@param value		a month in the range 0-11
    *	@returns			the difference in months between the current
    *						and the passed value
    */
    private final int getMonthDifference( int newMonth )
    {
    	if( newMonth > fields[MONTH] )
    		return newMonth - fields[MONTH];
    		
    	else //if( value < fields[MONTH] )
    		return 11-fields[MONTH] + newMonth;
    }
    
    /**
	*	Increments the Minute. If the minute exceeds its limit, it
	*	will reset to 0 and increment the Hour
	*/
	protected final void incrementMinute()
	{
		if( this.getField(MINUTE) < 59 )
			fields[MINUTE] += 1;
		else
		{
			fields[MINUTE] = 0;
			incrementHour();
		}
	}
	
	/**
	*	Increments the Hour. If the hour exceeds its limit, it will
	*	reset to 0 and increment the Day Of Month.
	*/
	protected final void incrementHour()
	{
		if( this.getField(HOUR) < 23 )
			fields[HOUR] += 1;
		else
		{
			fields[HOUR] = 0;
			incrementDayOfMonth();
		}
	}
	
	/**
	*	Increments the Day Of Month. If the day of month exceeds its limit,
	*	it will reset to 0 and increment the month
	*/
	protected final void incrementDayOfMonth()
	{
		if( fields[DAY_OF_MONTH] < MONTH_LENGTH[fields[MONTH]] )
			fields[DAY_OF_MONTH] += 1;
		else
		{
			fields[DAY_OF_MONTH] = 1;
			incrementMonth();
		}
	}
	
    /**
	*   Increments the month by one as long as it is within the
	*   total number of months. If not, the month is set back to 0.
	*/
	protected final void incrementMonth()
	{
    	if( getField(MONTH) < (MONTH_LENGTH.length-1) )
    		fields[MONTH] += 1;
        	
    	else
    	{
    		fields[MONTH] = 0;
        	//another year has passed, so increment the server years
        	incrementYear();
        }
	}
	
	/**
	* Increments the server year
	*/
	protected final void incrementYear()
	{
		fields[SERVER_YEARS] += 1;
	}
	
	
    /**
	*   Adjusts the fields of another GameDate object using this object
	*   as a time reference. This includes intepreting of wildcards.
	*
	*	@param currentDate		the date with which to compare the new,
	*							resolved date.
	*/
	protected final void resolveWildcards( GameDate currentDate )
	{
		//a set of booleans which will indicate which fields are
		// wildcards.
    	boolean wildcard[] = new boolean[5];
	    
	    //the time relative to the currentDate
	    boolean timeState = PRESENT;
	    
	    //-------------------
    	//----- Process Month
    	//look at month. if it is a wildcard, set it to the current month.
    	if( this.getField(MONTH) == WILDCARD_NO )
    	{
        	this.setField( MONTH, currentDate.getField(MONTH) );
        	wildcard[MONTH] = true;
    	}
    	//the month in the string is a required field
	    else
	    {
	    	//if the month is not equal, then we have a time which
	    	// is past the current time
	    	if( this.getField(MONTH) > currentDate.getField(MONTH) ||
	    		this.getField(MONTH) < currentDate.getField(MONTH) )
	    		timeState = POST;
	    	
	    	//if the new month is less than the current, then a year must
	    	// have passed
	    	if( this.getField(MONTH) < currentDate.getField(MONTH) )
	    		incrementYear();
	    }
	    //--------------------------
    	//----- Process Day of Month
    	//look at the date. if it is a wildcard, set it to the current date
    	if( this.getField(DAY_OF_MONTH) == WILDCARD_NO )
    	{
    		//if we have already passed beyond the current time
    		if( timeState == POST )
    			this.setField( DAY_OF_MONTH, 1 );
    		else
        		//set the date to the current date
        		this.setField( DAY_OF_MONTH, currentDate.getField(DAY_OF_MONTH) );
        		
        	wildcard[DAY_OF_MONTH] = true;
    	}
    	//if the dates aren't the same and the month is something we can alter ...
    	else if( wildcard[MONTH] && (this.getField(DAY_OF_MONTH) != currentDate.getField(DAY_OF_MONTH)) )
    	{
        	//the gamedate is beyond this date.
        	if( this.getField(DAY_OF_MONTH) > currentDate.getField(DAY_OF_MONTH) )
        	{
            	//is the date within its months limit ? if not, increment the month
            	// until we the date is valid.
            	while( this.getField(DAY_OF_MONTH) > MONTH_LENGTH[this.getField(MONTH)] )
            	{
                	this.incrementMonth();
                	timeState = POST;
	            }
            	//otherwise we leave the month alone, as it is already correct
        	}
	        
        	//if the date has already passed us by this month, then move to the next month
        	else if( this.getField(DAY_OF_MONTH) < currentDate.getField(DAY_OF_MONTH) )
        	{
            	this.incrementMonth();
            	timeState = POST;
            }
    	}
	    
	    //--------------------------
    	//------ Process Day of week
    	
    	//If the date is a wildcard and the day isn't, then set the date
    	// from the day
    	if( wildcard[DAY_OF_MONTH] && this.getField(DAY_OF_WEEK) != WILDCARD_NO )
    	{
        	//see what we set in the date already. Any day will have to 
        	// come after this.
        	int currentDay = getDayOfWeek( this.getField(MONTH), this.getField(DAY_OF_MONTH) );
	        
        	//get the number of days until the required day appears        
        	int requiredDay = currentDate.getField(DAY_OF_MONTH) + getDayDifference( currentDay, this.getField(DAY_OF_WEEK) );
	        
        	//if the required day exceeds the total number of days left in this month
        	if( requiredDay > MONTH_LENGTH[this.getField(MONTH)] )//MONTH_LENGTH[this.getField(MONTH)] )
        	{
            	//if we can alter the month, then do so ...    
            	if( wildcard[MONTH] )
            	{
                	//remove the month length from the required date
                	requiredDay -= MONTH_LENGTH[this.getField(MONTH)];
                	//and set it as our final value for day of the month
                	this.setField( DAY_OF_MONTH, requiredDay );
                	//and increment the month
                	this.incrementMonth();
	                
	                timeState = POST;
            	}
            	//if we can't alter the month, then we are going to have to look
            	// from the beginning of this current month for the first day match
            	// the date will effectively be a year after the current...
            	else
            	{
                	//get the difference between the first day of the month and the 1st date that
                	// the day appears on
                	int diff = getDayDifference( getDayOfWeek(this.getField(MONTH), 1),  this.getField(DAY_OF_WEEK) );
	                
                	//set the day of the month
                	this.setField( DAY_OF_MONTH, diff );
            	}
        	}
        	//the required day is within the number of days left in the month, so set the date accordingly
        	else
        		this.setField( DAY_OF_MONTH, requiredDay );
    	}
    	// otherwise, in all other cases set the day from the date ( and the month )
    	else
    	{
        	//assign day
        	this.setDayOfWeek( getDayOfWeek(this.getField(MONTH), this.getField(DAY_OF_MONTH)) );
        	//this.setField( DAY_OF_WEEK, getDayOfWeek(this.getField(MONTH), this.getField(DAY_OF_MONTH)) );
        	wildcard[DAY_OF_WEEK] = true;
    	}
		
		//-------------------
    	//------ Process Hour
    	
    	//if a wildcard was specified, set it to the current time
    	if( this.getField(HOUR) == WILDCARD_NO )
    	{
    		//if we have already passed beyond the current time
    		if( timeState == POST )
    			this.setField( HOUR, 0 );
    		else
        		this.setField( HOUR, currentDate.getField(HOUR) );
        	
        	wildcard[HOUR] = true;
    	}
    	//a wildcard was not specified...
    	else
    	{
        	//if we have already passed this hour of day and the dayof month has
        	//not been altered, then we need to see whether we can increment the day of month
        	if( (this.getField(HOUR) < currentDate.getField(HOUR)) &&
        		(this.getField(DAY_OF_MONTH) <= currentDate.getField(DAY_OF_MONTH)) )
        	{

        		//else 
            	//if the day of month is a wildcard, then increment it
            	if( wildcard[DAY_OF_MONTH] )
            	{
                	//check whether the day_of_month can be incremented any more...
                	if( this.getField(DAY_OF_MONTH) < MONTH_LENGTH[ this.getField(MONTH) ] )
                	{
                		//it can, so lets increment the day of the month
                		this.setDayOfMonth( this.getField(DAY_OF_MONTH) + 1 );
                	
                	}
                	// ...otherwise the day of month cannot be incremented anymore without busting
                	// the day limit of the month, so we need to see whether the month can be incremented.
                	else 
                	{
                		if( wildcard[MONTH] )
                			//increment the month (it will wrap round to 0 if > 11)
                			this.incrementMonth();
	                	
                		//set the day of month back to 1 whether we incremented the month or not
                		this.setDayOfMonth( 1 );
                	}
            	}
        	}
        	//if the hour has advanced, then we need to mark this
        	else if( this.getField(HOUR) != currentDate.getField(HOUR) )
    			timeState = POST;
    	}
	    
	    //----------------------
    	//------- Process Minute
    	
    	//if a wildcard was specified, set it to the current time
    	if( this.getField(MINUTE) == WILDCARD_NO )
    	{
    		//if we have already passed beyond the current time
    		if( timeState == POST )
    			this.setField( MINUTE, 0 );
    		else
    			this.setField( MINUTE, currentDate.getField(MINUTE) );
    		wildcard[MINUTE] = true;
    	}
    	// a wildcard was not specified
    	else
    	{
    		//if we have already passed this minute of the hour, then we need to see whether we
    		// can increment the hour.
    		if( this.getField(MINUTE) < currentDate.getField(MINUTE) &&
        		(this.getField(HOUR) <= currentDate.getField(HOUR)) )
    		{
    			//if the day of the month is a wild card, then increment it
    			if( wildcard[HOUR] && (this.getField(HOUR) < 23) )
    			{
    				//if we can still increment hours, then do so
    				this.setField( HOUR, this.getField(HOUR)+1 );
    			}
    			//if we cannot, then try and increment the day of month
    			else if(wildcard[DAY_OF_MONTH] && (this.getField(DAY_OF_MONTH) < MONTH_LENGTH[this.getField(MONTH)]) ) 
    			{
    				this.setDayOfMonth( this.getField(DAY_OF_MONTH)+1 );
    			}
    			else if( wildcard[MONTH] )
    			{
    				this.incrementMonth();			
    				this.setDayOfMonth( 1 );
    			}
    			else
    			{
    				incrementYear();
    			}
    			timeState = POST;
    		}
    		//add this to ensure that minutes beyond the current time are
    		// treated as advancing this time after the current.
    		else if( this.getField(MINUTE) != currentDate.getField(MINUTE) )
    			timeState = POST;
    	}
    	
    	//if the time has worked out to be exactly the same as the current time,
    	// then increment minute by one.
    	if( timeState == PRESENT )
    		incrementMinute();
	}

	
	
	
	public static void main(String args[])
	{
		/*
    	//System.out.println("Game Date");
    	String cron = "8-23-*-1-1";
    	System.out.println("wildcard = " + cron);
    	
    	GameDate now = new GameDate( 8, 23, 1, 1 );
    	GameDate then = new GameDate( cron, now );
    	
    	//int time = (ONE_DAY*235)+ONE_HOUR+ONE_MINUTE;
    	
    	//GameDate date = new GameDate( time );
    	System.out.println( "one year = " + ONE_YEAR + "\n : now=" + now.getSeconds() + " then=" + then.getSeconds() );
    	//*/
    }
    
}


/**
*	A specific runtime exception thrown in the fray of GameDate
*/
public class GameDateFormatException extends IllegalArgumentException
{
	public GameDateFormatException() { }
    public GameDateFormatException(String s) { super(s); }
}

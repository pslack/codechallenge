/*
 * Copyright (C) 2016 Peter J Slack
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.pjslack.codechallenge.searchengine;

import com.pjslack.codechallenge.CodeChallenge;
import java.util.ArrayList;
import java.util.HashMap;
import javax.json.JsonArray;
import javax.json.JsonObject;

/**
 * An Abstract class to use as a foundation to test various methods of search and match
 * 
 * @author Peter J Slack
 */
public abstract class AbstractSearchEngine
{
    /**
     * An abstract method for the implementing class process and return
     * matches
     * 
     * @return  HashMap<String,ArrayList<JsonObject>> the string is the unique product name
     * and the ArrayList is of the listing json objects of this form:
     * 
     * {
     *   "product_name": String
     *   "listings": Array[Listing]
     * }
     */
    public abstract void process();
    /**
    * Return the number of matches by this processor
    * @return int - number of matches processed by this implementation 
    */
    public abstract int getNumberOfMatches();
    /**
     * Return the number of listings not matched
     * @return int - number of links that were not matched
     */
    public abstract int getNumberOfMisses();
    /**
     * Any implementation must implement the constructor that takes the CodeChallenge object
     * 
     * @param c 
     */
    
    /**
     * Return the matched listings
     * @return Hashmap<String,ArrayList<JsonObject>> where the key is product_name and the arraylist of json objects
     * are the original listings that were matched up
     */
    public abstract HashMap<String, ArrayList<JsonObject>> getResults();
    
    /**
     * Returns the implementation name
     * @return String representing the implementation name 
     */
    public abstract String getImplementationName();
    
    /**
     * Returns the description of the implementation
     * @return String representing the description of the implementation
     */
    public abstract String getImplementationDescription();
    
    public AbstractSearchEngine(CodeChallenge c)
    {
                
    }  
   
}

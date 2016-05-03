/**
 * ********************************************************************
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
/**
 *  This is the main package for the code challenge. There files set up the data
 *  for processing by an AbstractSearchEngine class
 *
 * @see com.wavedna.codechallenge.searchengine
 */
package com.pjslack.codechallenge;

import com.pjslack.codechallenge.impl.SlackerTestMethod;
import com.pjslack.codechallenge.searchengine.AbstractSearchEngine;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;

import javax.json.stream.JsonParsingException;

/**
 * This is a Framework Class and main entry point for the code challenge refer
 * to @README.md
 *
 */
public final class CodeChallenge
{

    /**
     * A fixed Resource URL for products listing included in the build jar
     */
    public static final String LISTINGS_RESOURCE_PATH = "/listings.txt";

    /**
     * A fixed Resource URL for products listing data set included in the build
     * jar
     */
    public static final String PRODUCTS_RESOURCE_PATH = "/products.txt";

    /**
     * The raw product JSON array products are expected in JSON line format
     * Product { "product_name": String // A unique id for the product
     * "manufacturer": String "family": String // optional grouping of products
     * "model": String "announced-date": String // ISO-8601 formatted date
     * string, e.g. 2011-04-28T19:00:00.000-05:00 } * } }
     */
    private final JsonArray products;

    /**
     * Listing
     *
     * {
     * "title": String // description of product for sale "manufacturer": String
     * // who manufactures the product for sale "currency": String // currency
     * code, e.g. USD, CAD, GBP, etc. "price": String // price, e.g. 19.99,
     * 100.00 }
     */
    private final JsonArray listings;

    /**
     * this is used to check the uniqueness of the given product set we will
     * check against, this contains a validated data set to use for the
     * processing
     */
    private final HashMap<String, JsonObject> productKeys = new HashMap<>();

    /**
     * this is used to check the uniqueness of the given product listing set we
     * will check it contains a validated data set to use for the processing
     */
    private final HashMap<String, JsonObject> listingKeys = new HashMap<>();

    /**
     * any listings that are duplicates we put here so we only search them once.
     * A listing with a different price but identical title is of no consequence
     * to the search. At the end of the search th implementation must match up
     * duplicate listings to the ones that have been matched up to a product.
     *
     */
    private final HashMap<String, ArrayList<JsonObject>> duplicateListings = new HashMap<>();

    /**
     * Field name used in listing and product list to identify manufacturer
     */
    public static final String PRODUCT_MANUFACTURER_KEY = "manufacturer";

    /**
     * Field name used in product list to identify product model
     */
    public static final String PRODUCT_MODEL_KEY = "model";

    /**
     * Field name used in product list to identify product family
     */
    public static final String PRODUCT_FAMILY_KEY = "family";

    /**
     * Field name used in product list to identify the unique product name key
     */
    public static final String PRODUCT_NAME_KEY = "product_name";

    /**
     * Field name used in listing to identify the title
     */
    public static final String PRODUCT_LISTING_TITLE_KEY = "title";

    /**
     * The total number of listings imported
     */
    private int totalListings = 0;

    /**
     * The number of duplicate listings detected and stored in duplicate listing object
     */
    private int numDuplicateListingsDetected = 0;

    /**
     * the total number of product definitions imported
     */
    private int numProductDefinitions = 0;

    /**
     * the number of invalid product definitions
     */
    private int numInvalidProductDefinitions = 0;

    /**
     * Program to test a search engine to match products to product listings
     *
     * @param args - no arguments required
     */
    public static void main(String[] args)
    {
        CodeChallenge c = null;
        try
        {
            c = new CodeChallenge();

        } catch (NullPointerException | IllegalStateException | JsonException | IOException ex)
        {
            Logger.getLogger(CodeChallenge.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (c == null)
        {
            System.exit(2);
        }

        //TODO: allow user to enter different search implementations
        AbstractSearchEngine se = new SlackerTestMethod(c);

        //let's see how fast this is
        long startTime = System.currentTimeMillis();

        se.process();

        long endTime = System.currentTimeMillis();

        long difference = endTime - startTime;

        HashMap<String, ArrayList<JsonObject>> results = se.getResults();

        //TODO: allow user to enter different output file names via command line
        String outputFile = System.getProperty("user.dir") + File.separator + "codeChallenge.txt";

        System.out.println();
        System.out.println();
        System.out.println("***************SEARCH ENGINE***********************");
        System.out.println("Search Implementation : " + se.getImplementationName());
        System.out.println("Description : " + se.getImplementationDescription());
        System.out.println();

        System.out.println("*************INPUT STATISTICS**********************");
        System.out.println("Total Product Defintions   : " + c.getTotalProductDefintions());
        System.out.println("Total Invalid Defintions   : " + c.getTotalInvalidProdctListings());
        System.out.println("Total Listings             : " + c.getTotalListings());
        System.out.println();

        System.out.println("*****************RESULTS***************************");
        System.out.println("Total Hits                 : " + se.getNumberOfMatches());
        System.out.println("Total Misses               : " + se.getNumberOfMisses());
        System.out.println("Elapsed Process Time (s)   : " + difference / 1000);
        System.out.println();
        System.out.println("Save file name             : " + outputFile);

        try
        {
            //sent the results to the file
            c.dumpResults(outputFile, results);
        } catch (IOException ex)
        {
            Logger.getLogger(CodeChallenge.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * Dumps the results of the match ups into JSON line format to the given
     * filename
     *
     * @param fileName - String of the full path of the filename
     * @param results - the result set from the match as
     * HashMap&lt;String&lt;ArrayList&lt;JsonObject&gt;&gt;
     * @throws FileNotFoundException - if the file cannot be create
     * @throws IOException - if the file cannot be written to
     */
    public void dumpResults(String fileName, HashMap<String, ArrayList<JsonObject>> results) throws FileNotFoundException, IOException
    {

        File outputFile = new File(fileName);
        if (outputFile.exists())
        {
        } else
        {
            outputFile.createNewFile();
        }

        FileOutputStream fos = new FileOutputStream(outputFile);

        try (OutputStreamWriter osw = new OutputStreamWriter(fos))
        {
            for (String productName : results.keySet())
            {

                ArrayList<JsonObject> listings = results.get(productName);
                JsonArrayBuilder resultArray = Json.createArrayBuilder();
                listings.stream().forEach((j)
                        -> 
                        {
                            resultArray.add(j);
                });
                JsonArray outputSet = resultArray.build();

                JsonObject outputValue = Json.createObjectBuilder()
                        .add("product_name", productName)
                        .add("listings", outputSet)
                        .build();

                String outPre = outputValue.toString();

                osw.write(outPre + "\n");

            }
        }

    }

    /**
     * The main constructor for the Code Challenge
     *
     * @throws NullPointerException if the input stream definitions are null
     * @throws IllegalStateException if the JSON parsing state is invalid
     * @throws JsonException JSON errors
     * @throws JsonParsingException JSON parsing error
     * @throws IOException error reading the input streams
     */
    public CodeChallenge() throws NullPointerException, IllegalStateException, JsonException, JsonParsingException, IOException
    {
        //we are given this data for the challenge, it is embedded in this jar
        //let's make it into useable form by reading it in.
        listings = loadResourceStream(this.getClass().getResourceAsStream(CodeChallenge.LISTINGS_RESOURCE_PATH));
        products = loadResourceStream(this.getClass().getResourceAsStream(CodeChallenge.PRODUCTS_RESOURCE_PATH));

        assert products != null;
        assert listings != null;

        totalListings = listings.size();
        numProductDefinitions = products.size();

        buildKeyMaps();

    }

    /**
     * Builds the product and listing key maps for use in the search engine
     * implementation
     */
    private void buildKeyMaps()
    {

        for (int i = 0; i < listings.size(); i++)
        {

            JsonObject j = listings.getJsonObject(i);
            checkListingEntryStructure(j);
        }

        for (int i = 0; i < products.size(); i++)
        {
            JsonObject j = products.getJsonObject(i);
            if (!checkProductEntryStructure(j))
            {
                numInvalidProductDefinitions++;
            }
        }

    }

    /**
     *
     * @return - the map of unique product names to the corresponding JsonObject
     * for the product
     */
    public HashMap<String, JsonObject> getProductKeys()
    {
        return productKeys;
    }

    /**
     *
     * @return - the hash map of unique title to the corresponding JsonObject
     * for the product listing
     */
    public HashMap<String, JsonObject> getListingKeys()
    {
        return listingKeys;
    }

    /**
     * This function checks to see if the key string is unique and places the
     * corresponding JsonObject in the product map
     *
     * @param j String - unique string for the product
     * @param obj JsonObject - JsonObject to be mapped
     * @param map The hash map that will contain the unique key mapping
     * @return
     */
    private boolean checkKeyUniqueness(String j, JsonObject obj, HashMap<String, JsonObject> map)
    {
        boolean rval = true;

        assert j != null;
        assert obj != null;
        if (map.containsKey(j))
        {
            rval = false;
        } else
        {
            map.put(j, obj);

        }

        return rval;
    }

    /**
     * Verifies the structure of the product map provided in JSON and builds the
     * unique list of products to be matched by the search engine
     *
     * @param j JsonObject - the product entry
     * @return boolean - true if structure is valid and product name is unique
     * false otherwise
     */
    public boolean checkProductEntryStructure(JsonObject j)
    {
        //start with assuming this is all good
        boolean rval = true;

        //first check the field structure .. this is JSON and can be any structure
        //but we want consistency in our input data for this match maker
        String productMfg = j.getString(PRODUCT_MANUFACTURER_KEY, null);
        String productModel = j.getString(PRODUCT_MODEL_KEY, null);
        String productFamily = j.getString(PRODUCT_FAMILY_KEY, null);
        String productKey = j.getString(PRODUCT_NAME_KEY, null);

        if (productMfg == null)
        {
            rval = false;
            Logger.getLogger(CodeChallenge.class.getName()).log(Level.WARNING, "Manufacturer key missing in product.. igonoring entry");
        } else
        {
        }
        if (productModel == null)
        {
            rval = false;
            Logger.getLogger(CodeChallenge.class.getName()).log(Level.WARNING, "Product Model key missing in Entry.. igonoring entry");
        } else
        {
        }
        if (productFamily == null)
        {
            //this does not invalidate the entry it is optional field
            //Logger.getLogger(CodeChallenge.class.getName()).log(Level.INFO, "Product Familly key missing..");
        }
        if (productKey == null)
        {
            rval = false;
            Logger.getLogger(CodeChallenge.class.getName()).log(Level.WARNING, "Product key missing.. igonoring entry");
        } else //the product key uniqueness checks and enters the entry into our map
        {
            if (rval)
            {
                if (!checkKeyUniqueness(productKey, j, productKeys))
                {
                    rval = false;
                    Logger.getLogger(CodeChallenge.class.getName()).log(Level.WARNING, "Product key is not unique..ignoring entry : " + productKey);
                }
            }
        }

        return rval;
    }

    /**
     * This function checks the structure of the product listing to be matched
     * this will build a list of duplicate titles that can be used later to
     * merge into listings that have been matched a duplicate listing is one
     * that has identical title to another but may have different price or
     * currency the duplicate only needs to be matched once so it is put aside
     * to be merged after the main match is done
     *
     * @param j
     * @return boolean - true if the listing structure is valid - false if the
     * structure is missing fields or of the listing is not unique
     */
    private boolean checkListingEntryStructure(JsonObject j)
    {

        boolean rval = true;
        String title = j.getString(PRODUCT_LISTING_TITLE_KEY, null);
        String mfg = j.getString(PRODUCT_MANUFACTURER_KEY, null);
        if (mfg == null)
        {
            rval = false;
            Logger.getLogger(CodeChallenge.class.getName()).log(Level.WARNING, "Manufacturer key missing in Listing.. igonoring entry");
        }
        if (title == null)
        {
            rval = false;
            Logger.getLogger(CodeChallenge.class.getName()).log(Level.WARNING, "Title missing in Listing.. igonoring entry");
        } else if (rval)
        {
            if (!checkKeyUniqueness(title, j, listingKeys))
            {
                //this is not an error, we build a list of duplicate listings to save on serach time
                rval = true;
                numDuplicateListingsDetected++;
                //we build our dupllicate listings here for use in the end game of matching
                // we don't want to search the same title many times.  A title can have
                // many prices but identical listings
                storeDuplicateListing(title, j, duplicateListings);

            }

        }
        return rval;
    }

    /**
     * Stores a duplicate listing to a structured object for matching
     *
     * @param title String - the title of the listing
     * @param j the json object of the original listing
     * @param listSet the set that we will store the duplicates in
     */
    private void storeDuplicateListing(String title, JsonObject j, HashMap<String, ArrayList<JsonObject>> listSet)
    {

        if (listSet.containsKey(title))
        {
            ArrayList<JsonObject> objList = listSet.get(title);
            objList.add(j);
            listSet.put(title, objList);
        } else
        {
            ArrayList<JsonObject> objList = new ArrayList<>();
            objList.add(j);
            listSet.put(title, objList);
        }

    }

    /**
     * Returns the duplicate listings found in the data loading phase
     *
     * @return - duplicate listings indexed by product name
     */
    public HashMap<String, ArrayList<JsonObject>> getDuplicateListings()
    {
        return duplicateListings;
    }

    /**
     * loads a JSON line formatted resource stream
     *
     * @param jsonData an input stream of json data to read in
     * @return returns a json array of the input data read in from a JSON line
     * format
     * @throws NullPointerException - do not pass a null input stream
     * @throws JsonException - exception parsing JSON data in the stream
     * @throws JsonParsingException - exception parsing JSON data in the stream
     * @throws IllegalStateException - illegal JSON state
     * @throws IOException - error reading in stream of JSON data
     */
    public JsonArray loadResourceStream(InputStream jsonData) throws NullPointerException, JsonException,
            JsonParsingException, IllegalStateException, IOException
    {

        if (jsonData == null)
        {
            throw (new NullPointerException("Input Stream cannot be null cannot be empty"));
        }

        JsonBuilderFactory factory = Json.createBuilderFactory(null);
        JsonArrayBuilder builder = factory.createArrayBuilder();

        try
        {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(jsonData)))
            {
                String line;
                while ((line = reader.readLine()) != null)
                {

                    InputStream is = new ByteArrayInputStream(line.getBytes());
                    try (JsonReader jsonReader = Json.createReader(is))
                    {
                        JsonObject obj = jsonReader.readObject();
                        builder.add(obj);
                    }
                }
            }
        } catch (NullPointerException | JsonException | IllegalStateException | IOException e)
        {
            throw (e);
        }

        JsonArray rval = builder.build();

        return rval;
    }

    /**
     * A function to allow our implementations to report a product Definition
     * error
     */
    public void reportInvalidProductDefinition()
    {
        numInvalidProductDefinitions++;
    }

    /**
     * Returns the total number of duplicate listings that were detected
     *
     * @return the number of duplicate listings
     */
    public int getNumberOfDuplicateListings()
    {
        return numDuplicateListingsDetected;
    }

    /**
     *
     * @return the total number of listings read in
     */
    public int getTotalListings()
    {
        return totalListings;
    }

    /**
     *
     * @return the total number of product definitions read in
     */
    public int getTotalProductDefintions()
    {
        return numProductDefinitions;
    }

    /**
     *
     * @return detected invalid product descriptions, this includes non unique
     * entries in the models for a manufacturer
     */
    public int getTotalInvalidProdctListings()
    {
        return numInvalidProductDefinitions;
    }

}

package com.pjslack.codechallenge;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.stream.JsonParsingException;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for Code Challenge app
 */
public class CodeChallengeTest 
    extends TestCase
{
    
    public static final String MALFORMED_JSON_TEST_RESOURCE_PATH = "/malformedtest.txt";
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public CodeChallengeTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( CodeChallengeTest.class );
    }

    /**
     * We will embed the products listing and values in this example
     * test that they exist on the class path
     */
    public void testResourcesExist()
    {
        assertNotNull(this.getClass().getResource(CodeChallenge.LISTINGS_RESOURCE_PATH));
        assertNotNull(this.getClass().getResource(CodeChallenge.PRODUCTS_RESOURCE_PATH));
        
    }
    
    /**
     * Test that we have good JSON for each of the files
     */
    public void testloadResourceStream() throws NullPointerException, IllegalStateException, JsonException, JsonParsingException, IOException
    {
 
        CodeChallenge c = new CodeChallenge();
        JsonArray products = null;
        JsonArray listings = null;
        JsonArray badlistings = null;
        boolean tval = false;

        try {
            products = c.loadResourceStream(this.getClass().getResourceAsStream(CodeChallenge.PRODUCTS_RESOURCE_PATH));
        } catch (NullPointerException ex) {
            Logger.getLogger(CodeChallengeTest.class.getName()).log(Level.SEVERE, null, ex);
            tval = true;
        } catch (JsonException ex) {
            Logger.getLogger(CodeChallengeTest.class.getName()).log(Level.SEVERE, null, ex);
            tval = true;
        } catch (IllegalStateException ex) {
            Logger.getLogger(CodeChallengeTest.class.getName()).log(Level.SEVERE, null, ex);
            tval = true;
        } catch (IOException ex) {
            Logger.getLogger(CodeChallengeTest.class.getName()).log(Level.SEVERE, null, ex);
            tval = true;
        }

        try {
            listings = c.loadResourceStream(this.getClass().getResourceAsStream(CodeChallenge.LISTINGS_RESOURCE_PATH));
        } catch (NullPointerException ex) {
            Logger.getLogger(CodeChallengeTest.class.getName()).log(Level.SEVERE, null, ex);
            tval = true;
        } catch (JsonException ex) {
            Logger.getLogger(CodeChallengeTest.class.getName()).log(Level.SEVERE, null, ex);
            tval = true;
        } catch (IllegalStateException ex) {
            Logger.getLogger(CodeChallengeTest.class.getName()).log(Level.SEVERE, null, ex);
            tval = true;
        } catch (IOException ex) {
            Logger.getLogger(CodeChallengeTest.class.getName()).log(Level.SEVERE, null, ex);
            tval = true;
        }

        assertNotNull(products);
        assertNotNull(listings);
        assertFalse(tval);

        try {
            badlistings = c.loadResourceStream(this.getClass().getResourceAsStream(MALFORMED_JSON_TEST_RESOURCE_PATH));
        } catch (NullPointerException ex) {
            Logger.getLogger(CodeChallengeTest.class.getName()).log(Level.SEVERE, "null pointer", ex);
        } catch (JsonException ex) {
            Logger.getLogger(CodeChallengeTest.class.getName()).log(Level.SEVERE, "Json exception popo", ex);
            tval = true;
        } catch (IllegalStateException ex) {
            Logger.getLogger(CodeChallengeTest.class.getName()).log(Level.SEVERE, "Illegal state", ex);
        } catch (IOException ex) {
            Logger.getLogger(CodeChallengeTest.class.getName()).log(Level.SEVERE, "Io exception ", ex);
        }

        //we want listings to be null in the case it is messed up
        //and that we hit the json exception
        assertNull(badlistings);
        assertTrue(tval);

    }



}

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
package com.pjslack.codechallenge.impl;

import com.pjslack.codechallenge.CodeChallenge;
import com.pjslack.codechallenge.searchengine.AbstractSearchEngine;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.JsonObject;

/**
 * This is an implementation of search engine that first orchestrates the search
 * criteria into Object models in order to reduce the number of searches
 * required to match a product.
 *
 * This method will match a product only if there is either a manufacturer
 and/or family match and a model is identified in the title. the manufacturer
 is matched either in the manufacturer field of the listing or in the title of
 the listing. A family is matched in the title of the listing and is
 equivalent to finding a manufacturer name. Once one of these matches passes
 then the title is searched for a model name and passes the match if one is
 found. The search is optimized because we only search models that belong to
 manufacturer that has been identified.

 There are a few modifications to aid in the matching. First is an alias match
 for the manufacturer name Some manufacturers are known by the full name or by
 the acronym for the manufacturer. There are also cases where the manufacturer
 can be replaceable such as in cases where one company might buy another and
 the names become synonymous for the product lines

 The other subtle modification to aid in search is the generic model modifier.
 Manufacturers some times put identifiers in model names to specify a generic
 class of product types or some other internal product line identifier. for
 cameras example, SRL or DSLR can be used to specify a generic product type,
 many manufacturers. these can be generic technology type prefixModifiers that
 * 
 * This method constructs regular search expressions for each model based on the
 * generic model name 
 *
 *
 *
 * @author Peter J Slack
 */
public class SlackerTestMethod extends AbstractSearchEngine
{

    /**
     * model names by manufacturer index mapping
     */
    private final HashMap<String, HashMap<String, String>> modelByMfgMap = new HashMap<>();

    /**
     * for each regex pointer ( manufacturer @ family @ model ) a hashset of regular
     * search expressions to use to match model identifiers in a text string
     */
    private final HashMap<String,HashSet<String>> modelSearchRegex = new HashMap<>();
    

    /**
     * in our input products it is possible that models are duplicated across
     * families or are in error
     */
    private final HashMap<String, String> duplicateModelList = new HashMap<>();

    /**
     * collection of unique cases where an alias or initial can be used for a
     * manufacturer The Alias string is mapped to the know Manufacturer String
     * from the given list
     *
     */
    private final HashMap<String, String> aliasMfgMap = new HashMap<>();

    /**
     * this maps model name / manufacturer to a family
     */
    private final HashMap<String, HashMap<String, String>> modelByProductFamily = new HashMap<>();

    /**
     * Maps a product family to manufacturer
     */
    private final HashMap<String, String> mfgByProductFamily = new HashMap<>();

    /**
     * Unmatched listings
     */
    HashMap<String, JsonObject> unMatched = new HashMap<>();

    /**
     * Matched listings
     */
    HashMap<String, ArrayList<JsonObject>> matchedList = new HashMap<>();

    /** this umber of unmatched listings */
    private int numUnmatched=0;
    /** the number of matched listings*/
    private int numMatched=0;
    /** the number of matched from duplicate listings*/
    private int numDuplicateMatches=0;
    
    /**
     * the member variable pointer to our main code challenge core
     */
    private final CodeChallenge myCodeChallenge;

    /**
     * Constructor for the Super Slacker matching method
     *
     * @param c - main Code challenge instance
     */
    public SlackerTestMethod(CodeChallenge c)
    {
        super(c);
        myCodeChallenge = c;

        //assemble the know list of manufacturer aliases to aid in the search
        //Hewlett packard is sometimes used instead of HP
        aliasMfgMap.put("HEWLETTPACKARD", "HP");
        //Konica and minolta can be used independantly
        aliasMfgMap.put("KONICA", "KONICAMINOLTA");
        aliasMfgMap.put("MINOLTA", "KONICAMINOLTA");
        aliasMfgMap.put("FUJI", "FUJIFILM");

    }

    @Override
    public void process()
    {
        //build the object relational structure
        //and the regex search strings for models
        buildObjectRelationMaps();
        //do the big match
        match();
        
        //match up duplicate listings
        matchDuplicateListings();
        
        
 
    }

    @Override
    public int getNumberOfMatches()
    {
        //provide accounting for direct matches and duplicate matches
        return numMatched + numDuplicateMatches;

    }

    @Override
    public int getNumberOfMisses()
    {
        //the number of misses will also include the balance total duplicated listing matches
        return numUnmatched + myCodeChallenge.getNumberOfDuplicateListings() - numDuplicateMatches;
    }

    /**
     * This function builds an object model used to guide the match function.
     * Because the input data is flat JSON it is in a form that is not conducive
     * to optimization in Java.  It also builds a list of regular search expressions
     * (regexs) for searching each model string and the variations that may be used
     *
     * this function builds maps of the form: - model names identified by by
     * manufacturer - model names by product family - manufacturers by product
     * family
     *
     * A generic model name identifier is a sequence of digits that appear in 2
     * or more model names they typically show up as a prefix or suffix in the model name
     * and separated by either a space or a dash - model names by generic model
     * identifier
     *
     * Building an object model allows us another opportunity to verify the
     * input data for errors and inconsistencies in the data structures. This
     * method makes extensive use of hash maps to build key fields and
     * relationships to the final data we want to preset as results to the Code
     * Challenge object
     *
     */
    private void buildObjectRelationMaps()
    {
        myCodeChallenge.getProductKeys().keySet().stream().forEach((key) ->
        {
            JsonObject j = myCodeChallenge.getProductKeys().get(key);
            //the first conditioning is to convert to uppser case
            String conditionedMfg = conditionMfgString(j.getString(CodeChallenge.PRODUCT_MANUFACTURER_KEY));
            String conditionedModel = conditionModelString(j.getString(CodeChallenge.PRODUCT_MODEL_KEY));
            String family = conditionFamilyString(j.getString(CodeChallenge.PRODUCT_FAMILY_KEY, null));
            //build the model regex map
            String regexfam="";
            if(family!=null)
                regexfam=family;
            //build a key we can use to store regex model findall strings
            String regexKey=conditionedMfg+"@"+regexfam+"@"+conditionedModel;
            if (modelSearchRegex.containsKey(regexKey))
            {
                Logger.getLogger(SlackerTestMethod.class.getName()).log(Level.WARNING, "Duplicate Model key found inregex map:");
            }
            else
            {

                //every model include a basic match case as given in the table
                String basicSearch = "(?s).*\\b"+j.getString(CodeChallenge.PRODUCT_MODEL_KEY).toUpperCase()+"\\b.*";
                HashSet<String> al = new HashSet<>();
                al.add(basicSearch);
                //add a search based on our conditioned model string
                al.add("(?s).*\\b"+conditionedModel +"\\b.*");
                modelSearchRegex.put(regexKey, al);
            }
            if (modelByMfgMap.containsKey(conditionedMfg))
            {
                HashMap<String, String> models = modelByMfgMap.get(conditionedMfg);
                if (models.containsKey(conditionedModel))
                {
                    //this can either be a problem in input data 
                    //or we have a model number that is in a different family of products
                    //
                    Logger.getLogger(SlackerTestMethod.class.getName()).log(Level.WARNING, "Duplicate Model key found in MFG map:"
                            + key + " " + conditionedModel + " This may indicate an error in the product data used to search the listings");
                    //retrieve the duplicate model entry
                    JsonObject dup = myCodeChallenge.getProductKeys().get(models.get(conditionedModel));
                    duplicateModelList.put(conditionedModel, key);
                } else
                {
                    models.put(conditionedModel, key);
                }
            } else
            {
                HashMap<String, String> models = new HashMap<>();

                models.put(conditionedModel, key);

                modelByMfgMap.put(conditionedMfg, models);
            }
            //add the family to manufacturer mapping
            if (family != null)
            {
                if (!modelByProductFamily.containsKey(family))
                {
                    HashMap<String, String> mod = new HashMap<>();
                    mod.put(conditionedModel, key);
                    modelByProductFamily.put(family, mod);
                    mfgByProductFamily.put(family, conditionedMfg);
                } else
                {
                    modelByProductFamily.get(family).put(conditionedModel, key);
                    mfgByProductFamily.put(family, conditionedMfg);
                }
            }
        });

        //process the duplicate list here we see if a model has
        //the same value in 2 families, if not we log the error
        duplicateModelList.keySet().stream().forEach((dup) ->
        {
            JsonObject j = myCodeChallenge.getProductKeys().get(duplicateModelList.get(dup));
            String fam = conditionFamilyString(j.getString(CodeChallenge.PRODUCT_FAMILY_KEY, null));
            String Mfg = conditionMfgString(j.getString(CodeChallenge.PRODUCT_MANUFACTURER_KEY));
            String pkey = j.getString(CodeChallenge.PRODUCT_NAME_KEY);
            //in this case we have an error becasue our duplicate Product code has no family
            //therefor it is most likeley a data error we put up an error 
            if (fam == null)
            {
                Logger.getLogger(SlackerTestMethod.class.getName()).log(Level.SEVERE, "Duplicate model key unreconcilable, ignoring this record :" + j.toString());
                //tell code challenge we found an error
                myCodeChallenge.reportInvalidProductDefinition();
                
            } else if (modelByProductFamily.containsKey(fam))
            {
                modelByProductFamily.get(fam).put(dup, pkey);
            } else
            {
                HashMap<String, String> o = new HashMap<>();
                o.put(dup, pkey);
                modelByProductFamily.put(fam, o);
                mfgByProductFamily.put(fam, Mfg);
            }
        });
        //set up the generic model search regexs
        buildGenericModelModifierMap();

    }

    
    /**
     * This builds a generic model modifier map used to do finer grained search
     * where no model is identified using the conditioned model identifier
     * string. This allows us to remove the generic model modifier and so the
     * search again with the other part of the model name.  This adds new regexs to the 
     * search strings for the model name
     *
     */
    private void buildGenericModelModifierMap()
    {
        //for all manufacturers
        modelByMfgMap.keySet().stream().forEach((mfgKey) ->
        {
            //split model names into 2 and populate buckets
            //the bucket that has more than one match is a generic model modifier

            HashMap<String,String> list0 = new HashMap<>();
            HashMap<String,HashSet<String>> list1 = new HashMap<>();
            HashMap<String,HashSet<String>> list2 = new HashMap<>();
            HashMap<String,HashSet<String>> list3 = new HashMap<>();
            HashMap<String,HashSet<String>> list4 = new HashMap<>();
            HashMap<String,HashSet<String>> prefixModifiers = new HashMap<>();
            HashMap<String,HashSet<String>> suffixModifiers = new HashMap<>();
            
            
            //for all models
            HashMap<String, String> modelMap = modelByMfgMap.get(mfgKey);
            //for all models
            modelMap.keySet().stream().forEach((modelKey) ->
            {
                //let's first detect any prefix separatos (space and - )'
                JsonObject prod = myCodeChallenge.getProductKeys().get(modelMap.get(modelKey));
                String rawProduct = prod.getString(CodeChallenge.PRODUCT_MODEL_KEY);
                String family = conditionFamilyString(prod.getString(CodeChallenge.PRODUCT_FAMILY_KEY,""));
                String regexPointer = mfgKey+"@"+family+"@"+modelKey;
                //we only care about spaces and dashes as separators
                String[] split = rawProduct.split("[-_ ]");
                if (split.length == 1)
                {
                    
                    list0.put(modelKey,regexPointer);
                }
                if (split.length >= 2)
                {
                    HashSet<String> regexs;
                    
                    if (list1.containsKey(split[0]))
                    {
                        HashSet<String> regexPointers = list1.get(split[0]);
                        regexPointers.add(regexPointer);
                        list1.put(split[0], regexPointers);
                        prefixModifiers.put(conditionModelString(split[0]),regexPointers);
                        
                    } else
                    {
                        HashSet<String> regexPointers = new HashSet<>();
                        regexPointers.add(regexPointer);
                        list1.put(split[0], regexPointers);
                    }
                }
                if (split.length == 2)
                {

                    if (list2.containsKey(split[1]))
                    {
                        HashSet<String> regexPointers = list2.get(split[1]);
                        regexPointers.add(regexPointer);
                        list2.put(split[1], regexPointers);
                        suffixModifiers.put(conditionModelString(split[1]),regexPointers);
                    } else
                    {
                        HashSet<String> regexPointers = new HashSet<>();
                        regexPointers.add(regexPointer);
                        list2.put(split[1], regexPointers);
                    }

                }
                if (split.length == 3)
                {
                    if (list3.containsKey(split[2]))
                    {
                        HashSet<String> regexPointers = list3.get(split[2]);
                        regexPointers.add(regexPointer);
                        list3.put(split[2], regexPointers);
                        suffixModifiers.put(conditionModelString(split[2]),regexPointers);
                    } else
                    {
                        HashSet<String> regexPointers = new HashSet<>();
                        regexPointers.add(regexPointer);
                        list3.put(split[2],regexPointers);
                    }

                }
                if (split.length == 4)
                {
                    if (list4.containsKey(split[3]))
                    {
                        HashSet<String> regexPointers = list4.get(split[3]);
                        regexPointers.add(regexPointer);
                        list4.put(split[3], regexPointers);

                        suffixModifiers.put(conditionModelString(split[3]),regexPointers);
                    } else
                    {
                        HashSet<String> regexPointers = new HashSet<>();
                        regexPointers.add(regexPointer);
                        list4.put(split[3],regexPointers);
                    }
                }
            }); //end of first pass
            
            
            //we will look into madels that are not split to find any common prefixxes or suffixes
            HashMap<String,HashSet<String>> beginningSet = new HashMap<>();
            HashMap<String,HashSet<String>> endingSet = new HashMap<>();
            
            
            //this is for everything remaining that does not have a seprator
            //we want to look for alpha patterns on the ends or beginning of model strings
            //to discover new generic model prefix / suffix we are only interesed in alpha
            //prefix and suffix
            list0.keySet().stream().forEach((test2) ->
            {
                //remove all numbers
                String alpha = test2.replaceAll("[^a-zA-Z]","");
                //remove all alpha
                String number = test2.replaceAll("[^0-9]","");
                String regexPointer = list0.get(test2);
                boolean beginsAlpha = test2.matches("^" + alpha + ".*$");
                boolean endsAlpha = test2.matches("^.*" + alpha + "$");
                if (beginsAlpha && !endsAlpha)
                {
                    if (beginningSet.containsKey(alpha))
                    {
                        
                        HashSet<String> regexPointers = beginningSet.get(alpha);
                        regexPointers.add(regexPointer);                       
                        prefixModifiers.put(alpha, regexPointers);
                    } else
                    {
                        HashSet<String> regexPointers = new HashSet<String>();
                        regexPointers.add(regexPointer);
                        beginningSet.put(alpha,regexPointers);
                    }
                }
                if (endsAlpha && !beginsAlpha)
                {
                    if (endingSet.containsKey(alpha))
                    {
                        HashSet<String> regexPointers = endingSet.get(alpha);
                        regexPointers.add(regexPointer);                       
                        suffixModifiers.put(alpha, regexPointers);
                    } else
                    {
                        HashSet<String> regexPointers = new HashSet<String>();
                        regexPointers.add(regexPointer);
                        endingSet.put(alpha,regexPointers);
                    }
                }
            });

            prefixModifiers.keySet().stream().filter((keys) -> (keys.matches("\\D.*$"))).forEach((keys) ->
            {
                HashSet<String> RegexPointers = prefixModifiers.get(keys);
                //add new search regexs for each prefix
                RegexPointers.stream().forEach((regexPointer) ->
                {
                    HashSet<String> regexes = modelSearchRegex.get(regexPointer);
                    
                    String model =  getModelFromRegexPointer( regexPointer);
                    String post = model.replaceAll("^" + keys, "");
                    
                    String basicHyphenSearch = "(?s).*\\b"+keys+"-"+post+"\\b.*";
                    String basicWhiteSpaceSearch = "(?s).*\\b"+keys+"\\s+"+post+"\\b.*";
                    
                    regexes.add(basicHyphenSearch);
                    regexes.add(basicWhiteSpaceSearch);
                    
                    modelSearchRegex.put(regexPointer, regexes);
                });
            }); //only alpha
            suffixModifiers.keySet().stream().filter((keys) -> (keys.matches("\\D.*$"))).forEach((keys) ->
            {
                HashSet<String> RegexPointers = suffixModifiers.get(keys);
                //add new search reges for each suffix discovered
                RegexPointers.stream().forEach((regexPointer) ->
                {
                    HashSet<String> regexes = modelSearchRegex.get(regexPointer);
                    
                    String model =  getModelFromRegexPointer( regexPointer);
                    String pre = model.replaceAll(keys+"$","");
                    
                    String basicHyphenSearch = "(?s).*\\b"+pre+"-"+keys+"\\b.*";
                    String basicWhiteSpaceSearch = "(?s).*\\b"+pre+"\\s+"+keys+"\\b.*";
                    
                    regexes.add(basicHyphenSearch);
                    regexes.add(basicWhiteSpaceSearch);
                    
                    modelSearchRegex.put(regexPointer, regexes);
                });
            }); //only alpha prefixes allowed
        });



    }
    
    String getModelFromRegexPointer(String regexPointer)
    {
        return(regexPointer.split("@")[2]);
    }

    /**
     * In order for a product match we use this criteria
     *
     * a model in the title and manufacturer in either the title of the
     * manufacturer field
     *
     * OR
     *
     * a model and a family in the title
     * 
     * Once these are satisfied we then will do a deep search for specific model numbers
     *
     * this builds the output arrays as well as a no match array
     */
    private void match()
    {

        for (String listing : myCodeChallenge.getListingKeys().keySet())
        {
            JsonObject myobj = myCodeChallenge.getListingKeys().get(listing);
            String mfg = conditionMfgString(myobj.getString(CodeChallenge.PRODUCT_MANUFACTURER_KEY));
            String title = conditionListingString(myobj.getString(CodeChallenge.PRODUCT_LISTING_TITLE_KEY));
            String utitle = myobj.getString(CodeChallenge.PRODUCT_LISTING_TITLE_KEY);
            String productName = null;
            boolean matched = false;
            boolean mfgMatched = false;
            boolean familyMatched = false;

            String manufacturer = null;

            for (String mfgneedle : modelByMfgMap.keySet())
            {
                //we look for a mfg match in either manufacturer or the title
                if (mfg.contains(mfgneedle) || title.contains(mfgneedle))
                {
                    mfgMatched = true;
                    manufacturer = mfgneedle;
                    break;
                }

            }

            //check the alias map if we don't have a match yet
            if (!mfgMatched)
            {
                //first check the list of known aliases for a manufacturer match
                for (String aliasMfgNeedle : aliasMfgMap.keySet())
                {
                    if (mfg.contains(aliasMfgNeedle) || title.contains(aliasMfgNeedle))
                    {
                        mfgMatched = true;
                        manufacturer = aliasMfgNeedle;
                        break;
                    }
                }
            }

            String family = null;
            //attempt to find a family 
            for (String familyMfgNeedle : modelByProductFamily.keySet())
            {
                if (title.contains(familyMfgNeedle))
                {
                    familyMatched = true;
                    mfgMatched = true;   // by virtue of the family we know the MFG as well
                    family = familyMfgNeedle;
                    manufacturer = mfgByProductFamily.get(family);
                    break;
                }
            }

            //we do not bother to go further if we don't have a MFG match
            if (mfgMatched)
            {
                productName = matchModel(manufacturer, family, title);
            }

            if (productName != null)
            {
                if (matchedList.containsKey(productName))
                {
                    matchedList.get(productName).add(myobj);
                    numMatched++;
                } else
                {
                    ArrayList<JsonObject> no = new ArrayList<>();
                    no.add(myobj);
                    matchedList.put(productName, no);
                    numMatched++;
                }
            } else
            {
                unMatched.put(listing, myobj);
                numUnmatched++;
            }

        }

    }

    /**
     * When evaluating matches in model numbers we search all model numbers for a listing
     * given the manufacturer.  We use this to remove listings that have more than one model listed in the listing
     * typically these will be accessories that are suitable for different models, this removes most
     * accessory mismatches
     * 
     * @param firstMatch - String representing the first match that was made on the model number
     * @param nextMatch - String representing the last match that was made on the model number
     * @return - String - the preferred match string or null if the match should be discarded
     */
    String resolveDuplicateMatch(String firstMatch, String nextMatch)
    {
        //in this case the next Matching value is contained in the original match
        //this is a false match so return the first match
        
        if (firstMatch.contains(nextMatch))
        {
            return firstMatch;
        }

        //this is the same case as above except reverse
        if (nextMatch.contains(firstMatch))
        {
            return (nextMatch);
        }

        //another step we look at false positives as a result of matching numerics
        String alpha1 = firstMatch.replaceAll("[0123456789]", "");
        String alpha2 = nextMatch.replaceAll("[0123456789]", "");

        if (alpha1.length() == 0 && alpha2.length() == 0)
        {
            return null;
        }
        if (alpha1.length() == 0 && alpha2.length() != 0)
        {
            return nextMatch;
        }
        if (alpha1.length() != 0 && alpha2.length() == 0)
        {
            return firstMatch;
        }

        return null;

    }

    
    /**
     * This function searches all regexes that are in the modelSearchRegex private HashMap
     * TODO: precompile the regexes and use Pattern and Matcher for speed
     * 
     * @param mfgC - String the conditioned manufacturing code
     * @param familyC - String the conditioned family code (or "" for blank this is options
     * @param modelC - String - the conditioned model code to be searched
     * @param titleC - the conditioned listing string to search
     * @return boolean ture if a match is found 
     */
    private boolean regexMatchModel(String mfgC, String familyC, String modelC,String titleC)
    {
        String searchKey = mfgC +"@" + familyC + "@" + modelC;
        HashSet<String> modelRegex = modelSearchRegex.get(searchKey);
        if(modelRegex == null)
            return false;
        
        if (modelRegex.stream().anyMatch((reg) -> (titleC.matches(reg))))
        {
            return true;
        }
        
        return false;
    }
    
    /** This is a debugging set to look at what we didn't match when 
     we were clearly given the manufacturer and the Family code */
    HashSet<String> whyNoMatch = new HashSet<>();

    /**
     * This function matches all models for given manufacturer and or family type
     * 
     * @param mfgC - String the conditioned MAnufacturing code
     * @param familyC - String the conditioned Family code
     * @param titleC - String the conditioned title
     * @return - String the conditioned model code if found or null if no code is found
     */
    String matchModel(String mfgC, String familyC, String titleC)
    {
        String match = null;
        String rval = null;

        if (familyC != null)
        {

            //we know the family we use that to guide our model search
            HashMap<String, String> modelsToSearch = modelByProductFamily.get(familyC);
            for (String model : modelsToSearch.keySet())
            {
                if (regexMatchModel(mfgC,familyC,model,titleC))
                {
                    //we can't match multiple models
                    if (match != null)
                    {
                        match = resolveDuplicateMatch(match, model);
                        if (match == null)
                        {
                            return null;
                        }
                    } else
                    {

                        match = model;
                    }
                }
            }
            if (match != null)
            {
                rval = modelsToSearch.get(match);
            }
        } else
        {
            //we only know the MFG we use that to guid our model search
            HashMap<String, String> modelsToSearch = modelByMfgMap.get(mfgC);
            if (modelsToSearch != null)
            {

                for (String model : modelsToSearch.keySet())
                {
                    if (regexMatchModel(mfgC,"",model,titleC))
                    {
                        //we can't match multiple models
                        if (match != null)
                        {
                            match = resolveDuplicateMatch(match, model);
                            if (match == null)
                            {
                                return null;
                            }
                        } else
                        {
                            match = model;
                        }
                    }
                }
                if (match != null)
                {
                    rval = modelsToSearch.get(match);
                }

            }
        }

        if (rval != null)
        {
            return rval;
        }

 
        if (rval == null && familyC != null)
        {
            whyNoMatch.add(titleC);
        }

        return rval;

    }

    private String conditionListingString(String listingString)
    {
        if (listingString == null)
        {
            return null;
        }
        
        String rval = listingString.toUpperCase();

        return rval;
    }

    
    private String conditionMfgString(String preMfg)
    {
        if (preMfg == null)
        {
            return null;
        }

        return preMfg.toUpperCase().replaceAll("[^A-Za-z0-9\\.]", "");
    }
    private String conditionFamilyString(String preFamily)
    {
        if (preFamily == null)
        {
            return null;
        }
        
        return preFamily.toUpperCase().replaceAll("[^A-Za-z0-9\\.]", "");
    }

    private String conditionModelString(String preModel)
    {
        if (preModel == null)
        {
            return null;
        }

        return preModel.toUpperCase().replaceAll("[^A-Za-z0-9\\.]", "");
    }

    /**
     * Once we finish our matching we then match up the duplicate listing list and add
     * those that are duplicates to the final array list
     */
    private void matchDuplicateListings()
    {
 
        HashMap<String,ArrayList<JsonObject>> duplicates = myCodeChallenge.getDuplicateListings();
        
        matchedList.keySet().stream().map((matchKey) -> matchedList.get(matchKey)).map((matches) -> matches.listIterator()).forEach((iterator) ->
        {
            while(iterator.hasNext())
            {
                JsonObject listingMatch = iterator.next();
                String searchKey = listingMatch.getString(CodeChallenge.PRODUCT_LISTING_TITLE_KEY);
                if(duplicates.containsKey(searchKey))
                {
                    ArrayList<JsonObject> othermatches = duplicates.get(searchKey);
                    othermatches.stream().map((othermatch) ->
                    {
                        iterator.add(othermatch);
                        return othermatch;
                    }).forEach((_item) ->
                    {
                        numDuplicateMatches++;
                    });
                }
            }
        });
 
    }

    @Override
    public HashMap<String, ArrayList<JsonObject>> getResults()
    {
        return matchedList;
    }

    @Override
    public String getImplementationName()
    {
        return "SlackerTestMethod search engine implementation";
    }

    @Override
    public String getImplementationDescription()
    {
        return "A Rube Goldberg-esq machine that objectifies flat JSON data and programs \n regular expression arrays for search and destroy";
    }

}

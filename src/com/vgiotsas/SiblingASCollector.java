package com.vgiotsas;

import com.sun.istack.internal.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <h1>Sibling ASes Collector</h1>
 * The SiblingASCollector queries <a href="https://www.peeringdb.com/">PeringDB</a> to construct a list of sibling
 * Autonomous System Numbers (ASNs) by collecting the ASNs with the same organization ID.
 * @author Vasileios Giotsas
 * @version 1.0
 * @since 2018-01-07
 */

public class SiblingASCollector{
    private final String USER_AGENT = "Mozilla/5.0";

    /**
     * The main method that collects and outputs the PeeringDB data
     * @param args Unused
     * @throws Exception
     */
    public static void main(String[] args) throws Exception{
        SiblingASCollector parser = new SiblingASCollector();
        String pdb_url = "https://www.peeringdb.com/api/net";
        ArrayList<String> pdb_response = parser.sendGet(pdb_url);

        JSONObject pdb_data = parser.parseJson(String.join("", pdb_response));
        HashMap<Integer, ArrayList<Integer>> org2ASN = parser.parsePdbData(pdb_data);

        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        Date date = new Date();

        FileWriter writer = new FileWriter("SiblingASNs_"+dateFormat.format(date)+".txt");
        for(Integer orgId: org2ASN.keySet()) {
            if (org2ASN.get(orgId).size() > 1) {
                String ASNString = org2ASN.get(orgId).stream().map(Object::toString)
                        .collect(Collectors.joining(" "));

                writer.write(ASNString + "\n");
            }
        }
        writer.close();
    }

    /**
     * Issues an HTTP GET request and returns the body of the reply as list of lines
     * @param url The URL to which the GET request is sent
     * @return ArrayList<String> The list of lines in the response body
     * @throws Exception
     */
    private ArrayList<String> sendGet(String url) throws Exception {

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", USER_AGENT);

        //int responseCode = con.getResponseCode();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        // StringBuffer response = new StringBuffer();
        ArrayList<String> response = new ArrayList<>();
        while ((inputLine = in.readLine()) != null) {
            response.add(inputLine);
        }
        in.close();

        return response;
    }

    /**
     * Convert JSON string to JSONObject
     * @param jsonString The string that represents a JSON object
     * @return JSONObject The decoded JSON object
     */
    private JSONObject parseJson(String jsonString){
        JSONObject jsonObject = null;
        JSONParser parser = new JSONParser();
        try{
            Object obj = parser.parse(jsonString);
            jsonObject = (JSONObject) obj;
        }catch(ParseException pe){

            System.out.println("position: " + pe.getPosition());
            System.out.println(pe);
        }

        return jsonObject;
    }

    /**
     * Parse the PeeringDB JSON response to extract and return the ASN to Org ID mapping
     * @param pdbResponse The JSON response from the PeeringDB API
     * @return HashMap<Integer, ArrayList<Integer>> The mapping between organizations and ASNs under each organization
     */
    private HashMap<Integer, ArrayList<Integer>> parsePdbData(@NotNull JSONObject pdbResponse){
        HashMap<Integer, ArrayList<Integer>> org2ASN = new HashMap<>();
        JSONArray nets_array = (JSONArray) pdbResponse.get("data");
        if (nets_array != null){
            for (Object obj : nets_array) {
                if (obj instanceof JSONObject) {
                    JSONObject jsonObj = (JSONObject) obj;

                    Integer asn = (int) (long) jsonObj.get("asn");
                    Integer orgId = (int) (long) jsonObj.get("org_id");
                    if (!org2ASN.containsKey(orgId)) {
                        org2ASN.put(orgId, new ArrayList<Integer>());
                    }
                    org2ASN.get(orgId).add(asn);
                }
            }
        }

        return org2ASN;
    }
}
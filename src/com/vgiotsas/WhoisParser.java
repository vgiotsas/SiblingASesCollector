package com.vgiotsas;

import org.apache.commons.net.whois.WhoisClient;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

public class WhoisParser {

    private HashMap<String, ArrayList<String>> rirFields = new HashMap<>();

    private HashMap<String, TreeMap<Integer, String>> orderedRirFields = new HashMap<>();

    public WhoisParser(){
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("input/rirFields.txt"));
            String line = reader.readLine();
            while (line != null) {
                String[] lf = line.split(",");
                if (lf.length == 3 && !lf[0].startsWith("#")){
                    if (!rirFields.containsKey(lf[0])){
                        rirFields.put(lf[0], new ArrayList<>());
                        orderedRirFields.put(lf[0], new TreeMap<>());
                    }
                    rirFields.get(lf[0]).add(lf[1]);
                    orderedRirFields.get(lf[0]).put(Integer.parseInt(lf[2]), lf[1]);
                }
                // read next line
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String queryWhois(String query, String hostName) {

        StringBuilder result = new StringBuilder();

        WhoisClient whois = new WhoisClient();
        try {
            whois.connect(hostName);
            String whoisData1 = whois.query(query);
            result.append(whoisData1);
            whois.disconnect();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result.toString();

    }

    public HashMap<Integer, String> readWhoisAutNum(String autNumDump) throws IOException {
        HashMap<Integer, String> asnOrg = new HashMap<>();
        GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(autNumDump));
        BufferedReader br = new BufferedReader(new InputStreamReader(gzip));
        int asn = 0;
        String line;
        while ((line = br.readLine()) != null)
        {
            String [] lf = line.split(":");
            if (lf[0].startsWith("aut-num")){
                asn = Integer.parseInt(lf[1].trim().substring(2));
            }
            else if(lf[0].startsWith("org") && asn > 0){
                asnOrg.put(asn, lf[1].trim());
                asn = 0;
            }
        }

        return asnOrg;
    }

    public HashMap<String, ArrayList<String>> parseWhois(String whoisText, String rirName) throws IOException {
        // Flag to skip whois sections related to the RIR
        boolean parseField = false;
        HashMap<String, ArrayList<String>> orgIdentifiers = new HashMap<>();
        if (rirFields.containsKey(rirName)) {
            String line;
            BufferedReader bufReader = new BufferedReader(new StringReader(whoisText));
            while( (line=bufReader.readLine()) != null )
            {
                String [] lf = line.trim().split(":");
                String rirField = lf[0];
                if (rirField.equals("aut-num") || rirField.equals("ASNumber"))
                    parseField = true;

                if (lf.length > 1 && rirFields.get(rirName).contains(rirField) && parseField){
                    if (!orgIdentifiers.containsKey(rirField)){
                        orgIdentifiers.put(rirField, new ArrayList<>());
                    }
                    orgIdentifiers.get(rirField).add(lf[1].trim());
                }
            }
        }

        return orgIdentifiers;
    }

    public HashMap<String, TreeMap<Integer, String>> getOrderedRirFields() {
        return orderedRirFields;
    }
}

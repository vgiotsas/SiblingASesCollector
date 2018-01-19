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
    //
    private Map<Integer, List<Integer>> pdbSiblings = new HashMap<>();
    private Map<Integer, List<Integer>> v6LaunchSiblings = new HashMap<>();
    private Map<String, List<Integer>> opqIDSiblings = new HashMap<>();

    private Map<Integer, String> rirASNOpqIds = new HashMap<>();
    /**
     * The main method that collects and outputs the PeeringDB data
     * @param args Unused
     * @throws Exception
     */
    public static void main(String[] args) throws Exception{
        SiblingASCollector parser = new SiblingASCollector();
        String pdbUrl = "https://www.peeringdb.com/api/net";
        String v6DayUrl = "http://worldipv6launch.appspot.com/asns.txt";
        ArrayList<String> pdbResponse = parser.sendGet(pdbUrl);
        ArrayList<String> v6dayResponse = parser.sendGet(v6DayUrl);

        JSONObject pdbData = parser.parseJson(String.join("", pdbResponse));
        parser.pdbSiblings = parser.parsePdbData(pdbData);
        parser.v6LaunchSiblings = parser.parseV6DayData(v6dayResponse);
        parser.getASNDelegations();
        parser.mergeSiblings();

        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        Date date = new Date();

        FileWriter writer = new FileWriter("SiblingASNs_"+dateFormat.format(date)+".txt");
        for(Integer orgId: parser.pdbSiblings.keySet()) {
            if (parser.pdbSiblings.get(orgId).size() > 1) {
                Set<Integer> siblingsSet = new HashSet<>(parser.pdbSiblings.get(orgId));
                String ASNString = siblingsSet.stream().map(Object::toString)
                        .collect(Collectors.joining(" "));

                writer.write(ASNString + "\n");
            }
        }

        Set<Integer> printedASNs = new HashSet<>();
        for(Integer key: parser.v6LaunchSiblings.keySet()){
            if (!printedASNs.contains(key)) {
                StringBuilder ASNString = new StringBuilder();
                for (Integer asn: parser.v6LaunchSiblings.get(key)){
                    printedASNs.add(asn);
                    if (ASNString.length() == 0){
                        ASNString.append(asn);
                    }
                    else {
                        ASNString.append(" ").append(asn);
                    }
                }
                writer.write(ASNString.toString() + "\n");
            }
        }

        for (String key: parser.opqIDSiblings.keySet()){
            Set<Integer> siblingsSet = new HashSet<>(parser.opqIDSiblings.get(key));
            String ASNString = siblingsSet.stream().map(Object::toString)
                    .collect(Collectors.joining(" "));
            writer.write(ASNString + "\n");
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
     * Parses the PeeringDB JSON response to extract and return the ASN to Org ID mapping
     * @param pdbResponse The JSON response from the PeeringDB API
     * @return HashMap<Integer, ArrayList<Integer>> The mapping between organizations and ASNs under each organization
     */
    private HashMap<Integer, List<Integer>> parsePdbData(@NotNull JSONObject pdbResponse){
        HashMap<Integer, List<Integer>> org2ASN = new HashMap<>();
        JSONArray nets_array = (JSONArray) pdbResponse.get("data");
        if (nets_array != null){
            for (Object obj : nets_array) {
                if (obj instanceof JSONObject) {
                    JSONObject jsonObj = (JSONObject) obj;

                    Integer asn = (int) (long) jsonObj.get("asn");
                    Integer orgId = (int) (long) jsonObj.get("org_id");
                    //String
                    if (!org2ASN.containsKey(orgId)) {
                        org2ASN.put(orgId, new ArrayList<>());
                    }
                    org2ASN.get(orgId).add(asn);
                }
            }
        }

        return org2ASN;
    }

    /**
     * Parses the list of ASNs from the World IPv6 Launch site to extract self-reported sibling ASNs
     * @param lines The lines obtained by querying the World IPv6 launch page of participating ASNs
     */
    private HashMap<Integer, List<Integer>> parseV6DayData(@NotNull ArrayList<String> lines){
        HashMap<Integer, List<Integer>> asnSiblings = new HashMap<>();
        for (String line: lines){
            String[] lf = line.split("\\|")[0].split(",");
            if (lf.length > 1){
                for (String aLf : lf) {
                    Integer key = Integer.parseInt(aLf);
                    List<Integer> siblings = Arrays.stream(lf).map(Integer::valueOf).collect(Collectors.toList());
                    asnSiblings.put(key, siblings);
                }
            }
        }

        return asnSiblings;
    }

    /**
     * Takes the siblings collected by each different data source and merges them in a single dataset
     */
    private void mergeSiblings(){
        for (Integer orgId: pdbSiblings.keySet()){
            List<Integer> commonSiblings = new LinkedList<>();
            for (Integer asn: pdbSiblings.get(orgId)) {
                if (v6LaunchSiblings.containsKey(asn)) {
                    commonSiblings = v6LaunchSiblings.get(asn);
                    break;
                }
            }

            // if the two sets of sibling ASNs contain a common ASN, merge them
            if (commonSiblings.size() > 0) {
                for (Integer asn : commonSiblings) {
                    // Delete the siblings from the v6Lanuch dataset to avoid duplicates
                    v6LaunchSiblings.remove(asn);
                    // Add the ASN to the PeeringDB dataset
                    pdbSiblings.get(orgId).add(asn);
                }
            }

            // Merge PDB siblings with those inferred from the Opaque IDs of extended RIRs
            List<Integer> intersection_pdb = new ArrayList<>(pdbSiblings.get(orgId));
            intersection_pdb.retainAll(rirASNOpqIds.keySet());
            if (intersection_pdb.size() > 0){
                for (Integer common_asn: intersection_pdb){
                    String opqId = rirASNOpqIds.get(common_asn);
                    try{
                        pdbSiblings.get(orgId).addAll(opqIDSiblings.get(opqId));
                    }catch (java.lang.NullPointerException ex){
                        continue;
                    }
                    System.out.println(opqId);
                    opqIDSiblings.remove(opqId);
                }
            }
        }

        // Merge the siblings inferred from the Opaque IDs of extended RIRs with those from the IPv6 Launch day
        for (String opqId: opqIDSiblings.keySet()){
            List<Integer> intersection_v6launch = new ArrayList<>(opqIDSiblings.get(opqId));
            intersection_v6launch.retainAll(v6LaunchSiblings.keySet());
            if (intersection_v6launch.size() > 0){
                Integer common_asn = intersection_v6launch.get(0);
                opqIDSiblings.get(opqId).addAll(v6LaunchSiblings.get(common_asn));
                v6LaunchSiblings.keySet().removeAll(v6LaunchSiblings.get(common_asn));
            }
        }
    }

    /***
     * Parses the extended RIR delegation files for ASN assignments to organizations
     * with the same Opaque ID.
     * @throws Exception
     */
    private void getASNDelegations() throws Exception {
        String[] rirURLs = {"http://ftp.ripe.net/pub/stats/ripencc/delegated-ripencc-extended-latest",
                            "http://ftp.ripe.net/pub/stats/lacnic/delegated-lacnic-extended-latest",
                            "http://ftp.ripe.net/pub/stats/arin/delegated-arin-extended-latest",
                            "http://ftp.ripe.net/pub/stats/apnic/delegated-apnic-extended-latest",
                            "http://ftp.ripe.net/pub/stats/afrinic/delegated-afrinic-extended-latest"};
        for (String url: rirURLs){
            ArrayList<String> delegationsResponse = this.sendGet(url);
            for (String line: delegationsResponse){
                String[] lf = line.split("\\|");
                if (lf.length > 6 && lf[2].equals("asn")){
                    String opqID = lf[lf.length - 1];
                    if (!opqIDSiblings.containsKey(opqID)){
                        opqIDSiblings.put(opqID, new ArrayList<>());
                    }
                    opqIDSiblings.get(opqID).add(Integer.parseInt(lf[3]));
                }
            }
        }

        // Remove Opaque IDs with only one ASN since they don't have have sibling ASNs
        //opqIDSiblings.entrySet().removeIf(entry -> entry.getValue().size() < 2);
        Iterator<Map.Entry<String,List<Integer>>> iter = opqIDSiblings.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String,List<Integer>> entry = iter.next();
            if (entry.getValue().size() < 2){
                iter.remove();
            }else{
                for (Integer asn: entry.getValue()) {
                    rirASNOpqIds.put(asn, entry.getKey());
                }
            }
        }
    }
}
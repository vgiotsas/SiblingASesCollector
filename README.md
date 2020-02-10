The current best practice to infer sibling ASes uses organization names provided by RPSL objects.

This approach can miss some siblings, for instance when an organization has registered networks in many diffeerent
RIRs. For instance, consider the case of Limelight networks:

  ```
  LLNW-ARIN|20160212|Limelight Networks, Inc.|US|ARIN
  @family-21148||Limelight Networks India|IN|APNIC
  aut-45396-APNIC|20090630|Limelight Networks Korea Ltd|KR|APNIC
  ORG-LN1-AFRINIC|20130318|Limelight Networks|ZA|AFRINIC
  ORG-LNI1-RIPE||Limelight Networks, INC.|BH|RIPE
  ```

  22822|20160212|LLNW|LLNW-ARIN|ARIN
  23059|20160212|LLNW-LAX|LLNW-ARIN|ARIN
  23135|20160212|LLNW-SJC|LLNW-ARIN|ARIN
  23164|20160212|LLNW-SFO|LLNW-ARIN|ARIN
  25804|20160212|LLNW-IL-TLV|LLNW-ARIN|ARIN
  26506|20160212|LLNW-SEA|LLNW-ARIN|ARIN
  27191|20120224|LLNW-IAD|LLNW-ARIN|ARIN

  38621|20150319|LLNW-DEL|@family-21148|APNIC
  38622|20150319|LLNW-AU|@family-21148|APNIC
  55429|20150319|LLNW-IN|@family-21148|APNIC

  45396|20090630|LLNW-AS-KR|@aut-45396-APNIC|APNIC
  45396|20090630|LLNW-AS-KR|@aut-45396-APNIC|APNIC

  37277|20130318|LIMENET|ORG-LN1-AFRINIC|AFRINIC

  12411||LLNW-GCC|ORG-LNI1-RIPE|RIPE
  60261||LLNW-AE|ORG-LNI1-RIPE|RIPE
  
  ## Compilation Instructions
  
  Below we provide installation instructions tested for Ubuntu 16.04.2 (xenial) withn Open KDK 1.8
  
  1. Install java JDK
  
  `sudo apt install default-jdk`
  
  2. Set the `JAVA_HOME` variable
  
  `export JAVA_HOME=/usr/lib/jvm/default-java/`
  
  3. [Install maven](https://maven.apache.org/install.html)
  
  `sudo apt install maven`
  
  4. Build and package the sources
  
  Navigate to the root directory (same directory that contains the `pom.xml` file and run:
  
  `mvn compile`
  `mvn package`

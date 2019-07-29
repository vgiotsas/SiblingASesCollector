import sys
from collections import defaultdict

whois_siblings_file = sys.argv[1]  # C:\Users\Vasileios\Documents\Gitlab\kepler\data\as2org\20170401.as-org2info.txt
gt_siblings_file = sys.argv[2]  # C:\Users\Vasileios\IdeaProjects\SiblingASesCollector\output\SiblingASNs_20180107.txt

whois_asn_org = {}
whois_org_asn = defaultdict(set)
orgs_with_siblings = set()
with open(whois_siblings_file, "r") as fin:
    start_parsing = False
    for line in fin:
        # # format:aut|changed|aut_name|org_id|source
        if line.startswith("# format:aut"):
            start_parsing = True
        if not line.startswith("#") and start_parsing is True:
            lf = line.strip().split("|")
            if len(lf) == 5:
                whois_asn_org[lf[0]] = lf[3]
                whois_org_asn[lf[3]].add(lf[0])
                if len(whois_org_asn[lf[3]]) > 1:
                    orgs_with_siblings.add(lf[3])

missing_siblings = 0
total_siblings = 0
with open(gt_siblings_file) as fin:
    for line in fin:
        total_siblings += 1
        try:
            gt_siblings = set(line.strip().split("|")[1].split())
            for sib in gt_siblings:
                if sib in whois_asn_org:
                    whois_siblings = whois_org_asn[whois_asn_org[sib]]
                    sib_diff = set(gt_siblings) - whois_siblings
                    if len(sib_diff) > 0:
                        missing_siblings += 1
                        break
        except IndexError:
            continue

print missing_siblings, total_siblings, len(orgs_with_siblings)

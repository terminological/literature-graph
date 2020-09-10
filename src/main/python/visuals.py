# -*- coding: utf-8 -*-
"""
Spyder Editor

This is a temporary script file.
"""

#%%
from neo4j import GraphDatabase

uri = "bolt://localhost:7687"
driver = GraphDatabase.driver(uri, auth=("neo4j", "password"))

#%%

duplicateTitlesQry = """
MATCH (n:Article), (m:Article) 
WHERE n.title <> '' 
AND n.title = m.title 
AND n<>m 
RETURN n.title,n.pmid,n.doi,m.title,m.pmid,m.doi
"""
   
with driver.session() as session:
    result = session.run(duplicateTitlesQry)
    result.detach()

#%%
for item in result:
    print(item["n.pmid"])
# result here is an iterable of some sort and consumed.


#%%
"""

# matching titles - sometimes this is not a duplicate
match (n:Article), (m:Article) WHERE n.title <> "" AND n.title = m.title AND n<>m RETURN n.title,n.pmid,n.doi,m.title,m.pmid,m.doi


# matching dois case insensitive - this is always a duplicate
match (n:Article), (m:Article) WHERE toUpper(n.doi) = toUpper(m.doi) AND n<>m RETURN n.title,n.pmid,n.doi,m.title,m.pmid,m.doi

# articles with no references
MATCH (source:Article) WHERE NOT (source)-[:HAS_REFERENCE]-()
RETURN source

CALL algo.pageRank.stream('Article', 'HAS_REFERENCE', {iterations:20, dampingFactor:0.85})
YIELD nodeId, score
RETURN 
nodeId,
algo.getNodeById(nodeId).pmid AS pmid,
algo.getNodeById(nodeId).doi AS doi, 
algo.getNodeById(nodeId).title AS page,
duration.inDays(algo.getNodeById(nodeId).date,date()) AS age,
score
ORDER BY score DESC


# core articles (within broader search) which have no references. These probably need to be looked up as pdfs. Will need to update client to use xref citation matcher (https://www.crossref.org/labs/resolving-citations-we-dont-need-no-stinkin-parser/) and cermine

MATCH (source:Expand) WHERE NOT (source)-[:HAS_REFERENCE]->()
RETURN source



# time weighted citations



# insert coauthor relationships

MATCH (n:Author) <-[:HAS_AUTHOR]- (m:Article) -[:HAS_AUTHOR]-> (o:Author) CREATE (n)-[r:COAUTHOR]->(o) RETURN count(r)




CALL algo.louvain.stream('Author', 'CO_AUTHOR', {}) YIELD nodeId, community RETURN algo.getNodeById(nodeId).identifier AS user,algo.getNodeById(nodeId).lastName AS lastName,algo.getNodeById(nodeId).affiliations AS affiliations, community ORDER BY community



CALL algo.closeness.harmonic.stream('Author', 'CO_AUTHOR', {}) YIELD nodeId, centrality RETURN nodeId, algo.getNodeById(nodeId).identifier AS user,algo.getNodeById(nodeId).lastName AS lastName,algo.getNodeById(nodeId).affiliations AS affiliations, centrality ORDER BY centrality desc


# MeshCode relationships - needs filter for qualifiecation.

MATCH (n:MeshCode) <-[:HAS_MESH]- (m:Article) -[:HAS_MESH]-> (o:MeshCode) CREATE (n)-[r:CO_OCCUR]->(o) RETURN count(r)
MATCH (n:MeshCode) -[r:COOCCUR]-> (m:MeshCode) RETURN n.term, count(r) as freq, m.term ORDER BY freq DESC







# create author relationships

# orchid matches
#combine clause here causes error

MATCH (n:Author), (o:Author) WHERE n.orcid = o.orcid AND n<>o CALL apoc.refactor.mergeNodes([n,o],{mergeRels:true}) YIELD node RETURN *

# CALL apoc.periodic.iterate("MATCH (n:Author), (o:Author) WHERE n.lastName = o.lastName AND n.firstName = o.firstName AND n<>o RETURN [n,o] as p", "CALL apoc.refactor.mergeNodes(p,{mergeRels:true}) YIELD node RETURN count(node)", {batchSize:1, retries:0})

# Exact fullname match
MATCH (n:Author), (o:Author) WHERE n.lastName = o.lastName AND n.firstName = o.firstName AND n<>o CALL apoc.refactor.mergeNodes([n,o],{mergeRels:true}) YIELD node RETURN *

# Short name match plus similar affiliation
MATCH (n:Author) -[:HAS_AFFILIATION]-> () -[:SIMILAR_TO]-> () <-[:HAS_AFFILIATION]- (o:Author) WHERE NOT (n)-[:SAME_AS]-(o) AND n.authorLabel = o.authorLabel AND n<>o CALL apoc.refactor.mergeNodes([n,o],{mergeRels:true}) YIELD node RETURN *

# Create coauthor network
MATCH (n:Author) <-[:HAS_AUTHOR]- (m:Article) -[:HAS_AUTHOR]-> (o:Author) WHERE NOT (n)-[:SAME_AS]-(o) CREATE (n)-[r:CO_AUTHOR]->(o)

# Merge co-authors with same name
MATCH (n:Author) -[:CO_AUTHOR]-> () <-[:CO_AUTHOR]- (o:Author) WHERE NOT (n)-[:SAME_AS]-(o) AND n.authorLabel = o.authorLabel AND n<>o CALL apoc.refactor.mergeNodes([n,o],{mergeRels:true}) YIELD node RETURN *

# Delete any that have been merged together
MATCH (n:Author) -[r:CO_AUTHOR]-> (n:Author) DELETE r

# Tidy up null authors
MATCH (n:Author)-[r]-() WHERE n.lastName IS NULL DELETE r,n


# AUTHORS
# Graph connectedness
CALL algo.unionFind.stream('Author', 'CO_AUTHOR', {})
YIELD nodeId,setId
RETURN setId,count(*) as size_of_component
ORDER BY size_of_component DESC
# generally shows that the graph is disconnected

# Community
CALL algo.louvain.stream('Author', 'CO_AUTHOR', {direction:'out'}) 
YIELD nodeId, community 
MATCH (a:Author)-[:HAS_AFFILIATION]->(b:Affiliation) WHERE id(a) = nodeId
RETURN a.authorLabel, a.lastName, collect(b.organisationName) AS affiliations, community 
ORDER BY community

# centrality
# betweenness
CALL algo.betweenness.stream('Author','CO_AUTHOR',{direction:'out'})
YIELD nodeId, centrality
MATCH (a:Author)-[:HAS_AFFILIATION]->(b:Affiliation) WHERE id(a) = nodeId
RETURN a.authorLabel, a.lastName, collect(b.organisationName) AS affiliations,centrality
ORDER BY centrality DESC;

# closeness
CALL algo.closeness.harmonic.stream('Author','CO_AUTHOR',{direction:'out'})
YIELD nodeId, centrality
MATCH (a:Author)-[:HAS_AFFILIATION]->(b:Affiliation) WHERE id(a) = nodeId
RETURN a.authorLabel, a.lastName, collect(b.organisationName) AS affiliations,centrality
ORDER BY centrality DESC;

# The CO_AUTHOR graph is disconnected. The CITES graph however is not
# create the graph
MATCH (n:Author) <-[:HAS_AUTHOR]- () -[:HAS_REFERENCE]-> () -[:HAS_AUTHOR]-> (o:Author) CREATE (n)-[r:CITES]->(o)

# closeness algorithm
CALL algo.closeness.stream('Author','CITES',{direction:'out'})
YIELD nodeId, centrality
MATCH (a:Author)-[:HAS_AFFILIATION]->(b:Affiliation) WHERE id(a) = nodeId
RETURN a.authorLabel, a.lastName, collect(b.organisationName) AS affiliations,centrality
ORDER BY centrality DESC;


# PAPERS
# PageRank
CALL algo.pageRank.stream('Article', 'HAS_REFERENCE', {iterations:20, dampingFactor:0.85})
YIELD nodeId, score
MATCH (a:Article) WHERE id(a) = nodeId
RETURN nodeId,
a.pmid AS pmid,
a.doi AS doi,
duration.inDays(a.date,date()).days AS age,
a.title,
score,
score*365/(duration.inDays(a.date,date()).days) AS timeWeightedScore
ORDER BY score DESC


# MESH terms
MATCH (:MeshCode)-[u:CO_OCCUR]->(:MeshCode) DELETE u

# Create single CO-OCCUR relationships with count - quite slow - 370 secs
MATCH (n:MeshCode) <-[:HAS_MESH]- (m:Article) -[:HAS_MESH]-> (o:MeshCode)
WHERE n<>o
WITH n, o, count(distinct(m)) AS cooccurrences
MATCH (n),(o)
CREATE (n)-[r:CO_OCCUR]->(o)
SET r.cooccurrences = cooccurrences
RETURN count(r)

# Check connectedness
CALL algo.unionFind.stream('MeshCode', 'CO_OCCUR', {})
YIELD nodeId,setId
RETURN setId,count(*) as size_of_component
ORDER BY size_of_component DESC
# Connected graph

# set up counts
MATCH (n:Article)-[u:HAS_MESH]->(m:MeshCode) WITH m,count(n) as total MATCH (m:MeshCode) SET m.occurrences=total

# create pmi on relationship
MATCH (x:MeshCode) WITH sum(x.occurrences) as total
MATCH (m:MeshCode)-[r:CO_OCCUR]->(n:MeshCode) 
SET 
r.pmi = log( (toFloat(r.cooccurrences)*total) / (m.occurrences*n.occurrences) ),
r.probability = toFloat(r.cooccurrences)/total,
r.npmi = - log( (toFloat(r.cooccurrences)*total) / (m.occurrences*n.occurrences) ) / log ( toFloat(r.cooccurrences)/total ),
r.total = total

# export for further visualisation
MATCH (m:MeshCode)-[r:CO_OCCUR]->(n:MeshCode) 
RETURN n.term, m.term, r.pmi, r.npmi, r.cooccurrences, m.occurrences, n.occurrences, r.total
ORDER BY r.cooccurrences DESC

# Mutual information
MATCH (m:MeshCode)-[r:CO_OCCUR]->(n:MeshCode) RETURN sum(r.pmi*r.probability) as mutualInformation

#
MATCH (n:Keyword), (o:Keyword) WHERE lower(n.term) = lower(o.term) AND n<>o CALL apoc.refactor.mergeNodes([n,o],{mergeRels:true}) YIELD node RETURN *


# Keywords
MATCH (:Keyword)-[u:CO_OCCUR]->(:Keyword) DELETE u

# Create single CO-OCCUR relationships with count - quite slow - 370 secs
MATCH (n:Keyword) <-[:HAS_KEYWORD]- (m:Article) -[:HAS_KEYWORD]-> (o:Keyword)
WHERE n<>o
WITH n, o, count(distinct(m)) AS cooccurrences
MATCH (n),(o)
CREATE (n)-[r:CO_OCCUR]->(o)
SET r.cooccurrences = cooccurrences
RETURN count(r)

# Check connectedness
CALL algo.unionFind.stream('Keyword', 'CO_OCCUR', {})
YIELD nodeId,setId
RETURN setId,count(*) as size_of_component
ORDER BY size_of_component DESC
# Connected graph

# Community
CALL algo.louvain.stream('Keyword', 'HAS_KEYWORD', {direction:'out'}) 
YIELD nodeId, community 
MATCH (a:Author)-[:HAS_AFFILIATION]->(b:Affiliation) WHERE id(a) = nodeId
RETURN a.authorLabel, a.lastName, collect(b.organisationName) AS affiliations, community 
ORDER BY community

# set up counts
MATCH (n:Article)-[u:HAS_KEYWORD]->(m:Keyword) WITH m,count(n) as total MATCH (m) SET m.occurrences=total

# create pmi on relationship
MATCH (x:Keyword) WITH sum(x.occurrences) as total
MATCH (m:Keyword)-[r:CO_OCCUR]->(n:Keyword) 
SET 
r.pmi = log( (toFloat(r.cooccurrences)*total) / (m.occurrences*n.occurrences) ),
r.probability = toFloat(r.cooccurrences)/total,
r.npmi = - log( (toFloat(r.cooccurrences)*total) / (m.occurrences*n.occurrences) ) / log ( toFloat(r.cooccurrences)/total ),
r.total = total

# export for further visualisation
MATCH (m:Keyword)-[r:CO_OCCUR]->(n:Keyword) 
RETURN n.term, m.term, r.pmi, r.npmi, r.cooccurrences, m.occurrences, n.occurrences, r.total
ORDER BY r.cooccurrences DESC

# Mutual information
MATCH (m:Keyword)-[r:CO_OCCUR]->(n:Keyword) RETURN sum(r.pmi*r.probability) as mutualInformation



"""

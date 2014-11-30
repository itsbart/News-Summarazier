package hello;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.JSONPObject;
import hello.summarizer.Summarizer;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by daniel.bremiller on 11/25/2014.
 */
public class SolrAdapter
{

private final String SOLRURL = "http://ec2-54-148-45-194.us-west-2.compute.amazonaws.com:8983/solr/collection1/clustering?wt=json&indent=true&df=text&rows=100&dl=id,title,date,content,url,score	";
private HashMap<String,ObjectNode> requestCache = new HashMap<String, ObjectNode>();

	public SolrAdapter()
	{}


	public String solrQuery(String query, boolean force)
	{
		//fetch clustered solr results
		ObjectNode results = fetchResults(query,true);

		//run through summarizer. summarize the collection in a few sentences
		generateClusterSummaries(results);

		//run through NLP engine  openNLP? Person/Places/Things

		//return to UI append json

		return results.toString();
	}

	public String getQueryCluster(String query, String cluster)
	{
		//get clustered solr results
		ObjectNode results = fetchResults(query,false).deepCopy();
		List<String> clusterIds = getClusterIds(results, cluster);
		JsonNode docs = getDocs(clusterIds, results);

		return docs.toString();
	}

	private List<String> getClusterIds(ObjectNode root, String cluster)
{
	ArrayList<String> strings = new ArrayList<String>();

	//run through summarizer apache mahout? summarize the collection in a few sentences
	JsonNode clusters = root.get("clusters");
	for (JsonNode next : clusters)
	{

		for(JsonNode label: next.get("labels"))
		{
			if(label.asText().compareTo(cluster)==0)
			{
				//found the right cluster. Get ids.
				for(JsonNode docId : next.get("docs"))
					strings.add(docId.asText());
				return strings;
			}
		}
	}

	return strings;
}

	private void generateClusterSummaries(ObjectNode root)
	{
		ArrayList<String> strings = new ArrayList<String>();

		//run through summarizer apache mahout? summarize the collection in a few sentences
		JsonNode clusters = root.get("clusters");
		for (JsonNode next : clusters)
		{

			for(JsonNode label: next.get("labels"))
			{
				//get docIds so i can go get the content
					for(JsonNode docId : next.get("docs"))
						strings.add(docId.asText());
			}

			String content = getDocContent(strings, root);
			String summary = Summarizer.summarize(content);
			((ObjectNode) next).put("summary", summary);
		}
	}

	private String getDocContent(List<String> strings, ObjectNode root)
	{
		JsonNode docs = root.path("response").path("docs");
		StringBuilder builder = new StringBuilder();
		for(JsonNode doc : docs)
		{
			for(String id : strings)
			{
				if(id.compareTo(doc.get("id").asText())==0)
				{
					for(JsonNode content : doc.get("content"))
						builder.append(content.asText());
				}
			}
		}
		return builder.toString();
	}

	private JsonNode getDocs(List<String> strings, ObjectNode root)
	{
		ArrayNode aryNode = new ArrayNode(JsonNodeFactory.instance);

		JsonNode docs = root.path("response").path("docs");
		for(JsonNode doc : docs)
		{
			if(strings.contains(doc.get("id").asText()))
				aryNode.add(doc.deepCopy());
		}
		return aryNode;
	}

	/***
	 *
	 * Private method to fetch results from cache or solr
	 *
	 *
	 */
	private ObjectNode fetchResults(String query, boolean force)
	{
		ObjectNode results;
		// Check cache if its there
		if(!requestCache.containsKey(query)|| force)
		{
			results = fetchSolrResults(query);
			requestCache.clear();
			requestCache.put(query,results);
		}
		else
		{
			results = requestCache.get(query);
		}
		return results;
	}

	private ObjectNode fetchSolrResults(String query)
	{
		RestTemplate restTemplate = new RestTemplate();
		String json = restTemplate.getForObject(SOLRURL+"&q="+query, String.class);
		ObjectNode page = null;
		try
		{
			page = (ObjectNode) new ObjectMapper().readTree(json);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		return page;

	}


}

package hello;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.JSONPObject;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

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

		//run through summarizer apache mahout? summarize the collection in a few sentences
		JsonNode clusters = results.get("clusters");
		if(clusters.isArray())
		{
			Iterator<JsonNode> iterator = ((ArrayNode) clusters).iterator();
			while(iterator.hasNext())
			{
				JsonNode next = iterator.next();

				//TODO get document ids, then get the content from the response, then summarize
				((ObjectNode)next).put("summary", "This is a Test");
			}
		}
		//run through NLP engine  openNLP? Person/Places/Things

		//return to UI append json

		return results.toString();
	}

	public String getQueryCluster(String query, String cluster)
	{
		//get clustered solr results
		ObjectNode results = fetchResults(query,false);

		return results.toString();
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

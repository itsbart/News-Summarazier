package hello;

import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingController {

    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();
	private SolrAdapter manager = new SolrAdapter();

    @RequestMapping("/greeting")
    public Greeting greeting(@RequestParam(value="name", defaultValue="World") String name) {
        return new Greeting(counter.incrementAndGet(),
                            String.format(template, name));
    }

	@RequestMapping(value="/query", headers="Accept=application/json")
	public String executeQuery(@RequestParam(value="query") String query) {

		//execute solr query
		String results =  manager.solrQuery(query, false);

		return results;
	}

	@RequestMapping(value="/query/cluster", headers="Accept=application/json")
	public String executeQuery(@RequestParam(value="query") String query, @RequestParam(value="cluster") String cluster) {

		//Get solr query results for a cluster
		String results = manager.getQueryCluster(query, cluster);

		return results;
	}

	@RequestMapping(value="/query/datecluster", headers="Accept=application/json")
	public String executeDateQuery(@RequestParam(value="query") String query) {

		//Get solr query results for a cluster
		String results = manager.getDateClusters(query, false);

		return results;
	}


	@RequestMapping(value="/feeds", headers="Accept=application/json")
	public String getFeeds() {

		//Get solr query results for a cluster
		String results = manager.getFeeds();

		return results;
	}

	@RequestMapping(value="/feeds/{id}", headers="Accept=application/json")
	public String getFeed(@PathVariable(value="id") String id) {

		//Get solr query results for a cluster
		String results = manager.getFeed(id);

		return results;
	}

	@RequestMapping(value="/feeds/{id}/create", headers="Accept=application/json")
	public String addFeed(@PathVariable(value="id") String id) {

		//Get solr query results for a cluster
		manager.addFeed(id);

		return "success";
	}




}

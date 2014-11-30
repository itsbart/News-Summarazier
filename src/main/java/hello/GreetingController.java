package hello;

import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.JsonNode;
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





}

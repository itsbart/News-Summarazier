package hello;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.commons.codec.binary.Base64;

import static com.fasterxml.jackson.databind.ObjectMapper.*;


/**
 * 
 * @author Bartek
 * Stores web search results, saves links to text file
 *
 */


public class NewsCrawl {

	/* !!! STATIC FIELDS - SET FOR PROPER EXECUTION !!! */
	private  String NutchDir;
	private  String NutchSeedDir;
	private  String APIkey;
	private  int pageCap;
	private ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<String>();
	private HashSet<String> pastTerms = new HashSet<String>();
	private int state =0;  //idle, running

	private static final Logger log = Logger.getLogger("NewsCrawler");
	
	public NewsCrawl(){
		
		try {
			//load configuration file
			this.loadConfig();

			init();
		} catch (IOException e) {
			log.log(Level.SEVERE, " Nutch has failed to initialize ");
		}
		
	}

	private void init()
	{
		TimerTask task = new TimerTask(){

			@Override
			public void run()
			{

				if(state==0 && !queue.isEmpty())
				{
					state=1;
					String term = queue.peek();
					try {
						log.log(Level.INFO, "Starting crawl for term = " + term );

						crawlArticles(term, pageCap);
					} catch (IOException e) {
						e.printStackTrace();
					}
					state=0;
				}else
				{
					log.log(Level.INFO, "skipping crawling because either queue is empty or crawler is working. state = " + state + " queue.size = " + queue.size() );
				}
			}
		};
		Timer timer = new Timer(true);
		timer.scheduleAtFixedRate(task, 0, 10 * 1000);

	}


	/* Main method to get links using BING API*/
	public  void crawlArticles(String topic, int quantity)
			throws MalformedURLException, IOException {
	     
    	//encode URL
		topic = topic.trim();
		topic = topic.replaceAll(" ", "%20");
		
		String urlStr = "https://api.datamarket.azure.com/Bing/Search/v1/News?$format=json&Query=%27" + topic + "%27";
		URL url = new URL(urlStr);
		String authStr = APIkey + ":" + APIkey;
		
		byte[] authEncBytes = Base64.encodeBase64(authStr.getBytes());
		String authStringEnc = new String(authEncBytes);
		
		int offset = 0;
		ArrayList<String> urls = new ArrayList<String>();
		
		//keep requesting new links till cap reached
		while(offset < quantity){
			
			//open connection
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		    connection.setRequestMethod("GET");
		    connection.setDoOutput(true);
		    connection.setRequestProperty("Authorization", "Basic " + authStringEnc);
		    
		    //build json
	        InputStream content = (InputStream)connection.getInputStream();
	        BufferedReader in   = 
	        		new BufferedReader (new InputStreamReader (content));
	        String response;
	        StringBuilder builder = new StringBuilder();
	       
	        while ((response = in.readLine()) != null) {
	        	builder.append(response);
	        }
	       
	        //parse json and add links to set
	        urls.addAll(this.parseJSON(builder.toString()));
	        
	        offset += 15;
	        url = new URL(urlStr + "&$skip=" + offset);
		}
		
		System.out.println("Collected " + urls.size() + " links.");
		
		//save links to nutch dir
		this.dumpLinks(urls);
		
		//run nutch crawl script
		this.runScript(topic);
		
	}
	
	/* returns parsed URL's */
	private ArrayList<String> parseJSON(String response){
		
		ArrayList<String> results = new ArrayList<String>();
		
		try {
			ObjectNode json = (ObjectNode) new ObjectMapper().readTree(response);

			JsonNode entries = json.get("d").get("results");
			Iterator<JsonNode> iterator = entries.iterator();
			while(iterator.hasNext()){
				ObjectNode o = (ObjectNode)iterator.next();
				results.add(o.get("Url").asText());
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return results;
		
	}
	
	/* execute crawl for each term */
	public void addTerms(String[] terms){
		if(NutchDir==null)
		{
			log.log(Level.SEVERE, "Skipping term because Nutch has failed to initialize ");
			return;

		}
		for(String term : terms){
			if(!pastTerms.contains(term))
			{
				pastTerms.add(term);
				queue.add(term);
			}else
			{
				log.log(Level.INFO, "Skipping term because it has already been searched on : term : " + term);
			}
		}
		System.out.println("Finished batch");
		
	}
	
	
	/* Save links to text file - nutch seed links used for indexing web pages */
	public void dumpLinks(ArrayList<String> urls) throws IOException{
		
		String directory = (NutchSeedDir != null) ? NutchSeedDir : System.getProperty("User.dir");		
		System.out.println("Path: " +  directory);
	
		File output = new File(directory + File.separator + "seed.txt");
	
		if(!output.exists()) output.createNewFile(); 
	
		FileWriter fw = new FileWriter(output.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
	
		for(String e : urls){
			bw.write(e);
			bw.newLine();
		}
	
		bw.flush();
		bw.close();
		
	}
	
	/* Runs Nutch crawl script over crawled articles */
	public void runScript(String topic) throws IOException{
	 
		//script dir
		String[] cmd = { 
				NutchDir + File.separator + "bin" + File.separator + "crawl", 
				NutchSeedDir + File.separator, 
				NutchDir + File.separator + "CrawledArticles" + File.separator + topic + File.separator, 
				"http://localhost:8983/solr/collection2",
				"" + pageCap
				};
				
		//run process
		Runtime rt = Runtime.getRuntime();
		Process proc = rt.exec(cmd);
		
		BufferedReader stdInput = new BufferedReader(new 
			     InputStreamReader(proc.getInputStream()));

		BufferedReader stdError = new BufferedReader(new 
			     InputStreamReader(proc.getErrorStream()));
		
		// read the output from the command
		String s = null;
		while ((s = stdInput.readLine()) != null) {
		    System.out.println(s);
		}

		// read any errors from the attempted command
		while ((s = stdError.readLine()) != null) {
		    System.out.println(s);
		}
			
	}
	
	private void loadConfig() throws IOException{

		Properties prop = new Properties();
		String propFileName = "config.properties";
	
		InputStream inputStream = new FileInputStream(propFileName);
		
		if (inputStream != null) {
			prop.load(inputStream);
		} 
		
		NutchDir = prop.getProperty("NutchDir");
		NutchSeedDir = prop.getProperty("NutchSeedDir");
		APIkey = prop.getProperty("APIkey");
		pageCap = Integer.parseInt(prop.getProperty("pageCap"));
				
	}
	

	//Driver - testing
	public static void main(String[] args) {
	


		NewsCrawl newsCrawl = new NewsCrawl();
			
			/*
			Scanner sc = new Scanner(System.in);
			
			System.out.println("> Search for: ");
			String arg = sc.nextLine();
			
			System.out.println("> How many articles ? ");
			int cap = sc.nextInt();
			
			
			newsCrawl.crawlArticles(arg, cap);
			*/
		String[] topics = {"interstellar", "immigration", "leonardo dicaprio", "gta5"};
		newsCrawl.addTerms(topics);
	

		
	}
	

}

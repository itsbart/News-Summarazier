import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
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
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.commons.codec.binary.Base64;


/**
 * 
 * @author Bartek
 * Stores web search results, saves links to text file
 *
 */


public class NewsCrawl {

	/* !!! STATIC FIELDS - SET FOR PROPER EXECUTION !!! */
	private static final String NutchDir = "/Users/Bartek/Applications/apache-nutch-1.9";
	private static final String NutchSeedDir = NutchDir + File.separator + "urls";
	private static final String APIkey = "YbRJwaUTNNq6CPXBWMwJPK/P1LBvf5w4+gaMiKkemc8";
	
	private ArrayList<Entry> news;

	public NewsCrawl(){
		news = new ArrayList<Entry>();
	}
	
	
	/* Print Contents - News Information */
	public void print(){
		int count = 1;
		for(Entry e : news){
			System.out.println(count + ". " + e._title);
			System.out.println(e._Source);
			System.out.println(e._Date);
			System.out.println(e._Url);
			System.out.println("");
			count++;
		}
	}
	
	/* Main method to get links using BING API*/
	public void crawlArticles(String topic, int quantity) 
			throws MalformedURLException, IOException {
	     
    	//encode URL
		topic = topic.trim();
		topic = topic.replaceAll(" ", "%20");
		
		String urlStr = "https://api.datamarket.azure.com/Bing/Search/v1/News?$format=json&Query=%27" + topic + "%27";
		URL url = new URL(urlStr);
		String authStr = APIkey + ":" + APIkey;
		
		//encode
		byte[] authEncBytes = Base64.encodeBase64(authStr.getBytes());
		String authStringEnc = new String(authEncBytes);
		
		int offset = 0;
		
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
	       
	        this.parseJSON(builder.toString());
	        
	        offset += 15;
	        url = new URL(urlStr + "&$skip=" + offset);
	        
		}
	}
	
	private void parseJSON(String response){
		
		try {
			JSONObject json = new JSONObject(response);
			JSONArray entries = json.getJSONObject("d").getJSONArray("results");
							
			for(int i = 0; i < entries.length(); i++){
				JSONObject o = entries.getJSONObject(i);
				news.add(new Entry(
						o.getString("Title"),
						o.getString("Url"),
						o.getString("Source"),
						o.getString("Date")));
			}
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//execute crawl for each term
	public void addTerms(String[] terms){
		for(String term : terms){
			try {
				this.crawlArticles(term, 60);
				this.dumpLinks();
				this.runScript();
				this.clear();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	/* Save links to text file - nutch seed links used for indexing web pages */
	public void dumpLinks() throws IOException{
		
		String directory = (NutchSeedDir != null) ? NutchSeedDir : System.getProperty("User.dir");		
		System.out.println("Path: " +  directory);
	
		File output = new File(directory + File.separator + "seed.txt");
	
		if(!output.exists()) output.createNewFile(); 
	
		FileWriter fw = new FileWriter(output.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
	
		for(Entry e : news){
			bw.write(e._Url);
			bw.newLine();
		}
	
		bw.flush();
		bw.close();
		
	}
	
	/* Runs Nutch crawl script over crawled articles */
	public void runScript() throws IOException{
	 
		//script dir
		String[] cmd = { 
				NutchDir + File.separator + "bin" + File.separator + "crawl", 
				NutchSeedDir + File.separator, 
				NutchDir + File.separator + "CrawledArticles/", 
				"http://localhost:8983/solr/",
				"" + news.size()
				};
		
		System.out.println("Running script with size: " + cmd[cmd.length - 1]);
		
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
		System.out.println("Here is the standard error of the command (if any):\n");
		while ((s = stdError.readLine()) != null) {
		    System.out.println(s);
		}
			
	}
	
	public void clear(){
		news.clear();
	}

	//Driver - testing
	public static void main(String[] args) {
	
		try {
			
			NewsCrawl newsCrawl = new NewsCrawl();
			Scanner sc = new Scanner(System.in);
			
			System.out.println("> Search for: ");
			String arg = sc.nextLine();
			
			System.out.println("> How many articles ? ");
			int cap = sc.nextInt();
			
			newsCrawl.crawlArticles(arg, cap);
			newsCrawl.print();
			
			
			//newsCrawl.dumpLinks();
			//newsCrawl.runScript();
			//newsCrawl.clear();
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//inner class - quadruple for holding results
	public class Entry {
		
		String _title;
		String _Url;
		String _Date;
		String _Source;
		
		public Entry(String title, String Url, String Date, String Source){
			_title = title;
			_Url = Url;
			_Date = Date;
			_Source = Source;
		}
			
	}
}

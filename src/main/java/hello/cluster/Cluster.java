package hello.cluster;

import com.fasterxml.jackson.databind.JsonNode;
import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.clustering.KMeans;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.Instance;
import net.sf.javaml.tools.InstanceTools;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by daniel.bremiller on 11/30/2014.
 */
public class Cluster
{
	public static Dataset[] dateCluster(JsonNode docs)
	{
		Dataset data = new DefaultDataset();
		HashSet<Double> dates = new HashSet<Double>();

		for (JsonNode doc : docs) {
			Instance tmpInstance = InstanceTools.randomInstance(1);

			try
			{

			String  dateStr = (doc.get("date").asText());
			SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'");
			Date d = null;

			d = f.parse(dateStr);

			Double milliseconds = new Long(d.getTime()).doubleValue();
			dates.add(milliseconds);
			tmpInstance.setClassValue(doc.get("id").asText());
			tmpInstance.put(0,milliseconds);
			data.add(tmpInstance);
			} catch (ParseException e)
			{
				e.printStackTrace();
			}
		}
        /*
         * Create a new instance of the KMeans algorithm, with no options
         * specified. By default this will generate 4 clusters.
         */

		int clustersize =6;
		Clusterer km;
		if(dates.size()<= clustersize)
		{

				// too small to use kmeans, might as well just sort them myself.
			Logger.getLogger("Cluster:").log(Level.INFO,"Not doing kmeans due to small variety: " + data.size());

				return simpleSort(dates, data);

		}
		km = new KMeans(clustersize,10);

        /*
         * Cluster the data, it will be returned as an array of data sets, with
         * each dataset representing a cluster
         */
		Logger.getLogger("Cluster:").log(Level.INFO,"Dataset size: " + data.size());
		for(Instance i : data)
		{
			Logger.getLogger("Cluster:").log(Level.INFO, (i.toString()));
		}
		Dataset[] clusters = km.cluster(data);
		Logger.getLogger("Cluster:").log(Level.INFO,"Cluster count: " + clusters.length);
	return clusters;
	}

	private static Dataset[] simpleSort(HashSet<Double> dates, Dataset data)
	{
		Dataset[] sets = new Dataset[dates.size()];
		int i=0;
		for(Double d : dates)
		{

			Dataset s = new DefaultDataset();
			for(Instance in : data)
			{
				if(in.get(0).compareTo(d)==0)
				{
					s.add(in);
				}
			}
			sets[i++]=s;
		}
		return sets;
	}

	public static void main(String[] args)
		{
			Dataset data = new DefaultDataset();
			for (int i = 0; i < 50; i++)
			{
				Instance tmpInstance = InstanceTools.randomInstance(1);
				tmpInstance.put(0, new Double(i%10));
			//	tmpInstance.put(1, new Double(i));
				tmpInstance.setClassValue(i);
				data.add(tmpInstance);
			}
        /*
         * Create a new instance of the KMeans algorithm, with no options
         * specified. By default this will generate 4 clusters.
         */
			Clusterer km = new KMeans(10, 10);
        /*
         * Cluster the data, it will be returned as an array of data sets, with
         * each dataset representing a cluster
         */
			Dataset[] clusters = km.cluster(data);
			Logger.getLogger("Cluster:").log(Level.INFO, "Cluster count: " + clusters.length);

			int count = 0;
			for(Dataset set : clusters)
			{
				System.out.println("Cluster " + count);
						count++;
				for(Instance i : set)
				{
					System.out.println(i.toString());
				}
			}


		}
}

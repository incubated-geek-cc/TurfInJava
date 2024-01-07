import com.mapbox.geojson.Point;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ProximityAnalysis {
    static ProximityAnalysis demo;
    static double bufferDistance=400.0;
    
    static String clinicsGeojsonFilepath="chas_clinics.geojson"; // change filepath accordingly
    static String hdbGeojsonFilepath="geocoded_hdb_buildings.geojson"; // change filepath accordingly

    static JSONObject clinicGeojson,hdbGeojson;
    static JSONArray clinicGeojsonFeatures,hdbGeojsonFeatures;
    
    public static void main(String[] args) {
        demo=new ProximityAnalysis();
        
        clinicGeojson = new JSONObject(demo.readFileToString(clinicsGeojsonFilepath));
        hdbGeojson = new JSONObject(demo.readFileToString(hdbGeojsonFilepath));
        clinicGeojsonFeatures=clinicGeojson.getJSONArray("features");
        hdbGeojsonFeatures=hdbGeojson.getJSONArray("features");
        
        // a for-loop to access the Point coordinates of each residential block
        int fLen=hdbGeojsonFeatures.length(); // refers to total blocks
        int hdbWithinBuffer=0; // instantiate a counter to track blocks within 400m
        for(int f=0;f<fLen;f++) {
          JSONObject hdbGeojsonFeature=hdbGeojsonFeatures.getJSONObject(f);
          JSONArray hdbCoords=(hdbGeojsonFeature.getJSONObject("geometry")).getJSONArray("coordinates");
          double latitude=hdbCoords.getDouble(1);
          double longitude=hdbCoords.getDouble(0);

          /* TO DO: Check if this Point feature is within 400.0m of */
          /* at least one clinic */
          Point pt1=Point.fromLngLat(longitude,latitude);
          if(demo.isWithinBufferZone(bufferDistance, pt1)) {
              hdbWithinBuffer++;
          }
        }
        double ratioOfhdbWithinBufferZone=(hdbWithinBuffer*1.0)/(fLen*1.0);
        
        System.out.println("Total No. Of HDB Buildings: "+fLen);
        System.out.println("Total No. Of HDB Buildings Within 400m Buffer Zone: "+hdbWithinBuffer);
        System.out.println("% Of HDB Buildings Within 400m Buffer Zone: "+(ratioOfhdbWithinBufferZone*100));
    }
    
    private boolean isWithinBufferZone(double bufferDistance, Point pt1) {
        try {
          for(int i=0;i<clinicGeojsonFeatures.length();i++) {
            JSONObject clinicGeojsonFeature=clinicGeojsonFeatures.getJSONObject(i);
            JSONArray clinicCoords=(clinicGeojsonFeature.getJSONObject("geometry")).getJSONArray("coordinates");
            double latitude=clinicCoords.getDouble(1);
            double longitude=clinicCoords.getDouble(0);

            // instantiate clinic as a Point geometry
            Point pt2 = Point.fromLngLat(longitude, latitude);
            // Point pt1 shall refer to a residential block
            // represented as a Point geometry

            double distanceBetweenPts=TurfMeasurement.distance(pt1, pt2, TurfConstants.UNIT_METERS);
            // returns a value after a clinic within promixity
            // after a clinic is found and stop running for-loop       
            if(distanceBetweenPts<=bufferDistance) {
                return true;
            }
          }
        } catch (JSONException ex) {
          System.out.println("[JSONException ex]" + ex);
        }
        return false;
    }
    private String readFileToString(String filepath) {
        String content = "";
        try {
          content = new String(Files.readAllBytes(Paths.get(filepath)));
        } catch (IOException ex) {
          System.out.println("[readFileToString | IOException ex]" + ex);
        }
        return content;
    }
}
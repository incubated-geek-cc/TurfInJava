import com.mapbox.geojson.MultiPolygon;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.turf.TurfJoins;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

public class GeocodeNDC {
    public static void main(String args[]) {
        String boundariesFilepath="boundaries.geojson";
        String locationsFilepath="targetLocations.geojson";
        
        JSONObject targetLocations=new JSONObject(readFileContentToString(locationsFilepath));
        JSONObject boundaries=new JSONObject(readFileContentToString(boundariesFilepath));
        
        JSONObject updatedTargetGeojson=new JSONObject(targetLocations.toString());
        JSONArray updatedFeatures=new JSONArray();
        
        JSONArray targetFeatures=targetLocations.getJSONArray("features");
        JSONArray boundaryFeatures=boundaries.getJSONArray("features");
        
        for(int t=0; t<targetFeatures.length(); t++) {  // loop through each location feature
            JSONObject targetFeature=(JSONObject) targetFeatures.get(t);
            JSONObject locationPropObj=targetFeature.getJSONObject("properties");
            JSONObject locationGeomObj=targetFeature.getJSONObject("geometry");
            
            for(int b=0; b<boundaryFeatures.length(); b++) { // loop through each boundary feature
                JSONObject boundaryFeature=(JSONObject) boundaryFeatures.get(b);
                JSONObject boundaryPropObj=boundaryFeature.getJSONObject("properties");
                JSONObject boundaryGeomObj=boundaryFeature.getJSONObject("geometry");
                
                boolean isPoly=false;
                if(boundaryGeomObj.getString("type").equalsIgnoreCase("Polygon")) {
                    isPoly=true;
                } else if(boundaryGeomObj.getString("type").equalsIgnoreCase("MultiPolygon")) {
                    isPoly=false;
                }
                // use Turf to check if location coordinate resides within boundary
                boolean inBoundary=false;
                double ptLat = (double) locationGeomObj.getJSONArray("coordinates").get(1);
                double ptLng = (double) locationGeomObj.getJSONArray("coordinates").get(0);
                
                inBoundary = booleanPointInPolygon(ptLat, ptLng, isPoly, boundaryGeomObj.toString());
                if(inBoundary) {
                    JSONObject updatedPropertyObj = new JSONObject(locationPropObj.toString());
                    
                    Set<String> allBoundaryProps = boundaryPropObj.keySet();
                    Iterator<String> iter = allBoundaryProps.iterator();
                    while(iter.hasNext()) {
                        String boundaryProp=iter.next();
                        updatedPropertyObj.put(boundaryProp, boundaryPropObj.get(boundaryProp));
                    }
                    
                    JSONObject updatedTargetFeature=new JSONObject();
                    updatedTargetFeature.put("type", "Feature");
                    updatedTargetFeature.put("properties", updatedPropertyObj);
                    updatedTargetFeature.put("geometry", locationGeomObj);
                    
                    updatedFeatures.put(updatedTargetFeature);
                    break;
                }
            }
        }
        updatedTargetGeojson.put("features", updatedFeatures);
        System.out.println(updatedTargetGeojson.toString(2));
    }

    private static boolean booleanPointInPolygon(double ptLat, double ptLng, boolean isPoly, String jsonStr) {
        Point pt = Point.fromLngLat(ptLng, ptLat);
        if(isPoly) {
            Polygon turfPoly = Polygon.fromJson(jsonStr);
            return TurfJoins.inside(pt, turfPoly);
        } else {
            MultiPolygon turfMultiPoly = MultiPolygon.fromJson(jsonStr);
            return TurfJoins.inside(pt, turfMultiPoly);
        }
    }
    
    private static String readFileContentToString(String filepath) {
        String content = "";
        try {
            content = new String(Files.readAllBytes(Paths.get(filepath)));
        } catch (IOException ex) {
            System.out.println("[readFileContentToString IOException]" + ex.getLocalizedMessage());
        }
        return content;
    }
}
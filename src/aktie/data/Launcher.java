package aktie.data;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.json.JSONObject;

@Entity
public class Launcher
{

    @Id
    private String extension;
    private String path;

    public String getExtension()
    {
        return extension;
    }

    public void setExtension ( String extension )
    {
        this.extension = extension;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath ( String path )
    {
        this.path = path;
    }

    public String toString() {
    	JSONObject o = new JSONObject(this);
    	return o.toString(4);
    }


}

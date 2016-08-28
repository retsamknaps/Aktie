package aktie.data;

import javax.persistence.Entity;
import javax.persistence.Id;

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

}

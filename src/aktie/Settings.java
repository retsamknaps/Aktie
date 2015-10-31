package aktie;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Properties;

public class Settings
{

    private File settingsFile;

    public Settings ( String nodedir )
    {
        settingsFile = new File ( nodedir + File.separator + "settings.props" );
    }

    public Properties getProperties()
    {
        Properties p = new Properties();

        if ( settingsFile.exists() )
        {
            try
            {
                FileReader fr = new FileReader ( settingsFile );
                p.load ( fr );
                fr.close();
            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

        }

        return p;
    }

    public void saveProperties ( Properties p )
    {
        try
        {
            PrintWriter pw = new PrintWriter ( settingsFile );
            p.store ( pw, "aktie settings" );
            pw.close();
        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

    }

}

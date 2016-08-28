package aktie.gui.launchers;

import java.io.File;
import java.io.FileOutputStream;

public class ProgramRunner implements Runnable
{

    private String ProgName;
    private String FileName;
    private String BaseDir;

    public ProgramRunner ( String progname, String file, String bd )
    {
        ProgName = progname;
        FileName = file;
        BaseDir = bd;
        Thread t = new Thread ( this );
        t.start();
    }

    @Override
    public void run()
    {
        try
        {
            File bdir = new File ( BaseDir );
            File log = File.createTempFile ( "exec", ".log", bdir );
            log.deleteOnExit();
            FileOutputStream fos = new FileOutputStream ( log );
            ProcessBuilder pb = new ProcessBuilder ( ProgName, FileName );
            pb.redirectErrorStream ( true );
            Process p = pb.start();
            byte buf[] = new byte[1024];
            int l = p.getInputStream().read ( buf );

            while ( l >= 0 )
            {
                if ( l > 0 )
                {
                    fos.write ( buf, 0, l );
                }

                l = p.getInputStream().read ( buf );
            }

            fos.close();
            log.delete();
        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

    }

}



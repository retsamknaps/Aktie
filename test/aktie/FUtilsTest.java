package aktie;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Path;

import org.junit.Test;

import aktie.utils.FUtils;

public class FUtilsTest
{

    @Test
    public void testPath()
    {
        File nf = new File ( "testpathdir" );
        File nf2 = new File ( "testpathdir" );

        FUtils.deleteDir ( nf );

        assertTrue ( nf.mkdirs() );

        try
        {
            Path p = nf.getCanonicalFile().toPath();
            Path p2 = nf2.getCanonicalFile().toPath();
            assertTrue ( p.startsWith ( p2 ) );
        }

        catch ( Exception e )
        {
            e.printStackTrace();
            fail();
        }


    }

}

package aktie;

import static org.junit.Assert.*;

import java.io.File;
//import java.util.List;

//import org.hibernate.Query;
import org.hibernate.Session;
import org.junit.Test;

import aktie.data.HH2Session;
import aktie.utils.FUtils;

public class H2HibernateTest
{

    @Test
    public void testIt()
    {
        try
        {
            File tmpdir = new File ( "h2dbtest" );
            FUtils.deleteDir ( tmpdir );
            assertTrue ( tmpdir.mkdirs() );
            HH2Session sf = new HH2Session();
            sf.init ( "h2dbtest" );

            Session s = sf.getSession();
            s.close();
        }

        catch ( Exception e )
        {
            e.printStackTrace();
            fail();
        }

    }

}

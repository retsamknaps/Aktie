package aktie.index;

import java.io.IOException;

import org.json.JSONObject;

import aktie.Wrapper;
import aktie.data.CObj;

public class DumpIndexUtil
{

    public static void dumpIndex ( Index i )
    {
        CObjList lst = i.getAllCObj();

        for ( int c = 0; c < lst.size(); c++ )
        {
            try
            {
                CObj co = lst.get ( c );
                JSONObject jo = co.GETPRIVATEJSON();
                System.out.println ( jo.toString ( 4 ) );
                System.out.println ( "NAME: " + co.getString ( CObj.NAME ) + " payment: " +
                                     co.checkPayment ( Wrapper.NEWPAYMENT ) );
            }

            catch ( IOException e )
            {
                e.printStackTrace();
            }

        }

        lst.close();
    }

}

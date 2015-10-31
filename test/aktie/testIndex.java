package aktie;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import aktie.data.CObj;
import aktie.index.CObjList;
import aktie.index.Index;
import aktie.utils.FUtils;

public class testIndex
{

    @Test
    public void testIndexer()
    {
        File id = new File ( "testindex" );
        FUtils.deleteDir ( id );
        Index i = new Index();
        i.setIndexdir ( id );

        try
        {
            i.init();

            CObj b0 = new CObj();
            b0.setDig ( "dig0" );
            b0.setId ( "id0" );
            b0.setSignature ( "sig0" );
            b0.setType ( "type0" );
            b0.pushDecimal ( "dec", 0.54D );
            b0.pushDecimal ( "dec1", 1.44D );
            b0.pushNumber ( "num0", 123L );
            b0.pushPrivate ( "prv0", "private stuff" );
            b0.pushString ( "s0", "string0" );
            b0.pushString ( "s1", "string1" );
            b0.pushText ( "title", "This is a title" );
            i.index ( b0 );

            CObj b1 = new CObj();
            b1.setDig ( "dig1" );
            b1.setId ( "id1" );
            b1.setSignature ( "sig1" );
            b1.setType ( "type1" );
            b1.pushDecimal ( "dec", 0.54D );
            b1.pushDecimal ( "dec1", 1.44D );
            b1.pushNumber ( "num0", 123L );
            b1.pushPrivate ( "prv0", "private stuff" );
            b1.pushString ( "s0", "string0" );
            b1.pushString ( "s1", "string1" );
            b1.pushText ( "title", "This not not what you think!" );
            i.index ( b1 );

            CObjList l0 = i.search ( "text_title:what", 10000 );
            //List<CObj> l0 = i.searchId("id0");

            assertEquals ( 1, l0.size() );

            CObj c = l0.get ( 0 );
            l0.close();

            assertEquals ( c, b1 );

            l0 = i.search ( "text_title:title", 10000 );
            //List<CObj> l0 = i.searchId("id0");

            assertEquals ( 1, l0.size() );

            c = l0.get ( 0 );
            l0.close();

            assertEquals ( c, b0 );

            i.close();

        }

        catch ( Exception e )
        {
            e.printStackTrace();
            fail();
        }

    }

}

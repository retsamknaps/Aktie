package aktie.user;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aktie.GenericNoContextProcessor;
import aktie.data.CObj;
import aktie.index.Index;

public class NewQueryProcessor extends GenericNoContextProcessor
{

    private Index index;

    public NewQueryProcessor ( Index i )
    {
        index = i;
    }

    @Override
    public boolean process ( CObj o )
    {
        String type = o.getType();

        if ( CObj.QUERY.equals ( type ) )
        {
            String name = o.getString ( CObj.NAME );
            Matcher m = Pattern.compile ( "(\\S+)" ).matcher ( name );

            if ( m.find() )
            {
                o.setId ( "QUERY_ID_" + name );

                try
                {
                    index.index ( o );
                    index.forceNewSearcher();
                }

                catch ( IOException e )
                {
                    e.printStackTrace();
                }

            }

            return true;
        }

        return false;
    }

}

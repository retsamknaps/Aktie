package aktie.net;

import aktie.GenericProcessor;
import aktie.data.CObj;
import aktie.index.CObjList;
import aktie.index.Index;

public class ReqSpamExProcessor extends GenericProcessor
{

    private Index index;
    private ConnectionThread connection;

    public ReqSpamExProcessor ( Index i, ConnectionThread c )
    {
        index = i;
        connection = c;
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.CON_REQ_SPAMEX.equals ( type ) )
        {
            String creator = b.getString ( CObj.CREATOR );
            Long first = b.getNumber ( CObj.FIRSTNUM );
            Long last = b.getNumber ( CObj.LASTNUM );

            if ( creator != null && first != null && last != null )
            {
                long maxlast = first + 1000;
                last = Math.min ( maxlast, last );
                CObjList cl = index.getSpamEx ( creator, first, last );
                connection.enqueue ( cl );
            }

            return true;
        }

        return false;
    }

}

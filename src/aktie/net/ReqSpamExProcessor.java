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
                CObjList cl = index.getSpamEx ( creator, first, last );
                System.out.println ( "RETURNING SPAM EX!: " + cl.size() );
                connection.enqueue ( cl );
            }

            return true;
        }

        return false;
    }

}
